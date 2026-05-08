package com.agromag.service;

import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.entities.Recommendation;
import com.agromag.domain.enums.RecommendationSource;
import com.agromag.domain.enums.RecommendationType;
import com.agromag.domain.enums.RiskLevel;
import com.agromag.domain.enums.SyncStatus;
import com.agromag.dto.request.RecommendationDecisionRequest;
import com.agromag.dto.response.FertilizerRecommendationResponse;
import com.agromag.dto.response.IrrigationRecommendationResponse;
import com.agromag.dto.response.PhytosanitaryRecommendationResponse;
import com.agromag.dto.response.RecommendationResponse;
import com.agromag.exception.AiServiceException;
import com.agromag.exception.ResourceNotFoundException;
import com.agromag.repository.CropParameterRepository;
import com.agromag.repository.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates recommendation generation (irrigation, fertilizer, phytosanitary) using an
 * AI-first strategy with a deterministic rule-based fallback.
 */
@Service
public class RecommendationService {

	private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

	private static final BigDecimal PHYTO_TEMP_THRESHOLD = new BigDecimal("28");
	private static final BigDecimal PHYTO_HUMIDITY_THRESHOLD = new BigDecimal("80");
	private static final BigDecimal IRRIGATION_NEAR_MAX_DELTA = BigDecimal.valueOf(3);

	private static final int FERTILIZER_INITIAL_STAGE_WEEKS = 4;
	private static final int FERTILIZER_VEGETATIVE_STAGE_WEEKS = 12;

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

	// Query

	@Transactional(readOnly = true)
	public List<RecommendationResponse> getRecommendationsByCrop(UUID cropId, UUID profileId) {
		cropService.findCropAndValidateOwnership(cropId, profileId);
		return recommendationRepository.findByCrop_Id(cropId).stream()
				.map(RecommendationResponse::from)
				.toList();
	}

	/** Records whether the producer followed a recommendation (CU-09). */
	@Transactional
	public void markDecision(UUID profileId, RecommendationDecisionRequest request) {
		Recommendation rec = recommendationRepository.findById(request.recommendationId())
				.orElseThrow(() -> new ResourceNotFoundException("Recomendación", request.recommendationId()));
		cropService.findCropAndValidateOwnership(rec.getCrop().getId(), profileId);
		rec.setFollowed(request.followed());
		recommendationRepository.save(rec);
		log.info("mark_decision recommendationId={} followed={}", request.recommendationId(), request.followed());
	}

	// Irrigation (RF-04) — AI first, rule fallback

	@Transactional
	public IrrigationRecommendationResponse generateIrrigation(UUID cropId, UUID profileId) {
		Crop crop = cropService.findCropAndValidateOwnership(cropId, profileId);
		CropParameter params = requireParams(crop);
		ClimateData climate = climateService.getCurrentClimate(crop.getMunicipality());

		RuleResult ruleResult = computeIrrigationByRules(climate, params, crop);

		IrrigationRecommendationResponse response;
		try {
			response = aiRecommendationService.generateIrrigationRecommendation(crop, params, climate);
			if (ruleResult.level() == RiskLevel.HIGH && response.level() != RiskLevel.HIGH) {
				log.warn("ai_irrigation_level_override cropId={} aiLevel={} overriddenTo=HIGH",
						cropId, response.level());
				response = new IrrigationRecommendationResponse(
						response.id(), response.cropId(), RiskLevel.HIGH,
						response.message(), response.temperature(), response.generatedAt(),
						RecommendationSource.AI);
			}
		} catch (AiServiceException e) {
			log.warn("ai_irrigation_fallback cropId={} reason={}", cropId, e.getMessage());
			response = new IrrigationRecommendationResponse(
					UUID.randomUUID(), cropId, ruleResult.level(),
					ruleResult.message(), climate.temperature(), LocalDateTime.now(),
					RecommendationSource.RULE);
		}

		persist(response.id(), crop, RecommendationType.IRRIGATION, response.level(), response.message(),
				climate.temperature(), climate.humidity(), response.source());
		log.info("generate_irrigation cropId={} level={} source={}", cropId, response.level(), response.source());
		return response;
	}

	// Fertilizer (RF-05, RF-06) — AI first, rule fallback

	@Transactional
	public FertilizerRecommendationResponse generateFertilizer(UUID cropId, UUID profileId) {
		Crop crop = cropService.findCropAndValidateOwnership(cropId, profileId);
		CropParameter params = requireParams(crop);
		ClimateData climate = climateService.getCurrentClimate(crop.getMunicipality());

		FertilizerRecommendationResponse response;
		try {
			response = aiRecommendationService.generateFertilizerRecommendation(crop, params, climate);
		} catch (AiServiceException e) {
			log.warn("ai_fertilizer_fallback cropId={} reason={}", cropId, e.getMessage());
			response = computeFertilizerByRules(crop, params, climate);
		}

		persist(response.id(), crop, RecommendationType.FERTILIZER, response.level(), response.message(),
				climate.temperature(), climate.humidity(), response.source());
		log.info("generate_fertilizer cropId={} level={} source={}", cropId, response.level(), response.source());
		return response;
	}

	// Phytosanitary (RF-07, RF-08) — AI first, rule fallback

	@Transactional
	public PhytosanitaryRecommendationResponse generatePhytosanitary(UUID cropId, UUID profileId) {
		Crop crop = cropService.findCropAndValidateOwnership(cropId, profileId);
		CropParameter params = requireParams(crop);
		ClimateData climate = climateService.getCurrentClimate(crop.getMunicipality());

		RuleResult ruleResult = computePhytosanitaryByRules(climate, crop);

		PhytosanitaryRecommendationResponse response;
		try {
			response = aiRecommendationService.generatePhytosanitaryRecommendation(
					crop, params, climate, ruleResult.level());
		} catch (AiServiceException e) {
			log.warn("ai_phytosanitary_fallback cropId={} reason={}", cropId, e.getMessage());
			response = new PhytosanitaryRecommendationResponse(
					UUID.randomUUID(), cropId, ruleResult.level(), ruleResult.message(),
					ruleResult.suspectedPests(), climate.temperature(), climate.humidity(),
					LocalDateTime.now(), RecommendationSource.RULE);
		}

		persist(response.id(), crop, RecommendationType.PHYTOSANITARY, response.level(), response.message(),
				climate.temperature(), climate.humidity(), response.source());
		log.info("generate_phytosanitary cropId={} level={} source={}", cropId, response.level(), response.source());
		return response;
	}

	// Deterministic rule engines

	/** Evaluates irrigation need using crop-parameter thresholds (FAO-simplified). */
	RuleResult computeIrrigationByRules(ClimateData climate, CropParameter params, Crop crop) {
		BigDecimal temp = climate.temperature();
		BigDecimal hum = climate.humidity();
		BigDecimal maxTemp = params.getOptimalTempMax();
		BigDecimal minHum = params.getHumidityMin();

		boolean tempExceeded = temp.compareTo(maxTemp) > 0;
		boolean humLow = hum.compareTo(minHum) < 0;
		boolean tempNearMax = temp.compareTo(maxTemp.subtract(IRRIGATION_NEAR_MAX_DELTA)) > 0;

		if (tempExceeded || humLow) {
			String msg = String.format(
					"¡Alerta! Temperatura: %.1f°C (máx óptima: %.1f°C), Humedad: %.1f%% (mín óptima: %.1f%%). " +
					"Se recomienda riego inmediato. Referencia de riego para %s: %s.",
					temp, maxTemp, hum, minHum, crop.getCropType(), params.getIrrigationNeeds());
			return new RuleResult(RiskLevel.HIGH, msg, null);
		} else if (tempNearMax) {
			String msg = String.format(
					"La temperatura actual (%.1f°C) se acerca a la máxima óptima (%.1f°C). " +
					"Considere programar riego en las próximas horas. Referencia: %s.",
					temp, maxTemp, params.getIrrigationNeeds());
			return new RuleResult(RiskLevel.MEDIUM, msg, null);
		} else {
			String msg = String.format(
					"Condiciones dentro del rango óptimo (Temp: %.1f°C, Hum: %.1f%%). " +
					"No se requiere riego adicional por ahora.",
					temp, hum);
			return new RuleResult(RiskLevel.LOW, msg, null);
		}
	}

	/**
	 * Evaluates phytosanitary risk using PDF thresholds:
	 * temp &gt; 28°C AND humidity &gt; 80% → HIGH; one condition → MEDIUM; neither → LOW.
	 */
	RuleResult computePhytosanitaryByRules(ClimateData climate, Crop crop) {
		BigDecimal temp = climate.temperature();
		BigDecimal hum = climate.humidity();
		boolean highTemp = temp.compareTo(PHYTO_TEMP_THRESHOLD) > 0;
		boolean highHum = hum.compareTo(PHYTO_HUMIDITY_THRESHOLD) > 0;

		String pests = pestsForCropType(crop);

		if (highTemp && highHum) {
			String msg = String.format(
					"¡Alerta fitosanitaria! Temperatura: %.1f°C y humedad: %.1f%% superan los umbrales " +
					"de riesgo (>28°C y >80%%). Revise su cultivo de %s hoy. Plagas probables: %s.",
					temp, hum, crop.getCropType(), pests);
			return new RuleResult(RiskLevel.HIGH, msg, pests);
		} else if (highTemp || highHum) {
			String condition = highTemp
					? String.format("temperatura alta (%.1f°C)", temp)
					: String.format("humedad alta (%.1f%%)", hum);
			String msg = String.format(
					"Condición de riesgo moderado: %s. Monitoree su cultivo de %s. Posibles plagas: %s.",
					condition, crop.getCropType(), pests);
			return new RuleResult(RiskLevel.MEDIUM, msg, pests);
		} else {
			String msg = String.format(
					"Condiciones climáticas dentro del rango normal (Temp: %.1f°C, Hum: %.1f%%). " +
					"No se detectan condiciones de riesgo fitosanitario elevado.",
					temp, hum);
			return new RuleResult(RiskLevel.LOW, msg, null);
		}
	}

	/**
	 * Rule-based fertilizer recommendation using crop growth stage derived from weeks since sowing.
	 * Stage thresholds are crop-independent (generic); the AI provides crop-specific precision.
	 */
	FertilizerRecommendationResponse computeFertilizerByRules(Crop crop, CropParameter params, ClimateData climate) {
		long weeks = ChronoUnit.WEEKS.between(crop.getSownDate(), LocalDate.now());

		String cropStage;
		String nutrient;
		String dose;
		RiskLevel level;
		String message;

		if (weeks < FERTILIZER_INITIAL_STAGE_WEEKS) {
			cropStage = "Germinación / Establecimiento";
			nutrient = "Nitrógeno (N)";
			dose = "30-40 kg N/ha";
			level = RiskLevel.HIGH;
			message = String.format(
					"Etapa inicial (%d semanas). Se sugiere aplicar nitrógeno (N) para estimular el " +
					"crecimiento radicular. Referencia base: %s.", weeks, params.getRecommendedFertilizer());
		} else if (weeks <= FERTILIZER_VEGETATIVE_STAGE_WEEKS) {
			cropStage = "Desarrollo vegetativo";
			nutrient = "NPK balanceado";
			dose = "Formula NPK según referencia del cultivo";
			level = RiskLevel.MEDIUM;
			message = String.format(
					"Etapa de desarrollo (%d semanas). Se sugiere fertilización NPK balanceada. " +
					"Referencia base: %s.", weeks, params.getRecommendedFertilizer());
		} else {
			cropStage = "Producción / Maduración";
			nutrient = "Potasio (K)";
			dose = "50-60 kg K2O/ha";
			level = RiskLevel.MEDIUM;
			message = String.format(
					"Etapa de producción (%d semanas). Se sugiere reforzar potasio (K) para calidad " +
					"del fruto. Referencia base: %s.", weeks, params.getRecommendedFertilizer());
		}

		return new FertilizerRecommendationResponse(
				UUID.randomUUID(),
				crop.getId(),
				cropStage,
				(int) weeks,
				nutrient,
				dose,
				level,
				message,
				LocalDateTime.now(),
				RecommendationSource.RULE
		);
	}

	// Shared helpers

	/** Common pests/diseases per crop type in the Magdalena region (Caribbean Colombia). */
	private String pestsForCropType(Crop crop) {
		return switch (crop.getCropType()) {
			case BANANO, PLATANO -> "sigatoka negra, trips, picudo negro";
			case MANGO -> "mosca de la fruta, antracnosis, trips";
			case YUCA -> "mosca blanca, ácaros, bacteriosis";
			case MAIZ -> "cogollero (Spodoptera), roya, pudrición de mazorca";
			case PALMA -> "Rhynchophorus palmarum, pudrición del cogollo, roya";
		};
	}

	private CropParameter requireParams(Crop crop) {
		return cropParameterRepository.findByCropType(crop.getCropType())
				.orElseThrow(() -> new ResourceNotFoundException("Parámetros del cultivo", crop.getCropType()));
	}

	private void persist(UUID id, Crop crop, RecommendationType type, RiskLevel level, String message,
			BigDecimal temperature, BigDecimal humidity, RecommendationSource source) {
		Recommendation rec = new Recommendation();
		rec.setId(id);
		rec.setCrop(crop);
		rec.setType(type);
		rec.setLevel(level);
		rec.setMessage(message);
		rec.setTemperature(temperature);
		rec.setHumidity(humidity);
		rec.setSource(source);
		rec.setGeneratedAt(LocalDateTime.now());
		rec.setSyncStatus(SyncStatus.SYNCED);
		recommendationRepository.save(rec);
	}

	/**
	 * Internal result holder for deterministic rule engines.
	 * {@code suspectedPests} is only populated by the phytosanitary engine.
	 */
	record RuleResult(RiskLevel level, String message, String suspectedPests) {}
}
