package com.agromag.dto.response;

import com.agromag.domain.enums.CropType;
import com.agromag.domain.enums.SyncStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CropResponse(
		UUID id,
		CropType cropType,
		BigDecimal areaHectares,
		String municipality,
		LocalDate sownDate,
		SyncStatus syncStatus,
		LocalDateTime createdAt
) {
}
