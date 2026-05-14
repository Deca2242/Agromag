package com.agromag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

// Umbrales configurables para los motores de reglas de recomendación
@ConfigurationProperties(prefix = "agromag.recommendation")
public record RecommendationProperties(
		BigDecimal phytoTempThreshold,
		BigDecimal phytoHumidityThreshold,
		BigDecimal irrigationNearMaxDelta,
		int fertilizerInitialStageWeeks,
		int fertilizerVegetativeStageWeeks
) {}
