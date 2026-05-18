package com.agromag.service;

import com.agromag.domain.entities.Alert;
import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropEvent;
import com.agromag.domain.entities.Profile;
import com.agromag.domain.entities.Recommendation;
import com.agromag.domain.enums.AlertSeverity;
import com.agromag.domain.enums.CropType;
import com.agromag.domain.enums.EventType;
import com.agromag.domain.enums.Municipality;
import com.agromag.domain.enums.RecommendationType;
import com.agromag.domain.enums.RiskLevel;
import com.agromag.domain.model.ClimateData;
import com.agromag.dto.request.ChatRequest;
import com.agromag.dto.response.AssistantContextResponse;
import com.agromag.dto.response.ChatResponse;
import com.agromag.dto.response.ProfileResponse;
import com.agromag.repository.AlertRepository;
import com.agromag.repository.CropEventRepository;
import com.agromag.repository.CropRepository;
import com.agromag.repository.RecommendationRepository;
import com.agromag.service.WebSearchService.WebSearchResult;
import com.agromag.service.WebSearchService.WebSearchSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class AssistantService {

	private static final Logger log = LoggerFactory.getLogger(AssistantService.class);
	private static final int MAX_CROPS_IN_PROMPT = 40;
	private static final int MAX_RECENT_ALERTS = 5;
	private static final int MAX_PENDING_RECOMMENDATIONS = 5;
	private static final int MAX_RECENT_EVENTS = 8;
	private static final int MAX_HISTORY_TURNS = 10;
	private static final String CONTEXT_CACHE = "assistantContext";

	private enum AssistantIntent {
		SUMMARY,
		CROPS,
		ALERTS,
		RECOMMENDATIONS,
		IRRIGATION,
		PESTS,
		FERTILIZER,
		UNKNOWN
	}

	private final ChatClient chatClient;
	private final ProfileService profileService;
	private final CropRepository cropRepository;
	private final AlertRepository alertRepository;
	private final RecommendationRepository recommendationRepository;
	private final CropEventRepository cropEventRepository;
	private final ClimateService climateService;
	private final WebSearchService webSearchService;
	private final CacheManager cacheManager;

	public AssistantService(
			ChatClient.Builder chatClientBuilder,
			ProfileService profileService,
			CropRepository cropRepository,
			AlertRepository alertRepository,
			RecommendationRepository recommendationRepository,
			CropEventRepository cropEventRepository,
			ClimateService climateService,
			WebSearchService webSearchService,
			CacheManager cacheManager) {
		this.chatClient = chatClientBuilder.build();
		this.profileService = profileService;
		this.cropRepository = cropRepository;
		this.alertRepository = alertRepository;
		this.recommendationRepository = recommendationRepository;
		this.cropEventRepository = cropEventRepository;
		this.climateService = climateService;
		this.webSearchService = webSearchService;
		this.cacheManager = cacheManager;
	}

	public ProfileResponse getOrCreateProfileForChat(UUID profileId, String email) {
		return profileService.getOrCreateProfile(profileId, email);
	}

	public Profile getProfileEntity(UUID profileId) {
		return profileService.getProfileById(profileId);
	}

	public AssistantContextResponse getContext(UUID profileId, String email) {
		ProfileResponse profile = getOrCreateProfileForChat(profileId, email);
		UserContext ctx = buildUserContext(profileId, profile);
		return new AssistantContextResponse(
				profileId,
				profile.fullName(),
				profile.municipality().getDisplayName(),
				ctx.crops().size(),
				ctx.alerts().size(),
				ctx.recommendations().size(),
				ctx.events().size(),
				ctx.climateInfo(),
				ctx.crops().stream().map(this::formatCropForContext).toList(),
				ctx.alerts().stream().map(this::formatAlertForContext).toList(),
				ctx.recommendations().stream().map(this::formatRecommendationForContext).toList(),
				ctx.events().stream().map(this::formatEventForContext).toList()
		);
	}

	public ChatResponse respond(UUID profileId, String email, ChatRequest req) {
		ProfileResponse profile = getOrCreateProfileForChat(profileId, email);

		UserContext ctx = getCachedContext(profileId);

		List<String> suggestions = generateSuggestions(ctx);
		boolean useWebSearch = shouldUseWebSearch(req.message());
		String directReply = useWebSearch ? null : buildDirectReply(req.message(), ctx);
		if (directReply != null) {
			return new ChatResponse(directReply, Instant.now(), suggestions);
		}

		String systemPrompt = buildSystemPrompt(profile, ctx,
				useWebSearch ? buildWebContext(req.message(), profile, ctx) : null);

		List<Message> history = buildHistoryMessages(req.history());

		String reply = sanitizeAssistantReply(
				chatClient.prompt()
						.system(systemPrompt)
						.messages(history)
						.user(req.message())
						.call()
						.content()
		);
		return new ChatResponse(reply, Instant.now(), suggestions);
	}

	public void streamResponse(UUID profileId, String email, ChatRequest req, SseEmitter emitter) {
		AtomicBoolean completed = new AtomicBoolean(false);

		emitter.onTimeout(() -> {
			completed.set(true);
			emitter.complete();
		});
		emitter.onCompletion(() -> completed.set(true));
		emitter.onError(e -> completed.set(true));

		CompletableFuture.runAsync(() -> {
			try {
				ProfileResponse profile = getOrCreateProfileForChat(profileId, email);
				UserContext ctx = getCachedContext(profileId);
				List<String> suggestions = generateSuggestions(ctx);
				boolean useWebSearch = shouldUseWebSearch(req.message());
				String directReply = useWebSearch ? null : buildDirectReply(req.message(), ctx);
				if (directReply != null) {
					if (!completed.get()) {
						sendTypingChunks(emitter, completed, directReply);
						sendSse(emitter, "suggestions", suggestions);
						sendSse(emitter, "done", "");
					}
					emitter.complete();
					return;
				}

				String systemPrompt = buildSystemPrompt(profile, ctx,
						useWebSearch ? buildWebContext(req.message(), profile, ctx) : null);

				log.debug("assistant_system_prompt_length={}", systemPrompt.length());

				List<Message> history = buildHistoryMessages(req.history());

				if (!completed.get()) {
					sendSse(emitter, "status", "AGROBOT está pensando...");
				}

				chatClient.prompt()
						.system(systemPrompt)
						.messages(history)
						.user(req.message())
						.stream()
						.content()
					.doOnNext(token -> {
						if (!completed.get()) {
							String sanitized = sanitizeToken(token);
							// isBlank() descartaría tokens de espacio (" ") produciendo
							// palabras pegadas en el cliente. Solo se omiten cadenas vacías.
							if (!sanitized.isEmpty()) {
								try {
									sendSse(emitter, "token", sanitized);
								} catch (IOException e) {
									log.debug("assistant_token_send_failed", e);
									completed.set(true);
								}
							}
						}
					})
						.doOnComplete(() -> {
							if (!completed.get()) {
								try {
									sendSse(emitter, "suggestions", suggestions);
									sendSse(emitter, "done", "");
								} catch (IOException e) {
									log.debug("assistant_done_send_failed", e);
								}
								emitter.complete();
							}
						})
						.doOnError(e -> {
							log.error("assistant_stream_error", e);
							if (!completed.get()) {
								try {
									sendSse(emitter, "error",
											"AGROBOT no pudo responder en este momento. Intenta nuevamente.");
								} catch (IOException ex) {
									log.debug("assistant_stream_error_notification_failed", ex);
								}
								emitter.completeWithError(e);
							}
						})
						.subscribe();

			} catch (Exception e) {
				log.error("assistant_stream_setup_error", e);
				if (!completed.get()) {
					try {
						sendSse(emitter, "error",
								"AGROBOT no pudo responder en este momento. Intenta nuevamente.");
					} catch (IOException ex) {
						log.debug("assistant_stream_error_notification_failed", ex);
					}
					emitter.completeWithError(e);
				}
			}
		});
	}

	public UserContext buildUserContext(UUID profileId, ProfileResponse profile) {
		return buildUserContextInternal(profileId);
	}

	/**
	 * Returns the user context from the cache if available; otherwise builds and
	 * caches it. Uses programmatic cache access to avoid Spring AOP self-invocation
	 * limitations.
	 */
	private UserContext getCachedContext(UUID profileId) {
		Cache cache = cacheManager.getCache(CONTEXT_CACHE);
		if (cache != null) {
			Cache.ValueWrapper wrapper = cache.get(profileId);
			if (wrapper != null) {
				log.debug("assistant_context_cache_hit profileId={}", profileId);
				return (UserContext) wrapper.get();
			}
		}
		UserContext ctx = buildUserContextInternal(profileId);
		if (cache != null) {
			cache.put(profileId, ctx);
		}
		return ctx;
	}

	private UserContext buildUserContextInternal(UUID profileId) {
		log.debug("assistant_context_build profileId={}", profileId);

		Profile profile = profileService.getProfileById(profileId);
		log.debug("assistant_context_profile municipality={}", profile.getMunicipality());

		List<Crop> crops = cropRepository.findByProfile_Id(profileId);
		log.debug("assistant_context_crops count={}", crops.size());
		List<CropSummary> cropSummaries = crops.stream()
				.limit(MAX_CROPS_IN_PROMPT)
				.map(CropSummary::from)
				.toList();

		List<Alert> unreadAlerts = alertRepository.findByProfile_IdAndReadAtIsNullOrderByCreatedAtDesc(profileId)
				.stream().limit(MAX_RECENT_ALERTS).toList();
		log.debug("assistant_context_alerts count={}", unreadAlerts.size());
		List<AlertSummary> alertSummaries = unreadAlerts.stream()
				.map(AlertSummary::from)
				.toList();

		List<Recommendation> pendingRecs = recommendationRepository.findPendingByProfileId(
				profileId, PageRequest.of(0, MAX_PENDING_RECOMMENDATIONS));
		log.debug("assistant_context_recommendations count={}", pendingRecs.size());
		List<RecommendationSummary> recSummaries = pendingRecs.stream()
				.map(RecommendationSummary::from)
				.toList();

		List<CropEvent> recentEvents = cropEventRepository.findRecentByProfileId(
				profileId, PageRequest.of(0, MAX_RECENT_EVENTS));
		log.debug("assistant_context_events count={}", recentEvents.size());
		List<EventSummary> eventSummaries = recentEvents.stream()
				.map(EventSummary::from)
				.toList();

		String climateInfo = fetchClimateInfo(profile.getMunicipality());

		return new UserContext(cropSummaries, alertSummaries, recSummaries, eventSummaries, climateInfo);
	}

	private String fetchClimateInfo(Municipality municipality) {
		try {
			ClimateData climate = climateService.getCurrentClimate(municipality);
			return String.format("Clima actual en %s: temperatura %.1f°C, humedad %.0f%%",
					municipality.getDisplayName(), climate.temperature(), climate.humidity());
		} catch (Exception e) {
			log.warn("climate_fetch_failed for assistant: {}", e.getMessage());
			return "No se pudo obtener el clima actual.";
		}
	}

	/** Builds a properly-typed message list for conversation history. */
	private List<Message> buildHistoryMessages(List<ChatRequest.ChatTurn> history) {
		if (history == null || history.isEmpty()) {
			return List.of();
		}
		List<Message> messages = new ArrayList<>();
		int start = Math.max(0, history.size() - MAX_HISTORY_TURNS);
		for (int i = start; i < history.size(); i++) {
			ChatRequest.ChatTurn turn = history.get(i);
			if ("user".equals(turn.role())) {
				messages.add(new UserMessage(turn.content()));
			} else if ("assistant".equals(turn.role())) {
				messages.add(new AssistantMessage(turn.content()));
			}
		}
		return messages;
	}

	/** Strips markdown markers from a streaming token. */
	private String sanitizeToken(String token) {
		if (token == null) return "";
		return token.replace("**", "").replace("__", "");
	}

	String buildSystemPrompt(ProfileResponse profile, UserContext ctx) {
		return buildSystemPrompt(profile, ctx, null);
	}

	String buildSystemPrompt(ProfileResponse profile, UserContext ctx, String webContext) {
		return buildSystemPromptInternal(profile.fullName(), profile.municipality(), ctx, webContext);
	}

	String buildSystemPrompt(Profile profile, UserContext ctx) {
		return buildSystemPromptInternal(profile.getFullName(), profile.getMunicipality(), ctx, null);
	}

	private String buildSystemPromptInternal(String fullName, Municipality municipality, UserContext ctx, String webContext) {
		return AssistantPromptBuilder.build(fullName, municipality, ctx, webContext);
	}

	private String sanitizeAssistantReply(String reply) {
		if (reply == null) {
			return "";
		}
		return reply
				.replace("**", "")
				.replace("__", "")
				.trim();
	}

	private void sendTypingChunks(SseEmitter emitter, AtomicBoolean completed, String text) throws IOException {
		if (text == null || text.isBlank()) {
			return;
		}

		int index = 0;
		while (!completed.get() && index < text.length()) {
			int wordEnd = index;
			while (wordEnd < text.length() && !Character.isWhitespace(text.charAt(wordEnd))) {
				wordEnd++;
			}

			int tokenEnd = wordEnd;
			while (tokenEnd < text.length() && Character.isWhitespace(text.charAt(tokenEnd))) {
				tokenEnd++;
			}

			String token = text.substring(index, tokenEnd);
			if (completed.get()) {
				return;
			}
			sendSse(emitter, "token", token);
			index = tokenEnd;
			try {
				Thread.sleep(token.length() <= 4 ? 28 : 42);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	private void sendSse(SseEmitter emitter, String event, Object data) throws IOException {
		Object payload = data == null ? "" : data;
		emitter.send(SseEmitter.event().name(event).data(payload));
	}

	private boolean shouldUseWebSearch(String message) {
		if (message == null || message.isBlank()) {
			return false;
		}
		String normalized = Normalizer.normalize(message, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);

		boolean explicitlyRequestsResearch = containsAny(normalized,
				"busca", "buscar", "busqueda", "investiga", "investigar", "internet", "web", "noticia", "noticias",
				"actualidad", "actualizado", "actualizada", "reciente", "recientes", "hoy", "esta semana");
		boolean currentAgricultureTopic = containsAny(normalized,
				"precio", "precios", "mercado", "clima", "pronostico", "ideam", "ica", "agrosavia",
				"plaga", "plagas", "enfermedad", "enfermedades", "fertilizante", "fertilizantes",
				"norma", "normativa", "resolucion", "alerta sanitaria");

		return explicitlyRequestsResearch && currentAgricultureTopic;
	}

	private String buildWebContext(String message, ProfileResponse profile, UserContext ctx) {
		if (!webSearchService.isAvailable()) {
			return "El usuario pidió información actualizada, pero la búsqueda web no está habilitada en el servidor.";
		}

		String query = buildSearchQuery(message);
		WebSearchSummary summary = webSearchService.search(query);
		if (!summary.searched()) {
			return summary.note();
		}
		if (summary.results().isEmpty()) {
			return "Consulta web: " + query + "\n" + summary.note();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Consulta web: ").append(query).append('\n');
		sb.append("Resultados encontrados:\n");
		int index = 1;
		for (WebSearchResult result : summary.results()) {
			sb.append(index++).append(". ")
					.append(result.title().isBlank() ? "Fuente sin título" : result.title());
			if (!result.url().isBlank()) {
				sb.append(" — ").append(result.url());
			}
			if (!result.content().isBlank()) {
				sb.append("\nResumen: ").append(result.content());
			}
			sb.append('\n');
		}
		return sb.toString().trim();
	}

	private String buildSearchQuery(String message) {
		String sanitizedMessage = message.replaceAll("[\r\n]+", " ").trim();
		if (sanitizedMessage.length() > 180) {
			sanitizedMessage = sanitizedMessage.substring(0, 180).trim();
		}
		return sanitizedMessage + " agricultura Magdalena Colombia fuentes oficiales recientes";
	}

	private String buildDirectReply(String message, UserContext ctx) {
		AssistantIntent intent = detectIntent(message);
		return switch (intent) {
			case SUMMARY -> buildSummaryReply(ctx);
			case CROPS -> buildCropsReply(ctx);
			case ALERTS -> buildAlertsReply(ctx);
			case RECOMMENDATIONS -> buildRecommendationsReply(ctx);
			case IRRIGATION -> buildIrrigationReply(ctx);
			case PESTS -> buildPestsReply(ctx);
			case FERTILIZER -> buildFertilizerReply(ctx);
			case UNKNOWN -> null;
		};
	}

	private AssistantIntent detectIntent(String message) {
		if (message == null || message.isBlank()) {
			return AssistantIntent.UNKNOWN;
		}
		String normalized = Normalizer.normalize(message, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "")
				.toLowerCase(Locale.ROOT);
		if (containsAny(normalized, "resumen", "estado de mi finca", "como esta mi finca", "mi finca hoy", "panorama")) {
			return AssistantIntent.SUMMARY;
		}
		if (containsAny(normalized, "alerta", "alertas", "riesgo", "riesgos")) {
			return AssistantIntent.ALERTS;
		}
		if (containsAny(normalized, "recomendacion", "recomendaciones", "pendiente", "pendientes", "debo revisar")) {
			return AssistantIntent.RECOMMENDATIONS;
		}
		if (containsAny(normalized, "regar", "riego", "agua", "humedad", "necesito regar")) {
			return AssistantIntent.IRRIGATION;
		}
		if (containsAny(normalized, "plaga", "plagas", "enfermedad", "enfermedades", "fitosanit", "hongos", "insectos")) {
			return AssistantIntent.PESTS;
		}
		if (containsAny(normalized, "fertil", "abono", "abonar", "nutriente", "nutricion")) {
			return AssistantIntent.FERTILIZER;
		}
		if (normalized.contains("cultivo") && containsAny(normalized,
				"puedes ver", "puede ver", "ver mis", "mis cultivos", "que cultivos",
				"cuales cultivos", "cultivos tengo", "cultivos registrados")) {
			return AssistantIntent.CROPS;
		}
		return AssistantIntent.UNKNOWN;
	}

	private boolean containsAny(String value, String... candidates) {
		for (String candidate : candidates) {
			if (value.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	private String buildSummaryReply(UserContext ctx) {
		StringBuilder sb = new StringBuilder();
		sb.append("Resumen de tu finca hoy:\n");
		if (ctx.crops().isEmpty()) {
			sb.append("- No veo cultivos registrados en tu cuenta.\n");
		} else {
			sb.append("- Cultivos registrados: ").append(ctx.crops().size())
					.append(" (").append(buildCropTypeSummary(ctx.crops())).append(").\n");
		}
		sb.append("- Clima: ").append(ctx.climateInfo()).append(".\n");
		sb.append("- Alertas activas: ").append(ctx.alerts().size()).append(".\n");
		sb.append("- Recomendaciones pendientes: ").append(ctx.recommendations().size()).append(".\n");
		if (!ctx.alerts().isEmpty()) {
			sb.append("Acción recomendada: atiende primero las alertas de mayor riesgo y registra la labor realizada.");
		} else if (!ctx.recommendations().isEmpty()) {
			sb.append("Acción recomendada: revisa las recomendaciones pendientes y marca si las aplicaste.");
		} else if (!ctx.crops().isEmpty()) {
			sb.append("Acción recomendada: registra riego, fertilización u observaciones recientes para mejorar el seguimiento.");
		} else {
			sb.append("Acción recomendada: registra tu primer cultivo para recibir recomendaciones personalizadas.");
		}
		return sb.toString().trim();
	}

	private String buildCropsReply(UserContext ctx) {
		if (ctx.crops().isEmpty()) {
			return "No veo cultivos registrados para tu usuario actual. " +
					"Si ya los registraste, revisa que hayas iniciado sesión con la misma cuenta o sincroniza tus datos.";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Sí. Detecto ").append(ctx.crops().size())
				.append(ctx.crops().size() == 1 ? " cultivo registrado" : " cultivos registrados")
				.append(": ").append(buildCropTypeSummary(ctx.crops())).append(".\n\n");
		sb.append("Detalle:\n");
		for (CropSummary crop : ctx.crops()) {
			sb.append("- ").append(formatCropForContext(crop)).append('\n');
		}
		if (!ctx.alerts().isEmpty()) {
			sb.append("\nAtención: tienes ").append(ctx.alerts().size())
					.append(ctx.alerts().size() == 1 ? " alerta activa" : " alertas activas")
					.append(" sin leer.");
		}
		return sb.toString().trim();
	}

	private String buildAlertsReply(UserContext ctx) {
		if (ctx.alerts().isEmpty()) {
			return "No tienes alertas activas sin leer en este momento. Aun así, revisa tus cultivos después de lluvias, calor fuerte o cambios de humedad.";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Tienes ").append(ctx.alerts().size())
				.append(ctx.alerts().size() == 1 ? " alerta activa" : " alertas activas")
				.append(" sin leer:\n");
		for (AlertSummary alert : ctx.alerts()) {
			sb.append("- ").append(alert.severity().name()).append(" · ")
					.append(alert.cropType().label()).append(": ")
					.append(alert.title()).append(". ").append(alert.message()).append('\n');
		}
		sb.append("\nPrioriza primero las alertas HIGH o críticas y registra la acción que realices.");
		return sb.toString().trim();
	}

	private String buildRecommendationsReply(UserContext ctx) {
		if (ctx.recommendations().isEmpty()) {
			return "No tienes recomendaciones pendientes de decisión. Puedes pedirme una recomendación de riego, fertilización o manejo de plagas según tus cultivos actuales.";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Tienes ").append(ctx.recommendations().size())
				.append(ctx.recommendations().size() == 1 ? " recomendación pendiente" : " recomendaciones pendientes")
				.append(":\n");
		for (RecommendationSummary rec : ctx.recommendations()) {
			sb.append("- ").append(rec.type().name()).append(" · ")
					.append(rec.cropType().label()).append(" · nivel ")
					.append(rec.level().name()).append(": ").append(rec.message()).append('\n');
		}
		return sb.toString().trim();
	}

	private String buildIrrigationReply(UserContext ctx) {
		List<AlertSummary> irrigationAlerts = ctx.alerts().stream()
				.filter(a -> a.type() == RecommendationType.IRRIGATION)
				.toList();
		List<RecommendationSummary> irrigationRecs = ctx.recommendations().stream()
				.filter(r -> r.type() == RecommendationType.IRRIGATION)
				.toList();

		StringBuilder sb = new StringBuilder();
		sb.append("Según el contexto disponible: ").append(ctx.climateInfo()).append(".\n");
		if (!irrigationAlerts.isEmpty()) {
			sb.append("\nAlertas de riego:\n");
			for (AlertSummary alert : irrigationAlerts) {
				sb.append("- ").append(alert.cropType().label()).append(": ").append(alert.message()).append('\n');
			}
		}
		if (!irrigationRecs.isEmpty()) {
			sb.append("\nRecomendaciones de riego pendientes:\n");
			for (RecommendationSummary rec : irrigationRecs) {
				sb.append("- ").append(rec.cropType().label()).append(": ").append(rec.message()).append('\n');
			}
		}
		if (irrigationAlerts.isEmpty() && irrigationRecs.isEmpty()) {
			sb.append("\nNo veo alertas o recomendaciones específicas de riego pendientes. Verifica humedad del suelo antes de regar y evita encharcamientos.");
		}
		return sb.toString().trim();
	}

	private String buildPestsReply(UserContext ctx) {
		if (ctx.crops().isEmpty()) {
			return "No veo cultivos registrados para personalizar plagas. Registra tus cultivos y te diré qué plagas vigilar por tipo.";
		}
		StringBuilder sb = new StringBuilder("Plagas y enfermedades a vigilar según tus cultivos:\n");
		for (Map.Entry<CropType, Integer> entry : countByCropType(ctx.crops()).entrySet()) {
			sb.append("- ").append(entry.getKey().label()).append(" (").append(entry.getValue()).append(")")
					.append(": ").append(entry.getKey().getCommonPests()).append('\n');
		}
		List<AlertSummary> phytoAlerts = ctx.alerts().stream()
				.filter(a -> a.type() == RecommendationType.PHYTOSANITARY)
				.toList();
		if (!phytoAlerts.isEmpty()) {
			sb.append("\nAlertas fitosanitarias activas:\n");
			for (AlertSummary alert : phytoAlerts) {
				sb.append("- ").append(alert.cropType().label()).append(": ").append(alert.message()).append('\n');
			}
		}
		sb.append("\nRevisa hojas, tallos y frutos temprano en la mañana; si ves síntomas, registra un evento con foto o nota.");
		return sb.toString().trim();
	}

	private String buildFertilizerReply(UserContext ctx) {
		List<AlertSummary> fertilizerAlerts = ctx.alerts().stream()
				.filter(a -> a.type() == RecommendationType.FERTILIZER)
				.toList();
		List<RecommendationSummary> fertilizerRecs = ctx.recommendations().stream()
				.filter(r -> r.type() == RecommendationType.FERTILIZER)
				.toList();

		StringBuilder sb = new StringBuilder();
		if (!fertilizerAlerts.isEmpty()) {
			sb.append("Alertas de fertilización:\n");
			for (AlertSummary alert : fertilizerAlerts) {
				sb.append("- ").append(alert.cropType().label()).append(": ").append(alert.message()).append('\n');
			}
		}
		if (!fertilizerRecs.isEmpty()) {
			sb.append(sb.isEmpty() ? "" : "\n").append("Recomendaciones pendientes:\n");
			for (RecommendationSummary rec : fertilizerRecs) {
				sb.append("- ").append(rec.cropType().label()).append(": ").append(rec.message()).append('\n');
			}
		}
		if (sb.isEmpty()) {
			sb.append("No veo recomendaciones pendientes de fertilización. Como regla práctica, ajusta la fertilización según etapa del cultivo, análisis de suelo y lluvias recientes.");
		}
		return sb.toString().trim();
	}

	private String buildCropTypeSummary(List<CropSummary> crops) {
		List<String> parts = new ArrayList<>();
		for (Map.Entry<CropType, Integer> entry : countByCropType(crops).entrySet()) {
			parts.add(entry.getValue() + " de " + entry.getKey().label());
		}
		return String.join(", ", parts);
	}

	private Map<CropType, Integer> countByCropType(List<CropSummary> crops) {
		Map<CropType, Integer> counts = new LinkedHashMap<>();
		for (CropSummary crop : crops) {
			counts.put(crop.cropType(), counts.getOrDefault(crop.cropType(), 0) + 1);
		}
		return counts;
	}

	private String formatCropForContext(CropSummary crop) {
		String area = crop.areaHectares() != null ? crop.areaHectares() + " ha" : "área no registrada";
		String sownDate = crop.sownDate() != null
				? crop.sownDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
				: "fecha de siembra no registrada";
		return crop.cropType().label() + " en " + crop.municipality().getDisplayName() +
				", " + area + ", siembra " + sownDate +
				", etapa estimada " + AssistantPromptBuilder.estimateGrowthStage(crop.sownDate(), crop.cropType());
	}

	private String formatAlertForContext(AlertSummary alert) {
		return "[" + alert.severity().name() + "] " + alert.type().name() + " - " + alert.title() + " (" + alert.cropType().label() + ")";
	}

	private String formatRecommendationForContext(RecommendationSummary rec) {
		return "[" + rec.type().name() + "] " + rec.level().name() + " para " + rec.cropType().label() + ": " + rec.message();
	}

	private String formatEventForContext(EventSummary event) {
		return event.eventType().name() + " en " + event.cropType().label() +
				(event.occurredAt() != null ? ", " + event.occurredAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "");
	}

	private List<String> generateSuggestions(UserContext ctx) {
		List<String> suggestions = new ArrayList<>();
		suggestions.add("Resume mi finca hoy");
		if (!ctx.crops().isEmpty()) {
			suggestions.add("¿Puedes ver mis cultivos?");
		}
		if (!ctx.alerts().isEmpty()) {
			suggestions.add("¿Qué alerta debo atender primero?");
		}
		if (!ctx.recommendations().isEmpty()) {
			suggestions.add("¿Qué recomendaciones debo revisar?");
		}
		if (ctx.climateInfo().contains("temperatura")) {
			suggestions.add("¿Necesito regar hoy?");
		}
		if (!ctx.crops().isEmpty()) {
			CropSummary first = ctx.crops().get(0);
			suggestions.add("¿Qué plagas vigilo en " + first.cropType().label() + "?");
			suggestions.add("¿Qué labor registro hoy?");
		}
		if (suggestions.size() == 1) {
			suggestions.add("¿Cómo registro un nuevo cultivo?");
			suggestions.add("¿Qué plagas debo vigilar?");
		}
		return suggestions.size() > 3 ? suggestions.subList(0, 3) : suggestions;
	}

	public record UserContext(
			List<CropSummary> crops,
			List<AlertSummary> alerts,
			List<RecommendationSummary> recommendations,
			List<EventSummary> events,
			String climateInfo
	) {}

	public record CropSummary(
			UUID id,
			CropType cropType,
			BigDecimal areaHectares,
			Municipality municipality,
			LocalDate sownDate
	) {
		public static CropSummary from(Crop crop) {
			return new CropSummary(
					crop.getId(),
					crop.getCropType(),
					crop.getAreaHectares(),
					crop.getMunicipality(),
					crop.getSownDate()
			);
		}
	}

	public record AlertSummary(
			UUID id,
			RecommendationType type,
			AlertSeverity severity,
			String title,
			String message,
			CropType cropType,
			LocalDateTime createdAt
	) {
		public static AlertSummary from(Alert alert) {
			return new AlertSummary(
					alert.getId(),
					alert.getType(),
					alert.getSeverity(),
					alert.getTitle(),
					alert.getMessage(),
					alert.getCrop().getCropType(),
					alert.getCreatedAt()
			);
		}
	}

	public record RecommendationSummary(
			UUID id,
			RecommendationType type,
			RiskLevel level,
			String message,
			CropType cropType
	) {
		public static RecommendationSummary from(Recommendation rec) {
			return new RecommendationSummary(
					rec.getId(),
					rec.getType(),
					rec.getLevel(),
					rec.getMessage(),
					rec.getCrop().getCropType()
			);
		}
	}

	public record EventSummary(
			UUID id,
			EventType eventType,
			CropType cropType,
			BigDecimal quantity,
			String unit,
			String notes,
			LocalDateTime occurredAt
	) {
		public static EventSummary from(CropEvent event) {
			return new EventSummary(
					event.getId(),
					event.getEventType(),
					event.getCrop().getCropType(),
					event.getQuantity(),
					event.getUnit(),
					event.getNotes(),
					event.getOccurredAt()
			);
		}
	}
}
