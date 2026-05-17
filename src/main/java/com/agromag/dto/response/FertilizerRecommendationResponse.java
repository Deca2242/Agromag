package com.agromag.dto.response;

import com.agromag.domain.enums.RecommendationSource;
import com.agromag.domain.enums.RiskLevel;

import java.time.LocalDateTime;
import java.util.UUID;

// Respuesta de recomendación de fertilización (IA con fallback a reglas)
public record FertilizerRecommendationResponse(
		UUID id,
		UUID cropId,
		String cropStage,
		Integer weeksSinceSowing,
		String recommendedNutrient,
		String recommendedDose,
		RiskLevel level,
		String message,
		LocalDateTime generatedAt,
		RecommendationSource source
) {
}
