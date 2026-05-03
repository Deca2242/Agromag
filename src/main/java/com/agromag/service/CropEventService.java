package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropEvent;
import com.agromag.domain.enums.SyncStatus;
import com.agromag.dto.request.CropEventRequest;
import com.agromag.dto.response.CropEventResponse;
import com.agromag.exception.ResourceNotFoundException;
import com.agromag.repository.CropEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CropEventService {

	private final CropEventRepository cropEventRepository;
	private final CropService cropService;

	public CropEventService(CropEventRepository cropEventRepository, CropService cropService) {
		this.cropEventRepository = cropEventRepository;
		this.cropService = cropService;
	}

	/**
	 * Crea un evento sobre un cultivo, validando que el cultivo
	 * pertenece al usuario autenticado.
	 */
	@Transactional
	public CropEventResponse createEvent(UUID profileId, CropEventRequest request) {
		Crop crop = cropService.findCropAndValidateOwnership(request.cropId(), profileId);

		CropEvent event = new CropEvent();
		event.setId(request.id());
		event.setCrop(crop);
		event.setEventType(request.eventType());
		event.setQuantity(request.quantity());
		event.setUnit(request.unit());
		event.setNotes(request.notes());
		event.setOccurredAt(request.occurredAt());
		event.setSyncStatus(SyncStatus.SYNCED);

		return toResponse(cropEventRepository.save(event));
	}

	/**
	 * Lista los eventos de un cultivo en orden descendente por fecha,
	 * verificando que el cultivo pertenece al usuario.
	 */
	public List<CropEventResponse> getEventsByCrop(UUID cropId, UUID profileId) {
		cropService.findCropAndValidateOwnership(cropId, profileId);

		return cropEventRepository.findByCrop_IdOrderByOccurredAtDesc(cropId).stream()
				.map(this::toResponse)
				.toList();
	}

	/**
	 * Obtiene un evento específico verificando propiedad del cultivo asociado.
	 */
	public CropEventResponse getEventById(UUID eventId, UUID profileId) {
		CropEvent event = cropEventRepository.findById(eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Evento", eventId));

		// Validar que el cultivo del evento pertenece al usuario
		cropService.findCropAndValidateOwnership(event.getCrop().getId(), profileId);

		return toResponse(event);
	}

	private CropEventResponse toResponse(CropEvent event) {
		return new CropEventResponse(
				event.getId(),
				event.getEventType(),
				event.getQuantity(),
				event.getUnit(),
				event.getNotes(),
				event.getOccurredAt()
		);
	}

	/**
	 * Actualiza un evento existente, verificando la propiedad del cultivo.
	 */
	@Transactional
	public CropEventResponse updateEvent(UUID eventId, UUID profileId, CropEventRequest request) {
		CropEvent event = cropEventRepository.findById(eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Evento", eventId));

		// Validar propiedad del cultivo (podría ser el original o al que se quiere mover)
		cropService.findCropAndValidateOwnership(event.getCrop().getId(), profileId);
		if (!event.getCrop().getId().equals(request.cropId())) {
			Crop newCrop = cropService.findCropAndValidateOwnership(request.cropId(), profileId);
			event.setCrop(newCrop);
		}

		event.setEventType(request.eventType());
		event.setQuantity(request.quantity());
		event.setUnit(request.unit());
		event.setNotes(request.notes());
		event.setOccurredAt(request.occurredAt());
		event.setSyncStatus(SyncStatus.SYNCED);

		return toResponse(cropEventRepository.save(event));
	}

	/**
	 * Elimina un evento tras verificar la propiedad del cultivo asociado.
	 */
	@Transactional
	public void deleteEvent(UUID eventId, UUID profileId) {
		CropEvent event = cropEventRepository.findById(eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Evento", eventId));

		cropService.findCropAndValidateOwnership(event.getCrop().getId(), profileId);

		cropEventRepository.delete(event);
	}
}
