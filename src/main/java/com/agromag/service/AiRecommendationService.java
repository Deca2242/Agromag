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

			String raw = callAi(prompt);
			JsonNode json = objectMapper.readTree(raw);

			RiskLevel level = parseLevel(json, "level", crop.getId().toString());
			String message = safeText(json, "message");

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

			String raw = callAi(prompt);
			JsonNode json = objectMapper.readTree(raw);

			String cropStage = safeText(json, "cropStage");
			String recommendedNutrient = safeText(json, "recommendedNutrient");
			String recommendedDose = safeText(json, "recommendedDose");
			RiskLevel level = parseLevel(json, "level", crop.getId().toString());
			String message = safeText(json, "message");

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

			String raw = callAi(prompt);
			JsonNode json = objectMapper.readTree(raw);

			RiskLevel level = parseLevel(json, "level", crop.getId().toString());
			String suspectedPests = safeText(json, "suspectedPests");
			String preventiveAction = safeText(json, "preventiveAction");
			String aiMessage = safeText(json, "message");

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

	// Llama al modelo de IA y limpia los bloques de código Markdown de la respuesta
	private String callAi(String prompt) {
		String response = chatClient.prompt()
				.user(prompt)
				.call()
				.content();
		if (response == null) {
			throw new AiServiceException("La IA devolvió una respuesta vacía", null);
		}
		return response.replaceAll("```json", "").replaceAll("```", "").trim();
	}

	// Parsea el nivel de riesgo del JSON; si la IA devuelve un valor desconocido, usa MEDIUM
	private RiskLevel parseLevel(JsonNode json, String field, String cropId) {
		String text = safeText(json, field);
		try {
			return RiskLevel.valueOf(text.toUpperCase());
		} catch (IllegalArgumentException e) {
			log.warn("ai_unknown_risk_level cropId={} value={} defaulting_to=MEDIUM", cropId, text);
			return RiskLevel.MEDIUM;
		}
	}

	// Extrae texto de un campo JSON de forma segura — devuelve "No disponible" si el campo falta
	private String safeText(JsonNode json, String field) {
		if (json == null || !json.has(field) || json.get(field).isNull()) {
			log.warn("ai_missing_field field={}", field);
			return "No disponible";
		}
		return json.get(field).asText();
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
				2. Mensaje claro en español con ícono de gota: debe decir si regar o no, y cuánta agua aproximada

				Responde ÚNICAMENTE en formato JSON con las claves: level, message
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
				4. Mensaje breve en español con ícono de insecto, sin tecnicismos

				Responde ÚNICAMENTE en formato JSON con las claves:
				level, suspectedPests, preventiveAction, message
				""".formatted(
				crop.getCropType(),
				crop.getMunicipality().getDisplayName(),
				params.getGrowthCycleDays(),
				climate.temperature(),
				climate.humidity(),
				ruleLevel.name());
	}
}
