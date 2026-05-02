package com.agromag.dto.request;

import com.agromag.domain.enums.EventType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CropEventRequest(
		@NotNull UUID id,
		@NotNull UUID cropId,
		@NotNull EventType eventType,
		BigDecimal quantity,
		String unit,
		String notes,
		@NotNull LocalDateTime occurredAt
) {
}
