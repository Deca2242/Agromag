package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.enums.RiskLevel;
import com.agromag.dto.response.FertilizerRecommendationResponse;
import com.agromag.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

// Delega a Spring AI (OpenAI) la generación de recomendaciones de fertilización
@Service
public class AiRecommendationService {

	private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);

	private final ChatClient chatClient;
	private final ObjectMapper objectMapper;

	public AiRecommendationService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
		this.chatClient = chatClientBuilder.build();
		this.objectMapper = objectMapper;
	}

	// Genera recomendación de fertilización usando IA, construyendo un prompt agronómico
	public FertilizerRecommendationResponse generateFertilizerRecommendation(
			Crop crop, CropParameter params, ClimateData climate) {
		try {
			long weeksSinceSowing = ChronoUnit.WEEKS.between(crop.getSownDate(), LocalDate.now());

			String prompt = buildFertilizerPrompt(crop, params, climate, weeksSinceSowing);

			log.info("ai_request cropId={} weeks={}", crop.getId(), weeksSinceSowing);

			String aiResponse = chatClient.prompt()
					.user(prompt)
					.call()
					.content();

			// Remove possible markdown formatting from the response
			if (aiResponse != null) {
				aiResponse = aiResponse.replaceAll("```json", "").replaceAll("```", "").trim();
			}

			JsonNode json = objectMapper.readTree(aiResponse);

			// Parseo seguro: verificar que cada campo exista antes de leerlo
			String cropStage = safeText(json, "cropStage");
			String recommendedNutrient = safeText(json, "recommendedNutrient");
			String recommendedDose = safeText(json, "recommendedDose");
			String levelText = safeText(json, "level");
			String message = safeText(json, "message");

			RiskLevel level;
			try {
				level = RiskLevel.valueOf(levelText.toUpperCase());
			} catch (IllegalArgumentException e) {
				log.warn("ai_unknown_risk_level value={} defaulting_to=MEDIUM", levelText);
				level = RiskLevel.MEDIUM;
			}

			log.info("ai_response cropId={} stage={} level={}", crop.getId(), cropStage, level);

			return new FertilizerRecommendationResponse(
					UUID.randomUUID(),
					crop.getId(),
					cropStage,
					(int) weeksSinceSowing,
					recommendedNutrient,
					recommendedDose,
					level,
					message,
					LocalDateTime.now());

		} catch (AiServiceException e) {
			throw e;
		} catch (Exception e) {
			log.error("ai_error cropId={}", crop.getId(), e);
			throw new AiServiceException("Error al generar recomendación de fertilización con IA", e);
		}
	}

	// Extrae texto de un JsonNode de forma segura — devuelve "No disponible" si falta
	private String safeText(JsonNode json, String field) {
		if (json == null || !json.has(field) || json.get(field).isNull()) {
			log.warn("ai_missing_field field={}", field);
			return "No disponible";
		}
		return json.get(field).asText();
	}

	private String buildFertilizerPrompt(Crop crop, CropParameter params,
			ClimateData climate, long weeksSinceSowing) {
		return """
				Eres un agrónomo experto en cultivos tropicales del Magdalena, Colombia.

				Datos del cultivo:
				- Tipo: %s
				- Fecha de siembra: %s
				- Semanas desde la siembra: %d
				- Área: %s hectáreas
				- Municipio: %s
				- Distancia de siembra sugerida: %s
				- Profundidad de siembra: %s cm
				- Ciclo de crecimiento: %d días

				Parámetros agronómicos de referencia:
				- Rango de temperatura óptima: %s°C – %s°C
				- Rango de humedad óptima: %s%% – %s%%
				- Rango de pH del suelo: %s – %s
				- Rango de conductividad eléctrica (EC): %s – %s
				- Necesidades de riego: %s
				- Fertilizante recomendado base: %s

				Condiciones climáticas actuales:
				- Temperatura: %s°C
				- Humedad relativa: %s%%

				Con base en la etapa actual del cultivo, los parámetros agronómicos \
				y las condiciones ambientales, proporciona una recomendación de \
				fertilización que incluya:
				1. Etapa actual del cultivo
				2. Nutriente recomendado (N, P, K, u otro)
				3. Dosis recomendada por hectárea
				4. Nivel de riesgo si no se aplica (LOW, MEDIUM o HIGH)
				5. Mensaje explicativo breve en español

				Responde ÚNICAMENTE en formato JSON con las claves:
				cropStage, recommendedNutrient, recommendedDose, level, message
				""".formatted(
				crop.getCropType(),
				crop.getSownDate(),
				weeksSinceSowing,
				crop.getAreaHectares(),
				crop.getMunicipality().getDisplayName(),
				params.getSuggestedSpacing(),
				params.getPlantingDepthCm(),
				params.getGrowthCycleDays(),
				params.getOptimalTempMin(),
				params.getOptimalTempMax(),
				params.getHumidityMin(),
				params.getHumidityMax(),
				params.getPhMin(),
				params.getPhMax(),
				params.getEcMin(),
				params.getEcMax(),
				params.getIrrigationNeeds(),
				params.getRecommendedFertilizer(),
				climate.temperature(),
				climate.humidity());
	}
}
