package com.agromag.dto.response;

import com.agromag.domain.enums.RecommendationSource;
import com.agromag.domain.enums.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Respuesta de recomendación de riego basada en clima
public record IrrigationRecommendationResponse(
		UUID id,
		UUID cropId,
		RiskLevel level,
		String message,
		BigDecimal temperature,
		LocalDateTime generatedAt,
		RecommendationSource source
) {
}
