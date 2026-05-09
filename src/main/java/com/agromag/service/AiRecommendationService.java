package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.entities.CropTechnicalData;
import com.agromag.domain.enums.RiskLevel;
import com.agromag.dto.response.FertilizerRecommendationResponse;
import com.agromag.dto.response.IrrigationRecommendationResponse;
import com.agromag.exception.AiServiceException;
import com.agromag.repository.CropTechnicalDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Delegates recommendation generation to the AI model (Spring AI / OpenRouter).
 * When the crop has {@link CropTechnicalData}, the prompts are enriched with
 * real field measurements for higher-quality recommendations.
 *
 * @since 1.0
 */
@Service
public class AiRecommendationService {

	private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);

	private final ChatClient chatClient;
	private final ObjectMapper objectMapper;
	private final CropTechnicalDataRepository technicalDataRepository;

	public AiRecommendationService(ChatClient.Builder chatClientBuilder,
								   CropTechnicalDataRepository technicalDataRepository) {
		this.chatClient = chatClientBuilder.build();
		this.objectMapper = new ObjectMapper();
		this.technicalDataRepository = technicalDataRepository;
	}

	// ── Irrigation ────────────────────────────────────────────────────────

	/**
	 * Generates an AI-driven irrigation recommendation enriched with
	 * technical field data when available.
	 *
	 * @param crop the target crop
	 * @param params agronomic reference parameters for the crop type
	 * @param climate current climate conditions
	 * @return the AI response wrapped in an immutable DTO
	 * @throws AiServiceException if the AI call fails
	 */
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
					LocalDateTime.now()
			);
		} catch (AiServiceException e) {
			throw e;
		} catch (Exception e) {
			log.error("ai_irrigation_error cropId={}", crop.getId(), e);
			throw new AiServiceException("Error al generar recomendación de riego con IA", e);
		}
	}

	// ── Fertilizer ────────────────────────────────────────────────────────

	/**
	 * Generates an AI-driven fertilizer recommendation enriched with
	 * technical field data when available.
	 *
	 * @param crop the target crop
	 * @param params agronomic reference parameters for the crop type
	 * @param climate current climate conditions
	 * @return the AI response wrapped in an immutable DTO
	 * @throws AiServiceException if the AI call fails
	 */
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
					LocalDateTime.now()
			);
		} catch (AiServiceException e) {
			throw e;
		} catch (Exception e) {
			log.error("ai_fertilizer_error cropId={}", crop.getId(), e);
			throw new AiServiceException("Error al generar recomendación de fertilización con IA", e);
		}
	}

	// ── AI call + parsing helpers ─────────────────────────────────────────

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

	private RiskLevel parseLevel(JsonNode json, String field, String cropId) {
		String text = safeText(json, field);
		try {
			return RiskLevel.valueOf(text.toUpperCase());
		} catch (IllegalArgumentException e) {
			log.warn("ai_unknown_risk_level cropId={} value={} defaulting_to=MEDIUM", cropId, text);
			return RiskLevel.MEDIUM;
		}
	}

	private String safeText(JsonNode json, String field) {
		if (json == null || !json.has(field) || json.get(field).isNull()) {
			log.warn("ai_missing_field field={}", field);
			return "No disponible";
		}
		return json.get(field).asText();
	}

	// ── Technical data helper ─────────────────────────────────────────────

	/**
	 * Builds a prompt section with field-measured technical data, or an empty
	 * string if no technical data exists for this crop.
	 *
	 * @param crop the crop to look up technical data for
	 * @return a formatted prompt section or empty string
	 */
	private String buildTechnicalDataSection(Crop crop) {
		Optional<CropTechnicalData> opt = technicalDataRepository.findByCropId(crop.getId());
		if (opt.isEmpty()) {
			return "";
		}
		CropTechnicalData td = opt.get();
		StringBuilder sb = new StringBuilder("\nDatos técnicos medidos en campo");
		if (td.getMeasuredAt() != null) {
			sb.append(" (fecha: ").append(td.getMeasuredAt().toLocalDate()).append(")");
		}
		sb.append(":\n");

		appendIfPresent(sb, "pH del suelo", td.getSoilPh());
		appendIfPresent(sb, "Textura del suelo", td.getSoilTexture());
		appendIfPresent(sb, "Estructura del suelo", td.getSoilStructure());
		appendIfPresent(sb, "CIC", td.getCationExchangeCapacity(), " meq/100g");
		appendIfPresent(sb, "Nitrógeno (N)", td.getNitrogenLevel(), " ppm");
		appendIfPresent(sb, "Fósforo (P)", td.getPhosphorusLevel(), " ppm");
		appendIfPresent(sb, "Potasio (K)", td.getPotassiumLevel(), " ppm");
		appendIfPresent(sb, "Índice de clorofila (SPAD)", td.getChlorophyllIndex());
		appendIfPresent(sb, "Índice NDVI", td.getNdviIndex());
		appendIfPresent(sb, "Humedad del suelo", td.getSoilMoisture(), "%");
		appendIfPresent(sb, "Temperatura de campo", td.getFieldTemperature(), "°C");
		appendIfPresent(sb, "Precipitación", td.getPrecipitation(), " mm");
		appendIfPresent(sb, "Radiación solar", td.getSolarRadiation(), " W/m²");
		appendIfPresent(sb, "Velocidad del viento", td.getWindSpeed(), " km/h");
		appendIfPresent(sb, "Tecnología de riego", td.getIrrigationTechnology());
		appendIfPresent(sb, "Densidad de siembra", td.getPlantingDensity(), " plantas/ha");
		appendIfPresent(sb, "Variedad de semilla", td.getSeedVariety());
		appendIfPresent(sb, "Etapa de crecimiento", td.getCurrentGrowthStage());

		if (td.getSoilDisinfected() != null) {
			sb.append("- Suelo desinfectado: ").append(td.getSoilDisinfected() ? "Sí" : "No").append("\n");
		}
		if (td.getSeedAdaptedToZone() != null) {
			sb.append("- Semilla adaptada a la zona: ").append(td.getSeedAdaptedToZone() ? "Sí" : "No").append("\n");
		}
		if (td.getPathogenNotes() != null && !td.getPathogenNotes().isBlank()) {
			sb.append("- Patógenos/malezas detectados: ").append(td.getPathogenNotes()).append("\n");
		}

		return sb.toString();
	}

	private void appendIfPresent(StringBuilder sb, String label, Object value) {
		if (value != null) {
			sb.append("- ").append(label).append(": ").append(value).append("\n");
		}
	}

	private void appendIfPresent(StringBuilder sb, String label, Object value, String suffix) {
		if (value != null) {
			sb.append("- ").append(label).append(": ").append(value).append(suffix).append("\n");
		}
	}

	// ── Prompt builders ───────────────────────────────────────────────────

	private String buildIrrigationPrompt(Crop crop, CropParameter params, ClimateData climate) {
		String technicalSection = buildTechnicalDataSection(crop);

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
				%s
				Con base en estos datos, indica si el productor debe regar hoy, incluyendo:
				1. Nivel de urgencia (LOW, MEDIUM o HIGH)
				2. Mensaje claro en español: debe decir si regar o no, y cuánta agua aproximada

				Responde ÚNICAMENTE en formato JSON con las claves: level, message
				""".formatted(
				crop.getCropType(),
				crop.getMunicipality().getDisplayName(),
				params.getIrrigationNeeds(),
				params.getOptimalTempMin(), params.getOptimalTempMax(),
				params.getHumidityMin(), params.getHumidityMax(),
				climate.temperature(),
				climate.humidity(),
				technicalSection);
	}

	private String buildFertilizerPrompt(Crop crop, CropParameter params,
			ClimateData climate, long weeksSinceSowing) {
		String technicalSection = buildTechnicalDataSection(crop);

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
				%s
				Con base en la etapa actual del cultivo, los parámetros agronómicos, \
				las condiciones ambientales y los datos técnicos de campo (si están \
				disponibles), proporciona una recomendación de fertilización que incluya:
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
				climate.humidity(),
				technicalSection);
	}
}
