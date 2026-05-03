package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.entities.Recommendation;
import com.agromag.domain.enums.RecommendationType;
import com.agromag.domain.enums.RiskLevel;
import com.agromag.domain.enums.SyncStatus;
import com.agromag.dto.request.RecommendationDecisionRequest;
import com.agromag.dto.response.FertilizerRecommendationResponse;
import com.agromag.dto.response.IrrigationRecommendationResponse;
import com.agromag.dto.response.RecommendationResponse;
import com.agromag.exception.ResourceNotFoundException;
import com.agromag.repository.CropParameterRepository;
import com.agromag.repository.RecommendationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendationService {

	private final RecommendationRepository recommendationRepository;
	private final CropParameterRepository cropParameterRepository;
	private final CropService cropService;
	private final ClimateService climateService;
	private final AiRecommendationService aiRecommendationService;

	public RecommendationService(RecommendationRepository recommendationRepository,
								 CropParameterRepository cropParameterRepository,
								 CropService cropService,
								 ClimateService climateService,
								 AiRecommendationService aiRecommendationService) {
		this.recommendationRepository = recommendationRepository;
		this.cropParameterRepository = cropParameterRepository;
		this.cropService = cropService;
		this.climateService = climateService;
		this.aiRecommendationService = aiRecommendationService;
	}

	/**
	 * Lista todas las recomendaciones de un cultivo.
	 */
	public List<RecommendationResponse> getRecommendationsByCrop(UUID cropId, UUID profileId) {
		cropService.findCropAndValidateOwnership(cropId, profileId);

		return recommendationRepository.findByCrop_Id(cropId).stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * Marca si el usuario siguió o no una recomendación.
	 */
	@Transactional
	public void markDecision(UUID profileId, RecommendationDecisionRequest request) {
		Recommendation rec = recommendationRepository.findById(request.recommendationId())
				.orElseThrow(() -> new ResourceNotFoundException("Recomendación", request.recommendationId()));

		// Validar que el cultivo de la recomendación pertenece al usuario
		cropService.findCropAndValidateOwnership(rec.getCrop().getId(), profileId);

		rec.setFollowed(request.followed());
		recommendationRepository.save(rec);
	}

	/**
	 * Genera una recomendación de riego basada en la temperatura actual
	 * del municipio del cultivo vs. la temperatura óptima máxima.
	 */
	@Transactional
	public IrrigationRecommendationResponse generateIrrigation(UUID cropId, UUID profileId) {
		Crop crop = cropService.findCropAndValidateOwnership(cropId, profileId);

		CropParameter params = cropParameterRepository.findByCropType(crop.getCropType())
				.orElseThrow(() -> new ResourceNotFoundException("Parámetros del cultivo", crop.getCropType()));

		ClimateData climate = climateService.getCurrentClimate(crop.getMunicipality());

		// Lógica de riego basada en temperatura y humedad
		RiskLevel level;
		String message;
		BigDecimal temp = climate.temperature();
		BigDecimal hum = climate.humidity();
		BigDecimal maxTemp = params.getOptimalTempMax();
		BigDecimal minHum = params.getHumidityMin();

		boolean tempExceeded = temp.compareTo(maxTemp) > 0;
		boolean humLow = hum.compareTo(minHum) < 0;
		boolean tempNearMax = temp.compareTo(maxTemp.subtract(BigDecimal.valueOf(3))) > 0;

		if (tempExceeded || humLow) {
			level = RiskLevel.HIGH;
			message = String.format("¡Alerta! Temperatura: %.1f°C (máx óptima: %.1f°C), Humedad: %.1f%% (mín óptima: %.1f%%). " +
							"Se recomienda riego inmediato. Referencia de riego para %s: %s.",
					temp, maxTemp, hum, minHum, crop.getCropType(), params.getIrrigationNeeds());
		} else if (tempNearMax) {
			level = RiskLevel.MEDIUM;
			message = String.format("La temperatura actual (%.1f°C) se acerca a la máxima óptima (%.1f°C). " +
							"Considere programar riego en las próximas horas. Referencia: %s.",
					temp, maxTemp, params.getIrrigationNeeds());
		} else {
			level = RiskLevel.LOW;
			message = String.format("Condiciones dentro del rango óptimo (Temp: %.1f°C, Hum: %.1f%%). " +
							"No se requiere riego adicional por ahora.",
					temp, hum);
		}

		// Persistir la recomendación
		Recommendation rec = new Recommendation();
		rec.setId(UUID.randomUUID());
		rec.setCrop(crop);
		rec.setType(RecommendationType.IRRIGATION);
		rec.setLevel(level);
		rec.setMessage(message);
		rec.setTemperature(climate.temperature());
		rec.setHumidity(climate.humidity());
		rec.setGeneratedAt(LocalDateTime.now());
		rec.setSyncStatus(SyncStatus.SYNCED);
		recommendationRepository.save(rec);

		return new IrrigationRecommendationResponse(
				rec.getId(),
				crop.getId(),
				level,
				message,
				climate.temperature(),
				rec.getGeneratedAt()
		);
	}

	/**
	 * Genera una recomendación de fertilización delegando a la IA (Spring AI).
	 * Persiste el resultado como Recommendation en la base de datos.
	 */
	@Transactional
	public FertilizerRecommendationResponse generateFertilizer(UUID cropId, UUID profileId) {
		Crop crop = cropService.findCropAndValidateOwnership(cropId, profileId);

		CropParameter params = cropParameterRepository.findByCropType(crop.getCropType())
				.orElseThrow(() -> new ResourceNotFoundException("Parámetros del cultivo", crop.getCropType()));

		ClimateData climate = climateService.getCurrentClimate(crop.getMunicipality());

		// Delegar a IA
		FertilizerRecommendationResponse aiResult =
				aiRecommendationService.generateFertilizerRecommendation(crop, params, climate);

		// Persistir la recomendación
		Recommendation rec = new Recommendation();
		rec.setId(aiResult.id());
		rec.setCrop(crop);
		rec.setType(RecommendationType.FERTILIZER);
		rec.setLevel(aiResult.level());
		rec.setMessage(aiResult.message());
		rec.setTemperature(climate.temperature());
		rec.setHumidity(climate.humidity());
		rec.setGeneratedAt(aiResult.generatedAt());
		rec.setSyncStatus(SyncStatus.SYNCED);
		recommendationRepository.save(rec);

		return aiResult;
	}

	private RecommendationResponse toResponse(Recommendation rec) {
		return new RecommendationResponse(
				rec.getId(),
				rec.getType(),
				rec.getLevel(),
				rec.getMessage(),
				rec.getFollowed(),
				rec.getGeneratedAt(),
				rec.getTemperature(),
				rec.getHumidity()
		);
	}
}
