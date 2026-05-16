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
import java.util.Map;
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
	private final AlertService alertService;

	public SyncService(CropRepository cropRepository,
					   CropEventRepository cropEventRepository,
					   CropService cropService,
					   CropEventService cropEventService,
					   RecommendationService recommendationService,
					   AlertService alertService) {
		this.cropRepository = cropRepository;
		this.cropEventRepository = cropEventRepository;
		this.cropService = cropService;
		this.cropEventService = cropEventService;
		this.recommendationService = recommendationService;
		this.alertService = alertService;
	}

	// Procesamiento por fases con reporte parcial de errores.
	// Todas las fases comparten una unica transaccion; los errores individuales se capturan
	// para evitar rollback total por un fallo aislado.
	@Transactional
	public SyncBatchResponse processBatch(UUID profileId, SyncBatchRequest request) {
		log.info("sync_batch_start profileId={} crops={} events={} decisions={}",
				profileId, request.crops().size(), request.events().size(), request.decisions().size());

		// Limpieza de alertas huérfanas (cultivos eliminados sin cascade)
		int orphanedAlerts = alertService.cleanupOrphanedAlerts();
		if (orphanedAlerts > 0) {
			log.info("sync_cleanup_orphaned_alerts profileId={} deleted={}", profileId, orphanedAlerts);
		}

		List<CropResponse> syncedCrops = new ArrayList<>();
		List<CropEventResponse> syncedEvents = new ArrayList<>();
		List<String> failedCropIds = new ArrayList<>();
		List<String> failedEventIds = new ArrayList<>();

		// 1. Sincronizar cultivos — batch check para evitar N+1
		if (!request.crops().isEmpty()) {
			Set<UUID> requestedCropIds = request.crops().stream()
					.map(CropRequest::id)
					.collect(Collectors.toSet());

			List<Crop> existingCrops = cropRepository.findAllById(requestedCropIds);
			Map<UUID, UUID> existingCropOwnerMap = existingCrops.stream()
					.collect(Collectors.toMap(Crop::getId, c -> c.getProfile().getId()));

			for (CropRequest cropReq : request.crops()) {
				try {
					UUID existingOwnerId = existingCropOwnerMap.get(cropReq.id());
					if (existingOwnerId != null) {
						// El cultivo ya existe — verificar propiedad
						if (!existingOwnerId.equals(profileId)) {
							log.warn("sync_crop_conflict cropId={} requesterId={} ownerId={}",
									cropReq.id(), profileId, existingOwnerId);
							failedCropIds.add(cropReq.id().toString());
							continue;
						}
						// Pertenece al mismo usuario — actualizar
						CropResponse updated = cropService.updateCrop(cropReq.id(), profileId, cropReq);
						syncedCrops.add(updated);
					} else {
						// No existe — crear
						CropResponse created = cropService.createCrop(profileId, cropReq);
						syncedCrops.add(created);
					}
				} catch (Exception e) {
					log.warn("sync_crop_failed cropId={} error={}", cropReq.id(), e.getMessage());
					failedCropIds.add(cropReq.id().toString());
				}
			}
		}

		// 2. Sincronizar eventos — batch check para evitar N+1
		if (!request.events().isEmpty()) {
			Set<UUID> requestedEventIds = request.events().stream()
					.map(CropEventRequest::id)
					.collect(Collectors.toSet());

			List<CropEvent> existingEvents = cropEventRepository.findAllById(requestedEventIds);
			Set<UUID> existingEventIds = existingEvents.stream()
					.map(CropEvent::getId)
					.collect(Collectors.toSet());

			for (CropEventRequest eventReq : request.events()) {
				try {
					if (!existingEventIds.contains(eventReq.id())) {
						CropEventResponse created = cropEventService.createEventFromSync(profileId, eventReq);
						syncedEvents.add(created);
					} else {
						// Evento ya existe — actualizar
						CropEventResponse updated = cropEventService.updateEvent(eventReq.id(), profileId, eventReq);
						syncedEvents.add(updated);
					}
				} catch (Exception e) {
					log.warn("sync_event_failed eventId={} error={}", eventReq.id(), e.getMessage());
					failedEventIds.add(eventReq.id().toString());
				}
			}
		}

		// 3. Procesar decisiones sobre recomendaciones
		List<String> failedDecisionIds = new ArrayList<>();
		for (RecommendationDecisionRequest decision : request.decisions()) {
			try {
				recommendationService.markDecision(profileId, decision);
			} catch (Exception e) {
				log.warn("sync_decision_failed recommendationId={} error={}",
						decision.recommendationId(), e.getMessage());
				failedDecisionIds.add(decision.recommendationId().toString());
			}
		}

		String message = buildMessage(syncedCrops.size(), syncedEvents.size(),
				request.decisions().size() - failedDecisionIds.size(),
				failedCropIds, failedEventIds, failedDecisionIds);

		log.info("sync_batch_done profileId={} synced_crops={} synced_events={} failed_crops={} failed_events={}",
				profileId, syncedCrops.size(), syncedEvents.size(),
				failedCropIds.size(), failedEventIds.size());

		return new SyncBatchResponse(
				failedCropIds.isEmpty() && failedEventIds.isEmpty() && failedDecisionIds.isEmpty() ? "OK" : "PARTIAL",
				message,
				syncedCrops,
				syncedEvents,
				failedCropIds,
				failedEventIds,
				failedDecisionIds,
				LocalDateTime.now()
		);
	}

	private String buildMessage(int cropsOk, int eventsOk, int decisionsOk,
			List<String> failedCrops, List<String> failedEvents, List<String> failedDecisions) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Sincronización completada: %d cultivos, %d eventos, %d decisiones procesadas",
				cropsOk, eventsOk, decisionsOk));
		if (!failedCrops.isEmpty() || !failedEvents.isEmpty() || !failedDecisions.isEmpty()) {
			sb.append(String.format(". Fallos: %d cultivos, %d eventos, %d decisiones",
					failedCrops.size(), failedEvents.size(), failedDecisions.size()));
		}
		return sb.toString();
	}
}
