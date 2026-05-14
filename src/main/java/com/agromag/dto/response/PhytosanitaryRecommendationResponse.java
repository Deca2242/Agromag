package com.agromag.dto.response;

import com.agromag.domain.enums.RecommendationSource;
import com.agromag.domain.enums.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Respuesta de alerta fitosanitaria — incluye plagas sospechosas y acción preventiva (RF-07, RF-08)
public record PhytosanitaryRecommendationResponse(
		UUID id,
		UUID cropId,
		RiskLevel level,
		String message,
		String suspectedPests,
		BigDecimal temperature,
		BigDecimal humidity,
		LocalDateTime generatedAt,
		RecommendationSource source
) {
	// Crea una copia con nivel y fuente modificados — evita reconstrucción manual del record
	public PhytosanitaryRecommendationResponse withLevelAndSource(RiskLevel newLevel, RecommendationSource newSource) {
		return new PhytosanitaryRecommendationResponse(
				id, cropId, newLevel, message, suspectedPests,
				temperature, humidity, generatedAt, newSource);
	}
}
