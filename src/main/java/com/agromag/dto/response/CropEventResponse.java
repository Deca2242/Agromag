package com.agromag.dto.response;

import com.agromag.domain.enums.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CropEventResponse(
		UUID id,
		EventType eventType,
		BigDecimal quantity,
		String unit,
		String notes,
		LocalDateTime occurredAt
) {
}
