package com.agromag.dto.response;

import com.agromag.domain.enums.RiskLevel;

import java.time.LocalDateTime;
import java.util.UUID;

public record FertilizerRecommendationResponse(
		UUID id,
		UUID cropId,
		String cropStage,
		Integer weeksSinceSowing,
		String recommendedNutrient,
		String recommendedDose,
		RiskLevel level,
		String message,
		LocalDateTime generatedAt
) {
}
