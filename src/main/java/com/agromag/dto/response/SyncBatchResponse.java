package com.agromag.dto.response;

import java.time.LocalDateTime;
import java.util.List;

// Respuesta del procesamiento de sincronización batch
public record SyncBatchResponse(
		String status,
		String message,
		List<CropResponse> syncedCrops,
		List<CropEventResponse> syncedEvents,
		LocalDateTime timestamp
) {
}
