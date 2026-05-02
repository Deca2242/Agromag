package com.agromag.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecommendationDecisionRequest(
		@NotNull UUID recommendationId,
		@NotNull Boolean followed
) {
}
