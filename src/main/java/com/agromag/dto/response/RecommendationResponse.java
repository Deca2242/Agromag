package com.agromag.dto.response;

import com.agromag.domain.enums.RecommendationType;
import com.agromag.domain.enums.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
}
