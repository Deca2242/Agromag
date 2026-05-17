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
	// Crea una copia con nivel y fuente modificados — evita reconstrucción manual del record
	public IrrigationRecommendationResponse withLevelAndSource(RiskLevel newLevel, RecommendationSource newSource) {
		return new IrrigationRecommendationResponse(id, cropId, newLevel, message, temperature, generatedAt, newSource);
	}
}
