package com.agromag.service;

import com.agromag.domain.entities.*;
import com.agromag.domain.enums.Municipality;
import com.agromag.repository.CropTechnicalDataRepository;
import com.agromag.repository.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Builds the AI system prompt by aggregating all available data for a user:
 * profile, crops, technical data, events, recommendations and current climate.
 *
 * <p>This class is intentionally separated from {@link ChatService} to keep
 * prompt construction testable and single-responsibility.</p>
 *
 * @since 1.1
 */
@Component
public class ChatContextBuilder {

	private static final Logger log = LoggerFactory.getLogger(ChatContextBuilder.class);

	private static final int MAX_EVENTS_PER_CROP = 20;
	private static final int MAX_RECOMMENDATIONS_PER_CROP = 10;

	private final CropTechnicalDataRepository technicalDataRepository;
	private final RecommendationRepository recommendationRepository;
	private final ClimateService climateService;

	public ChatContextBuilder(CropTechnicalDataRepository technicalDataRepository,
							  RecommendationRepository recommendationRepository,
							  ClimateService climateService) {
		this.technicalDataRepository = technicalDataRepository;
		this.recommendationRepository = recommendationRepository;
		this.climateService = climateService;
	}

	/**
	 * Constructs the full system prompt with all user data.
	 *
	 * @param profile the user's profile with crops eagerly loaded
	 * @return a formatted system prompt string
	 */
	public String buildSystemPrompt(Profile profile) {
		StringBuilder sb = new StringBuilder();

		sb.append("""
				Eres AgroBot, un asistente agrícola inteligente especializado en el \
				departamento del Magdalena, Colombia. Tienes acceso completo a los \
				datos del productor y debes responder de forma clara, práctica y \
				en español. Usa emojis cuando sea apropiado.
				
				""");

		// Perfil
		sb.append("=== PERFIL DEL PRODUCTOR ===\n");
		sb.append("- Nombre: ").append(profile.getFullName()).append("\n");
		sb.append("- Municipio: ").append(profile.getMunicipality().getDisplayName()).append("\n");
		sb.append("- Cultivos registrados: ").append(profile.getCrops().size()).append("\n\n");

		// Clima actual del municipio del productor
		appendClimate(sb, profile.getMunicipality());

		// Cultivos
		List<Crop> crops = profile.getCrops();
		for (int i = 0; i < crops.size(); i++) {
			Crop crop = crops.get(i);
			appendCropSection(sb, crop, i + 1);
		}

		sb.append("""
				
				=== INSTRUCCIONES ===
				- Responde siempre en español.
				- Sé conciso y orientado a la acción.
				- Cuando no tengas datos suficientes, dilo claramente.
				- Si el productor pregunta algo fuera del ámbito agrícola, \
				redirige amablemente al tema.
				""");

		log.info("build_system_prompt profileId={} crops={} promptLength={}",
				profile.getId(), crops.size(), sb.length());
		return sb.toString();
	}

	private void appendClimate(StringBuilder sb, Municipality municipality) {
		try {
			ClimateData climate = climateService.getCurrentClimate(municipality);
			sb.append("=== CLIMA ACTUAL (").append(municipality.getDisplayName()).append(") ===\n");
			sb.append("- Temperatura: ").append(climate.temperature()).append("°C\n");
			sb.append("- Humedad relativa: ").append(climate.humidity()).append("%\n\n");
		} catch (Exception e) {
			log.warn("chat_context_climate_error municipality={} reason={}",
					municipality, e.getMessage());
			sb.append("=== CLIMA ACTUAL: No disponible ===\n\n");
		}
	}

	private void appendCropSection(StringBuilder sb, Crop crop, int index) {
		long weeks = ChronoUnit.WEEKS.between(crop.getSownDate(), LocalDate.now());

		sb.append("=== CULTIVO ").append(index).append(": ").append(crop.getCropType()).append(" ===\n");
		sb.append("- Área: ").append(crop.getAreaHectares()).append(" ha\n");
		sb.append("- Municipio: ").append(crop.getMunicipality().getDisplayName()).append("\n");
		sb.append("- Fecha de siembra: ").append(crop.getSownDate()).append(" (").append(weeks).append(" semanas)\n");

		// Datos técnicos
		technicalDataRepository.findByCropId(crop.getId()).ifPresent(data -> {
			sb.append("- Datos técnicos (medidos: ").append(data.getMeasuredAt() != null ? data.getMeasuredAt() : "sin fecha").append("):\n");
			appendIfNotNull(sb, "  pH suelo", data.getSoilPh());
			appendIfNotNull(sb, "  Textura", data.getSoilTexture());
			appendIfNotNull(sb, "  CIC", data.getCationExchangeCapacity(), " meq/100g");
			appendIfNotNull(sb, "  N", data.getNitrogenLevel(), " ppm");
			appendIfNotNull(sb, "  P", data.getPhosphorusLevel(), " ppm");
			appendIfNotNull(sb, "  K", data.getPotassiumLevel(), " ppm");
			appendIfNotNull(sb, "  NDVI", data.getNdviIndex());
			appendIfNotNull(sb, "  Clorofila", data.getChlorophyllIndex());
			appendIfNotNull(sb, "  Humedad suelo", data.getSoilMoisture(), "%");
			appendIfNotNull(sb, "  Riego", data.getIrrigationTechnology());
			appendIfNotNull(sb, "  Densidad", data.getPlantingDensity(), " plantas/ha");
			appendIfNotNull(sb, "  Variedad", data.getSeedVariety());
			appendIfNotNull(sb, "  Etapa", data.getCurrentGrowthStage());
			if (data.getSoilDisinfected() != null) {
				sb.append("  Suelo desinfectado: ").append(data.getSoilDisinfected() ? "Sí" : "No").append("\n");
			}
			if (data.getPathogenNotes() != null && !data.getPathogenNotes().isBlank()) {
				sb.append("  Patógenos: ").append(data.getPathogenNotes()).append("\n");
			}
		});

		// Eventos recientes
		List<CropEvent> events = crop.getEvents();
		if (events != null && !events.isEmpty()) {
			int limit = Math.min(events.size(), MAX_EVENTS_PER_CROP);
			sb.append("- Últimos eventos (").append(limit).append("):\n");
			events.stream()
					.sorted((a, b) -> b.getOccurredAt().compareTo(a.getOccurredAt()))
					.limit(limit)
					.forEach(e -> sb.append("  · ").append(e.getEventType())
							.append(" (").append(e.getOccurredAt().toLocalDate()).append(")")
							.append(e.getNotes() != null ? ": " + e.getNotes() : "")
							.append("\n"));
		}

		// Recomendaciones pasadas
		List<Recommendation> recs = recommendationRepository.findByCrop_Id(crop.getId());
		if (!recs.isEmpty()) {
			int limit = Math.min(recs.size(), MAX_RECOMMENDATIONS_PER_CROP);
			sb.append("- Últimas recomendaciones (").append(limit).append("):\n");
			recs.stream()
					.sorted((a, b) -> b.getGeneratedAt().compareTo(a.getGeneratedAt()))
					.limit(limit)
					.forEach(r -> sb.append("  · [").append(r.getType()).append("/").append(r.getLevel()).append("] ")
							.append(r.getMessage().length() > 120 ? r.getMessage().substring(0, 120) + "..." : r.getMessage())
							.append(" (").append(r.getGeneratedAt().toLocalDate()).append(")")
							.append("\n"));
		}

		sb.append("\n");
	}

	private void appendIfNotNull(StringBuilder sb, String label, Object value) {
		if (value != null) {
			sb.append(label).append(": ").append(value).append("\n");
		}
	}

	private void appendIfNotNull(StringBuilder sb, String label, Object value, String suffix) {
		if (value != null) {
			sb.append(label).append(": ").append(value).append(suffix).append("\n");
		}
	}
}
