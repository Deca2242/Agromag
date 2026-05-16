package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.enums.RecommendationSource;
import com.agromag.domain.enums.RiskLevel;
import com.agromag.domain.model.ClimateData;
import com.agromag.dto.response.FertilizerRecommendationResponse;
import com.agromag.dto.response.IrrigationRecommendationResponse;
import com.agromag.dto.response.PhytosanitaryRecommendationResponse;
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

// Delega la generación de recomendaciones al modelo de IA (Spring AI / OpenRouter)
@Service
public class AiRecommendationService {

	private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);

	private final ChatClient chatClient;
	private final ObjectMapper objectMapper;

	public AiRecommendationService(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
		this.chatClient = chatClientBuilder.build();
		this.objectMapper = objectMapper;
	}

	public IrrigationRecommendationResponse generateIrrigationRecommendation(
			Crop crop, CropParameter params, ClimateData climate) {
		try {
			String prompt = buildIrrigationPrompt(crop, params, climate);
			log.info("ai_irrigation_request cropId={}", crop.getId());

			JsonNode json = callAiForJson(prompt);

			RiskLevel level = parseLevel(json, "level", crop.getId().toString());
			String message = requireText(json, "message", crop.getId().toString());

			log.info("ai_irrigation_response cropId={} level={}", crop.getId(), level);

			return new IrrigationRecommendationResponse(
					UUID.randomUUID(),
					crop.getId(),
					level,
					message,
					climate.temperature(),
					LocalDateTime.now(),
					RecommendationSource.AI
			);
		} catch (Exception e) {
			if (e instanceof AiServiceException ase) throw ase;
			log.error("ai_irrigation_error cropId={}", crop.getId(), e);
			throw new AiServiceException("Error al generar recomendación de riego con IA", e);
		}
	}

	public FertilizerRecommendationResponse generateFertilizerRecommendation(
			Crop crop, CropParameter params, ClimateData climate) {
		try {
			long weeksSinceSowing = ChronoUnit.WEEKS.between(crop.getSownDate(), LocalDate.now());
			String prompt = buildFertilizerPrompt(crop, params, climate, weeksSinceSowing);

			log.info("ai_fertilizer_request cropId={} weeks={}", crop.getId(), weeksSinceSowing);

			JsonNode json = callAiForJson(prompt);

			String cropStage = requireText(json, "cropStage", crop.getId().toString());
			String recommendedNutrient = requireText(json, "recommendedNutrient", crop.getId().toString());
			String recommendedDose = requireText(json, "recommendedDose", crop.getId().toString());
			RiskLevel level = parseLevel(json, "level", crop.getId().toString());
			String message = requireText(json, "message", crop.getId().toString());

			log.info("ai_fertilizer_response cropId={} stage={} level={}", crop.getId(), cropStage, level);

			return new FertilizerRecommendationResponse(
					UUID.randomUUID(),
					crop.getId(),
					cropStage,
					(int) weeksSinceSowing,
					recommendedNutrient,
					recommendedDose,
					level,
					message,
					LocalDateTime.now(),
					RecommendationSource.AI
			);
		} catch (Exception e) {
			if (e instanceof AiServiceException ase) throw ase;
			log.error("ai_fertilizer_error cropId={}", crop.getId(), e);
			throw new AiServiceException("Error al generar recomendación de fertilización con IA", e);
		}
	}

	public PhytosanitaryRecommendationResponse generatePhytosanitaryRecommendation(
			Crop crop, CropParameter params, ClimateData climate, RiskLevel ruleLevel) {
		try {
			String prompt = buildPhytosanitaryPrompt(crop, params, climate, ruleLevel);
			log.info("ai_phytosanitary_request cropId={} ruleLevel={}", crop.getId(), ruleLevel);

			JsonNode json = callAiForJson(prompt);

			RiskLevel level = parseLevel(json, "level", crop.getId().toString());
			String suspectedPests = requireText(json, "suspectedPests", crop.getId().toString());
			String preventiveAction = requireText(json, "preventiveAction", crop.getId().toString());
			String aiMessage = requireText(json, "message", crop.getId().toString());

			// El mensaje final combina el diagnóstico de la IA con la acción preventiva recomendada
			String fullMessage = aiMessage + " Acción: " + preventiveAction;

			log.info("ai_phytosanitary_response cropId={} level={} pests={}", crop.getId(), level, suspectedPests);

			return new PhytosanitaryRecommendationResponse(
					UUID.randomUUID(),
					crop.getId(),
					level,
					fullMessage,
					suspectedPests,
					climate.temperature(),
					climate.humidity(),
					LocalDateTime.now(),
					RecommendationSource.AI
			);
		} catch (Exception e) {
			if (e instanceof AiServiceException ase) throw ase;
			log.error("ai_phytosanitary_error cropId={}", crop.getId(), e);
			throw new AiServiceException("Error al generar alerta fitosanitaria con IA", e);
		}
	}

	// Llama al modelo de IA y extrae un objeto JSON aunque el modelo agregue texto alrededor.
	private JsonNode callAiForJson(String prompt) {
		String response = chatClient.prompt()
				.user(prompt)
				.call()
				.content();
		if (response == null) {
			throw new AiServiceException("La IA devolvió una respuesta vacía", null);
		}
		try {
			return objectMapper.readTree(extractJsonObject(response));
		} catch (Exception e) {
			log.warn("ai_invalid_json raw={}", sanitizeAiText(response));
			throw new AiServiceException("La IA devolvió JSON inválido", e);
		}
	}

	private String extractJsonObject(String response) {
		String cleaned = response
				.replace("```json", "")
				.replace("```JSON", "")
				.replace("```", "")
				.trim();
		int start = cleaned.indexOf('{');
		int end = cleaned.lastIndexOf('}');
		if (start < 0 || end <= start) {
			throw new AiServiceException("La IA no devolvió un objeto JSON", null);
		}
		return cleaned.substring(start, end + 1);
	}

	// Parsea el nivel de riesgo del JSON; si la IA devuelve un valor desconocido, usa MEDIUM
	private RiskLevel parseLevel(JsonNode json, String field, String cropId) {
		String text = requireText(json, field, cropId);
		try {
			return RiskLevel.valueOf(text.toUpperCase());
		} catch (IllegalArgumentException e) {
			log.warn("ai_unknown_risk_level cropId={} value={} defaulting_to=MEDIUM", cropId, text);
			return RiskLevel.MEDIUM;
		}
	}

	private String requireText(JsonNode json, String field, String cropId) {
		if (json == null || !json.has(field) || json.get(field).isNull() || json.get(field).asText().isBlank()) {
			log.warn("ai_missing_required_field cropId={} field={}", cropId, field);
			throw new AiServiceException("La IA omitió un campo requerido: " + field, null);
		}
		return sanitizeAiText(json.get(field).asText());
	}

	private String sanitizeAiText(String text) {
		if (text == null) {
			return "";
		}
		return text
				.replace("**", "")
				.replace("__", "")
				.replace("```json", "")
				.replace("```", "")
				.trim();
	}

	private String buildIrrigationPrompt(Crop crop, CropParameter params, ClimateData climate) {
		return """
				Eres un agrónomo experto en cultivos tropicales del Magdalena, Colombia. \
				Aplicas el método FAO simplificado para calcular necesidades de riego.

				Datos del cultivo:
				- Tipo: %s
				- Municipio: %s
				- Necesidades de riego de referencia: %s
				- Temperatura óptima: %s°C – %s°C
				- Humedad óptima: %s%% – %s%%

				Condiciones climáticas actuales:
				- Temperatura: %s°C
				- Humedad relativa: %s%%

				Con base en estos datos, indica si el productor debe regar hoy, incluyendo:
				1. Nivel de urgencia (LOW, MEDIUM o HIGH)
				2. Mensaje claro en español: debe decir si regar o no, cantidad orientativa y precaución contra encharcamiento

				No inventes cálculos exactos de lámina si no hay datos suficientes; usa la referencia del cultivo como orientación.
				Responde ÚNICAMENTE en JSON válido con las claves: level, message.
				No uses Markdown, emojis, bloques de código ni texto antes o después del JSON.
				""".formatted(
				crop.getCropType(),
				crop.getMunicipality().getDisplayName(),
				params.getIrrigationNeeds(),
				params.getOptimalTempMin(), params.getOptimalTempMax(),
				params.getHumidityMin(), params.getHumidityMax(),
				climate.temperature(),
				climate.humidity());
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
				5. Mensaje explicativo breve en español con precaución y momento sugerido de aplicación

				Si no hay análisis de suelo, indica que la dosis es orientativa y debe validarse con un técnico local.
				No recomiendes mezclas peligrosas ni excedas rangos razonables sin advertencia.
				Responde ÚNICAMENTE en JSON válido con las claves:
				cropStage, recommendedNutrient, recommendedDose, level, message.
				No uses Markdown, emojis, bloques de código ni texto antes o después del JSON.
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

	private String buildPhytosanitaryPrompt(Crop crop, CropParameter params,
			ClimateData climate, RiskLevel ruleLevel) {
		return """
				Eres un agrónomo experto en manejo fitosanitario de cultivos tropicales \
				del Magdalena, Colombia. Conoces las plagas comunes de la región: \
				mosca blanca (Bemisia tabaci), sigatoka negra (Mycosphaerella fijiensis), \
				broca del café (Hypothenemus hampei), trips, ácaros y pudrición de raíces.

				Datos del cultivo:
				- Tipo: %s
				- Municipio: %s
				- Ciclo de crecimiento: %d días

				Condiciones climáticas actuales:
				- Temperatura: %s°C
				- Humedad relativa: %s%%

				Evaluación preliminar de riesgo (basada en umbrales FAO/PDF): %s

				Con base en las condiciones climáticas y el tipo de cultivo, proporciona:
				1. Nivel de riesgo fitosanitario real (LOW, MEDIUM o HIGH)
				2. Plagas o enfermedades sospechosas para este cultivo en estas condiciones
				3. Acción preventiva concreta que el productor puede realizar hoy
				4. Mensaje breve en español, sin tecnicismos

				No diagnostiques una plaga como confirmada. Habla de riesgo probable y recomienda inspección visual.
				No recomiendes químicos específicos sin confirmación técnica.
				Responde ÚNICAMENTE en JSON válido con las claves:
				level, suspectedPests, preventiveAction, message.
				No uses Markdown, emojis, bloques de código ni texto antes o después del JSON.
				""".formatted(
				crop.getCropType(),
				crop.getMunicipality().getDisplayName(),
				params.getGrowthCycleDays(),
				climate.temperature(),
				climate.humidity(),
				ruleLevel.name());
	}
}
