package com.agromag.dto.request;

import com.agromag.domain.enums.EventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Request para crear/actualizar un evento de cultivo
public record CropEventRequest(
		@NotNull UUID id,
		@NotNull UUID cropId,
		@NotNull EventType eventType,
		@Positive BigDecimal quantity,
		String unit,
		String notes,
		@NotNull LocalDateTime occurredAt
) {
}
