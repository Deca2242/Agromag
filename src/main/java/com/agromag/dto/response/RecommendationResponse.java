package com.agromag.dto.response;

import com.agromag.domain.entities.Recommendation;
import com.agromag.domain.enums.RecommendationType;
import com.agromag.domain.enums.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Respuesta con datos de una recomendación
public record RecommendationResponse(
		UUID id,
		RecommendationType type,
		RiskLevel level,
		String message,
		Boolean followed,
		LocalDateTime generatedAt,
		BigDecimal temperature,
		BigDecimal humidity
) {
	public static RecommendationResponse from(Recommendation rec) {
		return new RecommendationResponse(
				rec.getId(),
				rec.getType(),
				rec.getLevel(),
				rec.getMessage(),
				rec.getFollowed(),
				rec.getGeneratedAt(),
				rec.getTemperature(),
				rec.getHumidity()
		);
	}
}
