package com.agromag.dto.response;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.enums.CropType;
import com.agromag.domain.enums.Municipality;
import com.agromag.domain.enums.SyncStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// Respuesta con datos de un cultivo
public record CropResponse(
		UUID id,
		CropType cropType,
		BigDecimal areaHectares,
		Municipality municipality,
		LocalDate sownDate,
		SyncStatus syncStatus,
		LocalDateTime createdAt
) {
	public static CropResponse from(Crop crop) {
		return new CropResponse(
				crop.getId(),
				crop.getCropType(),
				crop.getAreaHectares(),
				crop.getMunicipality(),
				crop.getSownDate(),
				crop.getSyncStatus(),
				crop.getCreatedAt()
		);
	}
}
