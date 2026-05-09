package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropTechnicalData;
import com.agromag.dto.request.CropTechnicalDataRequest;
import com.agromag.dto.response.CropTechnicalDataResponse;
import com.agromag.exception.ResourceNotFoundException;
import com.agromag.repository.CropTechnicalDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing crop-specific technical data sheets.
 * Validates crop ownership before every operation via {@link CropService}.
 *
 * @since 1.1
 */
@Service
public class CropTechnicalDataService {

	private static final Logger log = LoggerFactory.getLogger(CropTechnicalDataService.class);

	private final CropTechnicalDataRepository technicalDataRepository;
	private final CropService cropService;

	public CropTechnicalDataService(CropTechnicalDataRepository technicalDataRepository,
									CropService cropService) {
		this.technicalDataRepository = technicalDataRepository;
		this.cropService = cropService;
	}

	/**
	 * Creates or updates the technical data sheet for the given crop (upsert).
	 *
	 * @param cropId the crop identifier
	 * @param profileId the authenticated user's profile identifier
	 * @param request the technical data payload
	 * @return the persisted technical data as a response DTO
	 */
	@Transactional
	public CropTechnicalDataResponse saveTechnicalData(UUID cropId, UUID profileId,
													   CropTechnicalDataRequest request) {
		Crop crop = cropService.findCropAndValidateOwnership(cropId, profileId);

		CropTechnicalData data = technicalDataRepository.findByCropId(cropId)
				.orElseGet(() -> {
					CropTechnicalData newData = new CropTechnicalData();
					newData.setCrop(crop);
					return newData;
				});

		applyRequest(data, request);
		technicalDataRepository.save(data);

		log.info("save_technical_data cropId={} profileId={} isNew={}",
				cropId, profileId, data.getId() == null);
		return CropTechnicalDataResponse.from(data);
	}

	/**
	 * Returns the technical data sheet for the given crop.
	 *
	 * @param cropId the crop identifier
	 * @param profileId the authenticated user's profile identifier
	 * @return the technical data response DTO
	 * @throws ResourceNotFoundException if no technical data exists for this crop
	 */
	@Transactional(readOnly = true)
	public CropTechnicalDataResponse getTechnicalData(UUID cropId, UUID profileId) {
		cropService.findCropAndValidateOwnership(cropId, profileId);

		CropTechnicalData data = technicalDataRepository.findByCropId(cropId)
				.orElseThrow(() -> new ResourceNotFoundException("Datos técnicos del cultivo", cropId));

		return CropTechnicalDataResponse.from(data);
	}

	/**
	 * Deletes the technical data sheet for the given crop.
	 *
	 * @param cropId the crop identifier
	 * @param profileId the authenticated user's profile identifier
	 * @throws ResourceNotFoundException if no technical data exists for this crop
	 */
	@Transactional
	public void deleteTechnicalData(UUID cropId, UUID profileId) {
		Crop crop = cropService.findCropAndValidateOwnership(cropId, profileId);

		CropTechnicalData data = technicalDataRepository.findByCropId(cropId)
				.orElseThrow(() -> new ResourceNotFoundException("Datos técnicos del cultivo", cropId));

		crop.setTechnicalData(null);
		technicalDataRepository.delete(data);
		log.info("delete_technical_data cropId={} profileId={}", cropId, profileId);
	}

	private void applyRequest(CropTechnicalData data, CropTechnicalDataRequest req) {
		// Suelo y nutrición
		data.setSoilPh(req.soilPh());
		data.setSoilTexture(req.soilTexture());
		data.setSoilStructure(req.soilStructure());
		data.setCationExchangeCapacity(req.cationExchangeCapacity());
		data.setNitrogenLevel(req.nitrogenLevel());
		data.setPhosphorusLevel(req.phosphorusLevel());
		data.setPotassiumLevel(req.potassiumLevel());
		data.setChlorophyllIndex(req.chlorophyllIndex());
		data.setNdviIndex(req.ndviIndex());
		data.setSoilDisinfected(req.soilDisinfected());
		data.setPathogenNotes(req.pathogenNotes());
		// Clima y riego
		data.setSoilMoisture(req.soilMoisture());
		data.setFieldTemperature(req.fieldTemperature());
		data.setPrecipitation(req.precipitation());
		data.setSolarRadiation(req.solarRadiation());
		data.setWindSpeed(req.windSpeed());
		data.setIrrigationTechnology(req.irrigationTechnology());
		// Siembra y desarrollo
		data.setPlantingDensity(req.plantingDensity());
		data.setSeedVariety(req.seedVariety());
		data.setSeedAdaptedToZone(req.seedAdaptedToZone());
		data.setOptimalSowingStart(req.optimalSowingStart());
		data.setOptimalSowingEnd(req.optimalSowingEnd());
		data.setCurrentGrowthStage(req.currentGrowthStage());
		// Timestamp
		data.setMeasuredAt(req.measuredAt());
	}
}
