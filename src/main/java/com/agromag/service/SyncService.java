package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropEvent;
import com.agromag.domain.enums.SyncStatus;
import com.agromag.dto.request.CropEventRequest;
import com.agromag.dto.request.CropRequest;
import com.agromag.dto.request.RecommendationDecisionRequest;
import com.agromag.dto.request.SyncBatchRequest;
import com.agromag.dto.response.CropEventResponse;
import com.agromag.dto.response.CropResponse;
import com.agromag.dto.response.SyncBatchResponse;
import com.agromag.repository.CropEventRepository;
import com.agromag.repository.CropRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orquestador de sincronización offline → online.
 * Procesa lotes de cultivos, eventos y decisiones de forma idempotente.
 */
@Service
public class SyncService {

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

	/**
	 * Procesa un lote de sincronización del cliente móvil.
	 * Orden: 1) Cultivos  2) Eventos  3) Decisiones de recomendación.
	 * Es idempotente: si un UUID ya existe, se omite.
	 */
	@Transactional
	public SyncBatchResponse processBatch(UUID profileId, SyncBatchRequest request) {
		List<CropResponse> syncedCrops = new ArrayList<>();
		List<CropEventResponse> syncedEvents = new ArrayList<>();

		// 1. Sincronizar cultivos
		for (CropRequest cropReq : request.crops()) {
			if (!cropRepository.existsById(cropReq.id())) {
				CropResponse created = cropService.createCrop(profileId, cropReq);
				syncedCrops.add(created);
			}
		}

		// 2. Sincronizar eventos
		for (CropEventRequest eventReq : request.events()) {
			if (!cropEventRepository.existsById(eventReq.id())) {
				CropEventResponse created = cropEventService.createEvent(profileId, eventReq);
				syncedEvents.add(created);
			}
		}

		// 3. Procesar decisiones sobre recomendaciones
		for (RecommendationDecisionRequest decision : request.decisions()) {
			recommendationService.markDecision(profileId, decision);
		}

		String message = String.format("Sincronización completada: %d cultivos, %d eventos, %d decisiones procesadas",
				syncedCrops.size(), syncedEvents.size(), request.decisions().size());

		return new SyncBatchResponse(
				"OK",
				message,
				syncedCrops,
				syncedEvents,
				LocalDateTime.now()
		);
	}
}
