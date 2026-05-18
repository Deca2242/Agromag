package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropEvent;
import com.agromag.domain.enums.SyncStatus;
import com.agromag.dto.request.CropEventRequest;
import com.agromag.dto.response.CropEventResponse;
import com.agromag.exception.ResourceNotFoundException;
import com.agromag.repository.CropEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CropEventService {

	private static final Logger log = LoggerFactory.getLogger(CropEventService.class);

	private final CropEventRepository cropEventRepository;
	private final CropService cropService;

	public CropEventService(CropEventRepository cropEventRepository, CropService cropService) {
		this.cropEventRepository = cropEventRepository;
		this.cropService = cropService;
	}

	@Transactional
	public CropEventResponse createEvent(UUID profileId, UUID pathCropId, CropEventRequest request) {
		if (!pathCropId.equals(request.cropId())) {
			throw new IllegalArgumentException(
					"El cropId del path (%s) no coincide con el del body (%s)"
							.formatted(pathCropId, request.cropId()));
		}

		Crop crop = cropService.findCropAndValidateOwnership(request.cropId(), profileId);
		CropEvent event = buildCropEvent(crop, request);

		log.info("create_event eventId={} cropId={} type={}", request.id(), request.cropId(), request.eventType());
		return CropEventResponse.from(cropEventRepository.save(event));
	}

	@Transactional
	public CropEventResponse createEventFromSync(UUID profileId, CropEventRequest request) {
		Crop crop = cropService.findCropAndValidateOwnership(request.cropId(), profileId);
		CropEvent event = buildCropEvent(crop, request);

		log.info("sync_create_event eventId={} cropId={}", request.id(), request.cropId());
		return CropEventResponse.from(cropEventRepository.save(event));
	}

	@Transactional(readOnly = true)
	public List<CropEventResponse> getEventsByCrop(UUID cropId, UUID profileId) {
		cropService.findCropAndValidateOwnership(cropId, profileId);

		return cropEventRepository.findByCrop_IdOrderByOccurredAtDesc(cropId).stream()
				.map(CropEventResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public CropEventResponse getEventById(UUID eventId, UUID profileId) {
		CropEvent event = findEventOrThrow(eventId);
		cropService.findCropAndValidateOwnership(event.getCrop().getId(), profileId);
		return CropEventResponse.from(event);
	}

	@Transactional
	public CropEventResponse updateEvent(UUID eventId, UUID profileId, CropEventRequest request) {
		CropEvent event = findEventOrThrow(eventId);

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

		log.info("update_event eventId={} cropId={}", eventId, request.cropId());
		return CropEventResponse.from(cropEventRepository.save(event));
	}

	@Transactional
	public void deleteEvent(UUID eventId, UUID profileId) {
		int deleted = cropEventRepository.deleteByIdAndProfileId(eventId, profileId);
		if (deleted == 0) {
			throw new ResourceNotFoundException("Evento", eventId);
		}
		log.info("delete_event eventId={}", eventId);
	}

	private CropEvent buildCropEvent(Crop crop, CropEventRequest request) {
		CropEvent event = new CropEvent();
		event.setId(request.id());
		event.setCrop(crop);
		event.setEventType(request.eventType());
		event.setQuantity(request.quantity());
		event.setUnit(request.unit());
		event.setNotes(request.notes());
		event.setOccurredAt(request.occurredAt());
		event.setSyncStatus(SyncStatus.SYNCED);
		return event;
	}

	private CropEvent findEventOrThrow(UUID eventId) {
		return cropEventRepository.findById(eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Evento", eventId));
	}
}
