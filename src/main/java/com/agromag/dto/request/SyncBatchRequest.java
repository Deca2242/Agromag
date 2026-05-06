package com.agromag.dto.request;

import jakarta.validation.Valid;

import java.util.List;

// Lote de sincronización offline → servidor
public record SyncBatchRequest(
		@Valid List<CropRequest> crops,
		@Valid List<CropEventRequest> events,
		@Valid List<RecommendationDecisionRequest> decisions
) {
	public SyncBatchRequest {
		if (crops == null) {
			crops = List.of();
		}
		if (events == null) {
			events = List.of();
		}
		if (decisions == null) {
			decisions = List.of();
		}
	}
}
