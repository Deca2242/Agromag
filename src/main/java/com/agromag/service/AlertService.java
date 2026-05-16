package com.agromag.service;

import com.agromag.config.RecommendationProperties;
import com.agromag.domain.entities.Alert;
import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.enums.AlertSeverity;
import com.agromag.domain.enums.RecommendationType;
import com.agromag.domain.enums.RiskLevel;
import com.agromag.domain.model.ClimateData;
import com.agromag.dto.response.AlertResponse;
import com.agromag.repository.AlertRepository;
import com.agromag.repository.CropParameterRepository;
import com.agromag.repository.CropRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

// Gestiona la creacion, consulta y marcado de lectura de alertas
@Service
public class AlertService {

	private static final Logger log = LoggerFactory.getLogger(AlertService.class);

	private final AlertRepository alertRepository;
	private final CropRepository cropRepository;
	private final CropParameterRepository cropParameterRepository;
	private final ClimateService climateService;
	private final RecommendationProperties props;

	public AlertService(AlertRepository alertRepository,
						CropRepository cropRepository,
						CropParameterRepository cropParameterRepository,
						ClimateService climateService,
						RecommendationProperties props) {
		this.alertRepository = alertRepository;
		this.cropRepository = cropRepository;
		this.cropParameterRepository = cropParameterRepository;
		this.climateService = climateService;
		this.props = props;
	}

	// ── Consultas para el frontend ──────────────────────────────────────

	@Transactional(readOnly = true)
	public Page<AlertResponse> getAlerts(UUID profileId, RecommendationType type, Pageable pageable) {
		if (type != null) {
			return alertRepository.findByProfile_IdAndTypeOrderByCreatedAtDesc(profileId, type, pageable)
					.map(AlertResponse::from);
		}
		return alertRepository.findByProfile_IdOrderByCreatedAtDesc(profileId, pageable)
				.map(AlertResponse::from);
	}

	@Transactional(readOnly = true)
	public long countUnread(UUID profileId) {
		return alertRepository.countUnreadByProfileIdAndSeverities(
				profileId, List.of(AlertSeverity.MEDIUM, AlertSeverity.HIGH));
	}

	@Transactional(readOnly = true)
	public long countUnreadHigh(UUID profileId) {
		return alertRepository.countUnreadByProfileIdAndSeverity(profileId, AlertSeverity.HIGH);
	}

	@Transactional
	public void markAsRead(UUID profileId, UUID alertId) {
		int updated = alertRepository.markAsReadDirect(alertId, profileId, LocalDateTime.now());
		if (updated == 0) {
			throw new com.agromag.exception.ResourceNotFoundException("Alerta", alertId);
		}
		log.info("alert_mark_read alertId={}", alertId);
	}

	@Transactional
	public int markAllAsRead(UUID profileId) {
		int updated = alertRepository.markAllAsReadDirect(profileId, LocalDateTime.now());
		log.info("alert_mark_all_read profileId={} updated={}", profileId, updated);
		return updated;
	}

	@Transactional
	public void deleteAlert(UUID profileId, UUID alertId) {
		int deleted = alertRepository.deleteByIdAndProfileId(alertId, profileId);
		if (deleted == 0) {
			throw new com.agromag.exception.ResourceNotFoundException("Alerta", alertId);
		}
		log.info("alert_deleted alertId={} profileId={}", alertId, profileId);
	}

	@Transactional
	public int deleteAllRead(UUID profileId) {
		int deleted = alertRepository.deleteAllByProfile_IdAndReadAtIsNotNull(profileId);
		log.info("alert_delete_all_read profileId={} deleted={}", profileId, deleted);
		return deleted;
	}

	@Transactional
	public int cleanupOrphanedAlerts() {
		int deleted = alertRepository.deleteOrphanedByMissingCrop();
		if (deleted > 0) {
			log.info("alert_cleanup_orphaned deleted={}", deleted);
		}
		return deleted;
	}

	// ── Generacion automatica de alertas (llamado por AlertScheduler) ───

	@Transactional
	public int evaluateAndCreateAlertsForAllCrops() {
		List<Crop> allCrops = cropRepository.findAll();
		int created = 0;

		for (Crop crop : allCrops) {
			try {
				created += evaluateAndCreateAlertsForCrop(crop);
			} catch (Exception e) {
				log.warn("alert_eval_failed cropId={} error={}", crop.getId(), e.getMessage());
			}
		}

		log.info("alert_batch_done totalCreated={}", created);
		return created;
	}

	@Transactional
	public int evaluateAndCreateAlertsForCrop(Crop crop) {
		int created = 0;
		try {
			ClimateData climate = climateService.getCurrentClimate(crop.getMunicipality());
			CropParameter params = cropParameterRepository.findByCropType(crop.getCropType()).orElse(null);
			if (params == null) {
				log.debug("alert_skip_no_params cropId={} cropType={}", crop.getId(), crop.getCropType());
				return 0;
			}

			// 1. Riego
			created += evaluateIrrigation(crop, params, climate);
			// 2. Fitosanitario
			created += evaluatePhytosanitary(crop, climate);
			// 3. Fertilizacion
			created += evaluateFertilizer(crop, params);

		} catch (Exception e) {
			log.warn("alert_eval_crop_failed cropId={} error={}", crop.getId(), e.getMessage());
		}
		return created;
	}

	private int evaluateIrrigation(Crop crop, CropParameter params, ClimateData climate) {
		BigDecimal temp = climate.temperature();
		BigDecimal hum = climate.humidity();
		BigDecimal maxTemp = params.getOptimalTempMax();
		BigDecimal minHum = params.getHumidityMin();

		boolean tempExceeded = temp.compareTo(maxTemp) > 0;
		boolean humLow = hum.compareTo(minHum) < 0;
		boolean tempNearMax = temp.compareTo(maxTemp.subtract(props.irrigationNearMaxDelta())) > 0;

		if (tempExceeded || humLow) {
			String msg = String.format(
					"¡Alerta de riego! Temperatura: %.1f°C (máx óptima: %.1f°C), Humedad: %.1f%% (mín óptima: %.1f%%). " +
					"Se recomienda riego inmediato para su cultivo de %s. Referencia: %s.",
					temp, maxTemp, hum, minHum, crop.getCropType(), params.getIrrigationNeeds());
			createAlert(crop, RecommendationType.IRRIGATION, AlertSeverity.HIGH,
					"Riego urgente requerido", msg, Icons.WATER_DROP);
			return 1;
		} else if (tempNearMax) {
			String msg = String.format(
					"La temperatura actual (%.1f°C) se acerca a la máxima óptima (%.1f°C) para %s. " +
					"Considere programar riego en las próximas horas.",
					temp, maxTemp, crop.getCropType());
			createAlert(crop, RecommendationType.IRRIGATION, AlertSeverity.MEDIUM,
					"Riego preventivo recomendado", msg, Icons.WATER_DROP);
			return 1;
		}
		return 0;
	}

	private int evaluatePhytosanitary(Crop crop, ClimateData climate) {
		BigDecimal temp = climate.temperature();
		BigDecimal hum = climate.humidity();
		boolean highTemp = temp.compareTo(props.phytoTempThreshold()) > 0;
		boolean highHum = hum.compareTo(props.phytoHumidityThreshold()) > 0;
		String pests = crop.getCropType().getCommonPests();

		if (highTemp && highHum) {
			String msg = String.format(
					"¡Alerta fitosanitaria! Temperatura: %.1f°C y humedad: %.1f%% superan los umbrales " +
					"de riesgo (>28°C y >80%%). Revise su cultivo de %s hoy. Plagas probables: %s.",
					temp, hum, crop.getCropType(), pests);
			createAlert(crop, RecommendationType.PHYTOSANITARY, AlertSeverity.HIGH,
					"Riesgo fitosanitario alto", msg, Icons.BUG_REPORT);
			return 1;
		} else if (highTemp || highHum) {
			String condition = highTemp
					? String.format("temperatura alta (%.1f°C)", temp)
					: String.format("humedad alta (%.1f%%)", hum);
			String msg = String.format(
					"Condición de riesgo moderado: %s. Monitoree su cultivo de %s. Posibles plagas: %s.",
					condition, crop.getCropType(), pests);
			createAlert(crop, RecommendationType.PHYTOSANITARY, AlertSeverity.MEDIUM,
					"Monitoreo fitosanitario recomendado", msg, Icons.BUG_REPORT);
			return 1;
		}
		return 0;
	}

	private int evaluateFertilizer(Crop crop, CropParameter params) {
		long weeks = ChronoUnit.WEEKS.between(crop.getSownDate(), LocalDateTime.now().toLocalDate());

		// Fertilizer alerts change slowly — use 24h duplicate window
		LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);
		boolean recentExists = alertRepository.existsRecentUnread(
				crop.getProfile().getId(), RecommendationType.FERTILIZER, crop.getId(), oneDayAgo);
		if (recentExists) {
			log.debug("alert_skip_duplicate_fertilizer cropId={}", crop.getId());
			return 0;
		}

		if (weeks < props.fertilizerInitialStageWeeks()) {
			String msg = String.format(
					"Su cultivo de %s está en etapa inicial (%d semanas). Se sugiere aplicar nitrógeno (N) " +
					"para estimular el crecimiento radicular. Referencia: %s.",
					crop.getCropType(), weeks, params.getRecommendedFertilizer());
			createAlert(crop, RecommendationType.FERTILIZER, AlertSeverity.HIGH,
					"Fertilización inicial requerida", msg, Icons.ECO);
			return 1;
		} else if (weeks <= props.fertilizerVegetativeStageWeeks()) {
			String msg = String.format(
					"Su cultivo de %s está en desarrollo vegetativo (%d semanas). " +
					"Se sugiere fertilización NPK balanceada. Referencia: %s.",
					crop.getCropType(), weeks, params.getRecommendedFertilizer());
			createAlert(crop, RecommendationType.FERTILIZER, AlertSeverity.MEDIUM,
					"Fertilización de desarrollo", msg, Icons.ECO);
			return 1;
		} else {
			String msg = String.format(
					"Su cultivo de %s está en etapa de producción (%d semanas). " +
					"Se sugiere reforzar potasio (K) para calidad del fruto. Referencia: %s.",
					crop.getCropType(), weeks, params.getRecommendedFertilizer());
			createAlert(crop, RecommendationType.FERTILIZER, AlertSeverity.MEDIUM,
					"Fertilización de producción", msg, Icons.ECO);
			return 1;
		}
	}

	private void createAlert(Crop crop, RecommendationType type, AlertSeverity severity,
							 String title, String message, int iconCode) {
		// Evitar alertas duplicadas recientes (mismo tipo + mismo cultivo en ultima hora)
		LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
		boolean recentExists = alertRepository.existsRecentUnread(
				crop.getProfile().getId(), type, crop.getId(), oneHourAgo);
		if (recentExists) {
			log.debug("alert_skip_duplicate cropId={} type={}", crop.getId(), type);
			return;
		}

		String cropTag = crop.getCropType().label() + " — " + crop.getMunicipality().getDisplayName();

		Alert alert = new Alert();
		alert.setId(UUID.randomUUID());
		alert.setCrop(crop);
		alert.setProfile(crop.getProfile());
		alert.setType(type);
		alert.setSeverity(severity);
		alert.setTitle(title);
		alert.setMessage(message);
		alert.setCropTag(cropTag);
		alert.setIconCode(iconCode);
		alertRepository.save(alert);

		log.info("alert_created cropId={} type={} severity={}", crop.getId(), type, severity);
	}

	// Iconos Material — constantes para uso en alertas
	static class Icons {
		static final int WATER_DROP = 0xe5f72;
		static final int BUG_REPORT = 0xe868;
		static final int ECO = 0xe5f70;
	}
}
