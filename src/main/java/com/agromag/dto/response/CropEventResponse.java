package com.agromag.dto.response;

import com.agromag.domain.entities.CropEvent;
import com.agromag.domain.enums.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Respuesta con datos de un evento de cultivo
public record CropEventResponse(
		UUID id,
		EventType eventType,
		BigDecimal quantity,
		String unit,
		String notes,
		LocalDateTime occurredAt
) {
	public static CropEventResponse from(CropEvent event) {
		return new CropEventResponse(
				event.getId(),
				event.getEventType(),
				event.getQuantity(),
				event.getUnit(),
				event.getNotes(),
				event.getOccurredAt()
		);
	}
}
