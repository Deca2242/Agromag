package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.Profile;
import com.agromag.domain.enums.SyncStatus;
import com.agromag.dto.request.CropRequest;
import com.agromag.dto.response.CropResponse;
import com.agromag.exception.ResourceNotFoundException;
import com.agromag.exception.UnauthorizedCropAccessException;
import com.agromag.repository.CropRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CropService {

	private final CropRepository cropRepository;
	private final ProfileService profileService;

	public CropService(CropRepository cropRepository, ProfileService profileService) {
		this.cropRepository = cropRepository;
		this.profileService = profileService;
	}

	/**
	 * Crea un cultivo asociado al perfil del usuario.
	 * El ID viene del cliente (soporte offline).
	 */
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

		return toResponse(cropRepository.save(crop));
	}

	/**
	 * Lista todos los cultivos del usuario autenticado.
	 */
	public List<CropResponse> getCropsByProfile(UUID profileId) {
		return cropRepository.findByProfile_Id(profileId).stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * Obtiene un cultivo verificando que pertenece al usuario.
	 */
	public CropResponse getCropById(UUID cropId, UUID profileId) {
		return toResponse(findCropAndValidateOwnership(cropId, profileId));
	}

	/**
	 * Actualiza un cultivo existente verificando propiedad.
	 */
	@Transactional
	public CropResponse updateCrop(UUID cropId, UUID profileId, CropRequest request) {
		Crop crop = findCropAndValidateOwnership(cropId, profileId);
		crop.setCropType(request.cropType());
		crop.setAreaHectares(request.areaHectares());
		crop.setMunicipality(request.municipality());
		crop.setSownDate(request.sownDate());
		return toResponse(cropRepository.save(crop));
	}

	/**
	 * Elimina un cultivo verificando propiedad.
	 */
	@Transactional
	public void deleteCrop(UUID cropId, UUID profileId) {
		Crop crop = findCropAndValidateOwnership(cropId, profileId);
		cropRepository.delete(crop);
	}

	/**
	 * Método interno reutilizable: busca un cultivo y valida que
	 * pertenece al usuario autenticado. Usado por CropEventService
	 * y RecommendationService.
	 */
	public Crop findCropAndValidateOwnership(UUID cropId, UUID profileId) {
		Crop crop = cropRepository.findById(cropId)
				.orElseThrow(() -> new ResourceNotFoundException("Cultivo", cropId));

		if (!crop.getProfile().getId().equals(profileId)) {
			throw new UnauthorizedCropAccessException(cropId, profileId);
		}
		return crop;
	}

	private CropResponse toResponse(Crop crop) {
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
