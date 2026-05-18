package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.Profile;
import com.agromag.domain.enums.SyncStatus;
import com.agromag.dto.request.CropRequest;
import com.agromag.dto.response.CropResponse;
import com.agromag.exception.ResourceNotFoundException;
import com.agromag.exception.UnauthorizedCropAccessException;
import com.agromag.repository.AlertRepository;
import com.agromag.repository.CropEventRepository;
import com.agromag.repository.CropRepository;
import com.agromag.repository.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CropService {

	private static final Logger log = LoggerFactory.getLogger(CropService.class);

	private final CropRepository cropRepository;
	private final AlertRepository alertRepository;
	private final CropEventRepository cropEventRepository;
	private final RecommendationRepository recommendationRepository;
	private final ProfileService profileService;

	public CropService(CropRepository cropRepository,
					   AlertRepository alertRepository,
					   CropEventRepository cropEventRepository,
					   RecommendationRepository recommendationRepository,
					   ProfileService profileService) {
		this.cropRepository = cropRepository;
		this.alertRepository = alertRepository;
		this.cropEventRepository = cropEventRepository;
		this.recommendationRepository = recommendationRepository;
		this.profileService = profileService;
	}

	@Transactional
	public CropResponse createCrop(UUID profileId, CropRequest request) {
		Profile profile = profileService.getProfileById(profileId);

		Crop crop = new Crop();
		crop.setId(request.id());
		crop.setProfile(profile);
		crop.setCropType(request.cropType());
		crop.setAreaHectares(request.areaHectares());
		crop.setMunicipality(request.municipality());
		crop.setSownDate(request.sownDate());
		crop.setSyncStatus(SyncStatus.SYNCED);

		log.info("create_crop cropId={} profileId={} type={}", request.id(), profileId, request.cropType());
		return CropResponse.from(cropRepository.save(crop));
	}

	@Transactional(readOnly = true)
	public List<CropResponse> getCropsByProfile(UUID profileId) {
		return cropRepository.findByProfile_Id(profileId).stream()
				.map(CropResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public CropResponse getCropById(UUID cropId, UUID profileId) {
		return CropResponse.from(findCropAndValidateOwnership(cropId, profileId));
	}

	@Transactional
	public CropResponse updateCrop(UUID cropId, UUID profileId, CropRequest request) {
		Crop crop = findCropAndValidateOwnership(cropId, profileId);
		crop.setCropType(request.cropType());
		crop.setAreaHectares(request.areaHectares());
		crop.setMunicipality(request.municipality());
		crop.setSownDate(request.sownDate());
		log.info("update_crop cropId={} profileId={}", cropId, profileId);
		return CropResponse.from(cropRepository.save(crop));
	}

	@Transactional
	public void deleteCrop(UUID cropId, UUID profileId) {
		findCropAndValidateOwnership(cropId, profileId);
		int deletedAlerts = alertRepository.deleteByCrop_Id(cropId);
		int deletedRecommendations = recommendationRepository.deleteByCrop_Id(cropId);
		int deletedEvents = cropEventRepository.deleteByCrop_Id(cropId);
		int deletedCrops = cropRepository.deleteByIdAndProfileId(cropId, profileId);
		log.info("delete_crop cropId={} profileId={} deletedCrops={} deletedAlerts={} deletedRecommendations={} deletedEvents={}",
				cropId, profileId, deletedCrops, deletedAlerts, deletedRecommendations, deletedEvents);
		if (deletedCrops == 0) {
			throw new ResourceNotFoundException("Cultivo", cropId);
		}
	}

	public Crop findCropAndValidateOwnership(UUID cropId, UUID profileId) {
		Crop crop = cropRepository.findById(cropId)
				.orElseThrow(() -> new ResourceNotFoundException("Cultivo", cropId));

		if (!crop.getProfile().getId().equals(profileId)) {
			throw new UnauthorizedCropAccessException(cropId, profileId);
		}
		return crop;
	}
}
