package com.agromag.dto.request;

import com.agromag.domain.enums.CropType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CropRequest(
		@NotNull UUID id,
		@NotNull CropType cropType,
		@NotNull @Positive BigDecimal areaHectares,
		@NotBlank String municipality,
		@NotNull @PastOrPresent LocalDate sownDate
) {
}
