package com.agromag.dto.response;

import com.agromag.domain.entities.CropTechnicalData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable response DTO for a crop's technical data sheet.
 */
public record CropTechnicalDataResponse(
		UUID cropId,
		// Suelo y nutrición
		BigDecimal soilPh,
		String soilTexture,
		String soilStructure,
		BigDecimal cationExchangeCapacity,
		BigDecimal nitrogenLevel,
		BigDecimal phosphorusLevel,
		BigDecimal potassiumLevel,
		BigDecimal chlorophyllIndex,
		BigDecimal ndviIndex,
		Boolean soilDisinfected,
		String pathogenNotes,
		// Clima y riego
		BigDecimal soilMoisture,
		BigDecimal fieldTemperature,
		BigDecimal precipitation,
		BigDecimal solarRadiation,
		BigDecimal windSpeed,
		String irrigationTechnology,
		// Siembra y desarrollo
		BigDecimal plantingDensity,
		String seedVariety,
		Boolean seedAdaptedToZone,
		LocalDate optimalSowingStart,
		LocalDate optimalSowingEnd,
		String currentGrowthStage,
		// Timestamps
		LocalDateTime measuredAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
	/**
	 * Factory method to convert a JPA entity into this DTO.
	 *
	 * @param data the technical data entity
	 * @return an immutable response record
	 */
	public static CropTechnicalDataResponse from(CropTechnicalData data) {
		return new CropTechnicalDataResponse(
				data.getCrop().getId(),
				data.getSoilPh(),
				data.getSoilTexture(),
				data.getSoilStructure(),
				data.getCationExchangeCapacity(),
				data.getNitrogenLevel(),
				data.getPhosphorusLevel(),
				data.getPotassiumLevel(),
				data.getChlorophyllIndex(),
				data.getNdviIndex(),
				data.getSoilDisinfected(),
				data.getPathogenNotes(),
				data.getSoilMoisture(),
				data.getFieldTemperature(),
				data.getPrecipitation(),
				data.getSolarRadiation(),
				data.getWindSpeed(),
				data.getIrrigationTechnology(),
				data.getPlantingDensity(),
				data.getSeedVariety(),
				data.getSeedAdaptedToZone(),
				data.getOptimalSowingStart(),
				data.getOptimalSowingEnd(),
				data.getCurrentGrowthStage(),
				data.getMeasuredAt(),
				data.getCreatedAt(),
				data.getUpdatedAt()
		);
	}
}
