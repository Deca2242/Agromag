package com.agromag.dto.request;

import java.util.List;

public record SyncBatchRequest(
		List<CropRequest> crops,
		List<CropEventRequest> events,
		List<RecommendationDecisionRequest> decisions
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
