package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.enums.RecommendationType;
import com.agromag.domain.enums.RiskLevel;
import com.agromag.dto.response.FertilizerRecommendationResponse;
import com.agromag.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Servicio que delega a Spring AI (OpenAI) la generación de
 * recomendaciones de fertilización basadas en datos del cultivo,
 * parámetros agronómicos y condiciones climáticas.
 */
@Service
public class AiRecommendationService {

	private final ChatClient chatClient;
	private final ObjectMapper objectMapper;

	public AiRecommendationService(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Genera una recomendación de fertilización usando IA.
	 * Construye un prompt agronómico con los datos del cultivo
	 * y parsea la respuesta JSON del modelo.
	 */
	public FertilizerRecommendationResponse generateFertilizerRecommendation(
			Crop crop, CropParameter params, ClimateData climate) {
		try {
			long weeksSinceSowing = ChronoUnit.WEEKS.between(crop.getSownDate(), LocalDate.now());

			String prompt = buildFertilizerPrompt(crop, params, climate, weeksSinceSowing);

			String aiResponse = chatClient.prompt()
					.user(prompt)
					.call()
					.content();

			JsonNode json = objectMapper.readTree(aiResponse);

			String cropStage = json.get("cropStage").asText();
			String recommendedNutrient = json.get("recommendedNutrient").asText();
			String recommendedDose = json.get("recommendedDose").asText();
			RiskLevel level = RiskLevel.valueOf(json.get("level").asText().toUpperCase());
			String message = json.get("message").asText();

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
			throw new AiServiceException("Error al generar recomendación de fertilización con IA", e);
		}
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
				climate.humidity()
		);
	}
}
