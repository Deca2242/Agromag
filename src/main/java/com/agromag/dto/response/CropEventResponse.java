package com.agromag.dto.response;

import com.agromag.domain.entities.CropEvent;
import com.agromag.domain.enums.EventType;
import com.agromag.domain.enums.SyncStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Respuesta con datos de un evento de cultivo
public record CropEventResponse(
		UUID id,
		UUID cropId,
		EventType eventType,
		BigDecimal quantity,
		String unit,
		String notes,
		LocalDateTime occurredAt,
		SyncStatus syncStatus
) {
	public static CropEventResponse from(CropEvent event) {
		return new CropEventResponse(
				event.getId(),
				event.getCrop().getId(),
				event.getEventType(),
				event.getQuantity(),
				event.getUnit(),
				event.getNotes(),
				event.getOccurredAt(),
				event.getSyncStatus()
		);
	}
}
