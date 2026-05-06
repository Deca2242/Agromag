package com.agromag.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Request para marcar si el usuario siguió o no una recomendación
public record RecommendationDecisionRequest(
		@NotNull UUID recommendationId,
		@NotNull Boolean followed
) {
}
