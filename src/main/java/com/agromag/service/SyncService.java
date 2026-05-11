package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropEvent;
import com.agromag.dto.request.CropEventRequest;
import com.agromag.dto.request.CropRequest;
import com.agromag.dto.request.RecommendationDecisionRequest;
import com.agromag.dto.request.SyncBatchRequest;
import com.agromag.dto.response.CropEventResponse;
import com.agromag.dto.response.CropResponse;
import com.agromag.dto.response.SyncBatchResponse;
import com.agromag.repository.CropEventRepository;
import com.agromag.repository.CropRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Procesamiento de sincronización batch offline → servidor
@Service
public class SyncService {

	private static final Logger log = LoggerFactory.getLogger(SyncService.class);

	private final CropRepository cropRepository;
	private final CropEventRepository cropEventRepository;
	private final CropService cropService;
	private final CropEventService cropEventService;
	private final RecommendationService recommendationService;

	public SyncService(CropRepository cropRepository,
					   CropEventRepository cropEventRepository,
					   CropService cropService,
					   CropEventService cropEventService,
					   RecommendationService recommendationService) {
		this.cropRepository = cropRepository;
		this.cropEventRepository = cropEventRepository;
		this.cropService = cropService;
		this.cropEventService = cropEventService;
		this.recommendationService = recommendationService;
	}

	// Todo-o-nada: si un item falla, se revierte todo el batch (el cliente reintenta completo)
	@Transactional
	public SyncBatchResponse processBatch(UUID profileId, SyncBatchRequest request) {
		log.info("sync_batch_start profileId={} crops={} events={} decisions={}",
				profileId, request.crops().size(), request.events().size(), request.decisions().size());

		List<CropResponse> syncedCrops = new ArrayList<>();
		List<CropEventResponse> syncedEvents = new ArrayList<>();

		// 1. Sincronizar cultivos — batch check para evitar N+1
		if (!request.crops().isEmpty()) {
			Set<UUID> requestedCropIds = request.crops().stream()
					.map(CropRequest::id)
					.collect(Collectors.toSet());

			Set<UUID> existingCropIds = cropRepository.findAllById(requestedCropIds).stream()
					.map(Crop::getId)
					.collect(Collectors.toSet());

			for (CropRequest cropReq : request.crops()) {
				if (!existingCropIds.contains(cropReq.id())) {
					CropResponse created = cropService.createCrop(profileId, cropReq);
					syncedCrops.add(created);
				}
			}
		}

		// 2. Sincronizar eventos — batch check para evitar N+1
		if (!request.events().isEmpty()) {
			Set<UUID> requestedEventIds = request.events().stream()
					.map(CropEventRequest::id)
					.collect(Collectors.toSet());

			Set<UUID> existingEventIds = cropEventRepository.findAllById(requestedEventIds).stream()
					.map(CropEvent::getId)
					.collect(Collectors.toSet());

			for (CropEventRequest eventReq : request.events()) {
				if (!existingEventIds.contains(eventReq.id())) {
					CropEventResponse created = cropEventService.createEventFromSync(profileId, eventReq);
					syncedEvents.add(created);
				}
			}
		}

		// 3. Procesar decisiones sobre recomendaciones
		for (RecommendationDecisionRequest decision : request.decisions()) {
			recommendationService.markDecision(profileId, decision);
		}

		String message = String.format("Sincronización completada: %d cultivos, %d eventos, %d decisiones procesadas",
				syncedCrops.size(), syncedEvents.size(), request.decisions().size());

		log.info("sync_batch_done profileId={} synced_crops={} synced_events={}",
				profileId, syncedCrops.size(), syncedEvents.size());

		return new SyncBatchResponse(
				"OK",
				message,
				syncedCrops,
				syncedEvents,
				LocalDateTime.now()
		);
	}
}
