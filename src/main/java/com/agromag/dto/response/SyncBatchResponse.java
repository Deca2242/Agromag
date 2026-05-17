package com.agromag.dto.response;

import java.time.LocalDateTime;
import java.util.List;

// Respuesta del procesamiento de sincronización batch
public record SyncBatchResponse(
		String status,
		String message,
		List<CropResponse> syncedCrops,
		List<CropEventResponse> syncedEvents,
		List<String> failedCropIds,
		List<String> failedEventIds,
		List<String> failedDecisionIds,
		LocalDateTime timestamp
) {
	public SyncBatchResponse {
		if (failedCropIds == null) failedCropIds = List.of();
		if (failedEventIds == null) failedEventIds = List.of();
		if (failedDecisionIds == null) failedDecisionIds = List.of();
	}
}
