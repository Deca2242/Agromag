package com.agromag.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Request payload for creating or updating a crop's technical data sheet.
 * All fields are optional to allow partial submissions.
 *
 * @param soilPh pH medido del suelo (0–14)
 * @param soilTexture textura del suelo
 * @param soilStructure estructura del suelo
 * @param cationExchangeCapacity CIC en meq/100g
 * @param nitrogenLevel nivel de N en ppm
 * @param phosphorusLevel nivel de P en ppm
 * @param potassiumLevel nivel de K en ppm
 * @param chlorophyllIndex índice de clorofila (SPAD)
 * @param ndviIndex índice NDVI (0.0–1.0)
 * @param soilDisinfected si se realizó desinfección del suelo
 * @param pathogenNotes notas sobre patógenos y malezas
 * @param soilMoisture humedad del suelo en porcentaje
 * @param fieldTemperature temperatura medida en campo en °C
 * @param precipitation precipitación registrada en mm
 * @param solarRadiation radiación solar en W/m²
 * @param windSpeed velocidad del viento en km/h
 * @param irrigationTechnology tecnología de riego usada
 * @param plantingDensity plantas por hectárea
 * @param seedVariety variedad de semilla seleccionada
 * @param seedAdaptedToZone si la semilla está adaptada a la zona
 * @param optimalSowingStart inicio de ventana óptima de siembra
 * @param optimalSowingEnd fin de ventana óptima de siembra
 * @param currentGrowthStage etapa actual de crecimiento
 * @param measuredAt fecha en la que se tomaron las mediciones
 */
public record CropTechnicalDataRequest(
		@DecimalMin("0.0") @DecimalMax("14.0") BigDecimal soilPh,
		@Size(max = 100) String soilTexture,
		@Size(max = 100) String soilStructure,
		@DecimalMin("0.0") BigDecimal cationExchangeCapacity,
		@DecimalMin("0.0") BigDecimal nitrogenLevel,
		@DecimalMin("0.0") BigDecimal phosphorusLevel,
		@DecimalMin("0.0") BigDecimal potassiumLevel,
		@DecimalMin("0.0") BigDecimal chlorophyllIndex,
		@DecimalMin("0.0") @DecimalMax("1.0") BigDecimal ndviIndex,
		Boolean soilDisinfected,
		@Size(max = 2000) String pathogenNotes,
		@DecimalMin("0.0") @DecimalMax("100.0") BigDecimal soilMoisture,
		BigDecimal fieldTemperature,
		@DecimalMin("0.0") BigDecimal precipitation,
		@DecimalMin("0.0") BigDecimal solarRadiation,
		@DecimalMin("0.0") BigDecimal windSpeed,
		@Size(max = 100) String irrigationTechnology,
		@DecimalMin("0.0") BigDecimal plantingDensity,
		@Size(max = 200) String seedVariety,
		Boolean seedAdaptedToZone,
		LocalDate optimalSowingStart,
		LocalDate optimalSowingEnd,
		@Size(max = 100) String currentGrowthStage,
		LocalDateTime measuredAt
) {}
