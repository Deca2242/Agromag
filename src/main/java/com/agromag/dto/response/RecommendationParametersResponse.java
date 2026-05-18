package com.agromag.dto.response;

import com.agromag.config.RecommendationProperties;
import com.agromag.domain.entities.CropParameter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// Respuesta del endpoint GET /api/recommendations/parameters.
// Expone los umbrales del motor de reglas y los parámetros agronómicos
// por tipo de cultivo para que el cliente Flutter pueda mantener
// el OfflineRuleEngine sincronizado con la configuración del servidor.
public record RecommendationParametersResponse(
		BigDecimal phytoTempThreshold,
		BigDecimal phytoHumidityThreshold,
		BigDecimal irrigationNearMaxDelta,
		int fertilizerInitialStageWeeks,
		int fertilizerVegetativeStageWeeks,
		List<CropParameterEntry> cropParameters
) {

	public record CropParameterEntry(
			String cropType,
			BigDecimal optimalTempMin,
			BigDecimal optimalTempMax,
			BigDecimal humidityMin,
			BigDecimal humidityMax,
			String irrigationNeeds,
			String recommendedFertilizer,
			int growthCycleDays
	) {
		public static CropParameterEntry from(CropParameter cp) {
			return new CropParameterEntry(
					cp.getCropType().name(),
					cp.getOptimalTempMin(),
					cp.getOptimalTempMax(),
					cp.getHumidityMin(),
					cp.getHumidityMax(),
					cp.getIrrigationNeeds(),
					cp.getRecommendedFertilizer(),
					cp.getGrowthCycleDays()
			);
		}
	}

	public static RecommendationParametersResponse from(
			RecommendationProperties props,
			List<CropParameter> cropParams
	) {
		return new RecommendationParametersResponse(
				props.phytoTempThreshold(),
				props.phytoHumidityThreshold(),
				props.irrigationNearMaxDelta(),
				props.fertilizerInitialStageWeeks(),
				props.fertilizerVegetativeStageWeeks(),
				cropParams.stream().map(CropParameterEntry::from).toList()
		);
	}

	// Convierte a mapa plano clave-valor para el cliente Flutter.
	// Los parámetros de cultivo se serializan como cropType.campo=valor.
	public Map<String, String> toFlatMap() {
		var map = new java.util.LinkedHashMap<String, String>();
		map.put("phytoTempThreshold", phytoTempThreshold.toPlainString());
		map.put("phytoHumidityThreshold", phytoHumidityThreshold.toPlainString());
		map.put("irrigationNearMaxDelta", irrigationNearMaxDelta.toPlainString());
		map.put("fertilizerInitialStageWeeks", String.valueOf(fertilizerInitialStageWeeks));
		map.put("fertilizerVegetativeStageWeeks", String.valueOf(fertilizerVegetativeStageWeeks));
		for (var cp : cropParameters) {
			String prefix = "crop." + cp.cropType().toLowerCase() + ".";
			map.put(prefix + "optimalTempMin", cp.optimalTempMin().toPlainString());
			map.put(prefix + "optimalTempMax", cp.optimalTempMax().toPlainString());
			map.put(prefix + "humidityMin", cp.humidityMin().toPlainString());
			map.put(prefix + "humidityMax", cp.humidityMax().toPlainString());
			map.put(prefix + "irrigationNeeds", cp.irrigationNeeds());
			map.put(prefix + "recommendedFertilizer", cp.recommendedFertilizer());
			map.put(prefix + "growthCycleDays", String.valueOf(cp.growthCycleDays()));
		}
		return map;
	}
}
