package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.dto.request.ChatRequest;
import com.agromag.dto.response.ChatResponse;
import com.agromag.dto.response.ProfileResponse;
import com.agromag.repository.CropRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Asistente IA conversacional: responde preguntas agrícolas contextualizadas
// con el perfil del usuario. Stateless: el cliente envía el historial en cada
// llamada y el backend reconstruye el contexto.
// Usa ProfileService.getOrCreateProfile para auto-provisionar el perfil si
// el usuario aún no ha llamado GET /api/profile.
@Service
public class AssistantService {

	private static final int MAX_CROPS_IN_PROMPT = 40;

	private final ChatClient chatClient;
	private final ProfileService profileService;
	private final CropRepository cropRepository;

	public AssistantService(
			ChatClient.Builder chatClientBuilder,
			ProfileService profileService,
			CropRepository cropRepository) {
		this.chatClient = chatClientBuilder.build();
		this.profileService = profileService;
		this.cropRepository = cropRepository;
	}

	public ChatResponse respond(UUID profileId, String email, ChatRequest req) {
		ProfileResponse profile = profileService.getOrCreateProfile(profileId, email);

		List<Crop> crops = cropRepository.findByProfile_Id(profileId);
		String cropsBlock = buildCropsContextBlock(crops);

		String systemPrompt = """
				Eres un asistente agrícola para productores del Magdalena, Colombia.
				El usuario es %s, del municipio %s.
				Da respuestas concisas y prácticas, enfocadas en cultivos de la región
				(banano, mango, yuca, plátano, maíz, palma). Responde siempre en español.
				Usa la siguiente información de finca como referencia; no inventes cultivos
				que no aparezcan ahí. Si no hay cultivos, orienta sobre cómo registrarlos en la app.

				%s
				""".formatted(profile.fullName(), profile.municipality().getDisplayName(), cropsBlock);

		var prompt = chatClient.prompt().system(systemPrompt);

		List<ChatRequest.ChatTurn> history = req.history();
		if (history != null) {
			for (var turn : history) {
				// El SDK de Spring AI no tiene método .assistant() en la cadena fluida,
				// por lo que los turnos previos del asistente se inyectan como sistema.
				if ("user".equals(turn.role())) {
					prompt = prompt.user(turn.content());
				} else if ("assistant".equals(turn.role())) {
					prompt = prompt.system("Asistente (turno anterior): " + turn.content());
				}
			}
		}

		String reply = prompt.user(req.message()).call().content();
		return new ChatResponse(reply != null ? reply : "", Instant.now());
	}

	/** Resumen en español para el system prompt (tope de filas). */
	static String buildCropsContextBlock(List<Crop> crops) {
		if (crops == null || crops.isEmpty()) {
			return "El productor no tiene cultivos registrados en la app.";
		}
		int n = Math.min(crops.size(), MAX_CROPS_IN_PROMPT);
		StringBuilder sb = new StringBuilder();
		sb.append("Cultivos registrados del productor (").append(crops.size());
		if (crops.size() > MAX_CROPS_IN_PROMPT) {
			sb.append("; se listan los primeros ").append(n);
		}
		sb.append("):\n");
		for (int i = 0; i < n; i++) {
			Crop c = crops.get(i);
			sb.append("- Cultivo ").append(i + 1).append(": tipo ")
					.append(c.getCropType().name())
					.append(", área ").append(c.getAreaHectares()).append(" ha")
					.append(", municipio del lote ").append(c.getMunicipality().getDisplayName())
					.append(", siembra ").append(c.getSownDate())
					.append(", estado sync ").append(c.getSyncStatus().name())
					.append(", id ").append(c.getId())
					.append('\n');
		}
		if (crops.size() > MAX_CROPS_IN_PROMPT) {
			sb.append("(Hay más cultivos no listados por límite de contexto.)\n");
		}
		return sb.toString().trim();
	}
}
