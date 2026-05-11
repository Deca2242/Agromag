package com.agromag.service;

import com.agromag.config.RecommendationProperties;
import com.agromag.domain.entities.Crop;
import com.agromag.domain.entities.CropParameter;
import com.agromag.domain.entities.Recommendation;
import com.agromag.domain.enums.RecommendationSource;
import com.agromag.domain.enums.RecommendationType;
import com.agromag.domain.enums.RiskLevel;
import com.agromag.domain.enums.SyncStatus;
import com.agromag.domain.model.ClimateData;
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

// Orquesta la generación de recomendaciones (riego, fertilización, fitosanitaria)
@Service
public class RecommendationService {

	private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

	private final RecommendationRepository recommendationRepository;
	private final CropParameterRepository cropParameterRepository;
	private final CropService cropService;
	private final ClimateService climateService;
	private final AiRecommendationService aiRecommendationService;
	private final RecommendationProperties props;

	public RecommendationService(RecommendationRepository recommendationRepository,
								 CropParameterRepository cropParameterRepository,
								 CropService cropService,
								 ClimateService climateService,
								 AiRecommendationService aiRecommendationService,
								 RecommendationProperties props) {
		this.recommendationRepository = recommendationRepository;
		this.cropParameterRepository = cropParameterRepository;
		this.cropService = cropService;
		this.climateService = climateService;
		this.aiRecommendationService = aiRecommendationService;
		this.props = props;
	}

	@Transactional(readOnly = true)
	public List<RecommendationResponse> getRecommendationsByCrop(UUID cropId, UUID profileId) {
		cropService.findCropAndValidateOwnership(cropId, profileId);
		return recommendationRepository.findByCrop_Id(cropId).stream()
				.map(RecommendationResponse::from)
				.toList();
	}

	// Registra si el productor siguió una recomendación (CU-09)
	@Transactional
	public void markDecision(UUID profileId, RecommendationDecisionRequest request) {
		Recommendation rec = recommendationRepository.findById(request.recommendationId())
				.orElseThrow(() -> new ResourceNotFoundException("Recomendación", request.recommendationId()));
		cropService.findCropAndValidateOwnership(rec.getCrop().getId(), profileId);
		rec.setFollowed(request.followed());
		recommendationRepository.save(rec);
		log.info("mark_decision recommendationId={} followed={}", request.recommendationId(), request.followed());
	}

	// Genera recomendación de riego (RF-04) — IA primero, reglas como respaldo
	@Transactional
	public IrrigationRecommendationResponse generateIrrigation(UUID cropId, UUID profileId) {
		Crop crop = cropService.findCropAndValidateOwnership(cropId, profileId);
		CropParameter params = requireParams(crop);
		ClimateData climate = climateService.getCurrentClimate(crop.getMunicipality());
		LocalDateTime now = LocalDateTime.now();

		RuleResult ruleResult = computeIrrigationByRules(climate, params, crop);

		IrrigationRecommendationResponse response;
		try {
			response = aiRecommendationService.generateIrrigationRecommendation(crop, params, climate);
			// Si las reglas detectan riesgo alto, no permitimos que la IA lo suavice
			if (ruleResult.level() == RiskLevel.HIGH && response.level() != RiskLevel.HIGH) {
				log.warn("ai_irrigation_level_override cropId={} aiLevel={} overriddenTo=HIGH",
						cropId, response.level());
				response = response.withLevelAndSource(RiskLevel.HIGH, RecommendationSource.RULE);
			}
		} catch (AiServiceException e) {
			log.warn("ai_irrigation_fallback cropId={} reason={}", cropId, e.getMessage());
			response = new IrrigationRecommendationResponse(
					UUID.randomUUID(), cropId, ruleResult.level(),
					ruleResult.message(), climate.temperature(), now,
					RecommendationSource.RULE);
		}

		persist(new PersistRequest(response.id(), crop, RecommendationType.IRRIGATION, response.level(),
				response.message(), climate.temperature(), climate.humidity(), response.source(),
				response.generatedAt()));
		log.info("generate_irrigation cropId={} level={} source={}", cropId, response.level(), response.source());
		return response;
	}

	// Genera recomendación de fertilización (RF-05, RF-06) — IA primero, reglas como respaldo
	@Transactional
	public FertilizerRecommendationResponse generateFertilizer(UUID cropId, UUID profileId) {
		Crop crop = cropService.findCropAndValidateOwnership(cropId, profileId);
		CropParameter params = requireParams(crop);
		ClimateData climate = climateService.getCurrentClimate(crop.getMunicipality());
		LocalDateTime now = LocalDateTime.now();

		FertilizerRecommendationResponse response;
		try {
			response = aiRecommendationService.generateFertilizerRecommendation(crop, params, climate);
		} catch (AiServiceException e) {
			log.warn("ai_fertilizer_fallback cropId={} reason={}", cropId, e.getMessage());
			FertilizerRuleResult rule = computeFertilizerByRules(crop, params, now);
			response = new FertilizerRecommendationResponse(
					UUID.randomUUID(), cropId, rule.cropStage(), rule.weeks(),
					rule.nutrient(), rule.dose(), rule.level(), rule.message(),
					now, RecommendationSource.RULE);
		}

		persist(new PersistRequest(response.id(), crop, RecommendationType.FERTILIZER, response.level(),
				response.message(), climate.temperature(), climate.humidity(), response.source(),
				response.generatedAt()));
		log.info("generate_fertilizer cropId={} level={} source={}", cropId, response.level(), response.source());
		return response;
	}

	// Genera alerta fitosanitaria (RF-07, RF-08) — IA primero, reglas como respaldo
	@Transactional
	public PhytosanitaryRecommendationResponse generatePhytosanitary(UUID cropId, UUID profileId) {
		Crop crop = cropService.findCropAndValidateOwnership(cropId, profileId);
		CropParameter params = requireParams(crop);
		ClimateData climate = climateService.getCurrentClimate(crop.getMunicipality());
		LocalDateTime now = LocalDateTime.now();

		PhytosanitaryRuleResult ruleResult = computePhytosanitaryByRules(climate, crop);

		PhytosanitaryRecommendationResponse response;
		try {
			response = aiRecommendationService.generatePhytosanitaryRecommendation(
					crop, params, climate, ruleResult.level());
			// Si las reglas detectan riesgo alto, no permitimos que la IA lo suavice
			if (ruleResult.level() == RiskLevel.HIGH && response.level() != RiskLevel.HIGH) {
				log.warn("ai_phytosanitary_level_override cropId={} aiLevel={} overriddenTo=HIGH",
						cropId, response.level());
				response = response.withLevelAndSource(RiskLevel.HIGH, RecommendationSource.RULE);
			}
		} catch (AiServiceException e) {
			log.warn("ai_phytosanitary_fallback cropId={} reason={}", cropId, e.getMessage());
			response = new PhytosanitaryRecommendationResponse(
					UUID.randomUUID(), cropId, ruleResult.level(), ruleResult.message(),
					ruleResult.suspectedPests(), climate.temperature(), climate.humidity(),
					now, RecommendationSource.RULE);
		}

		persist(new PersistRequest(response.id(), crop, RecommendationType.PHYTOSANITARY, response.level(),
				response.message(), climate.temperature(), climate.humidity(), response.source(),
				response.generatedAt()));
		log.info("generate_phytosanitary cropId={} level={} source={}", cropId, response.level(), response.source());
		return response;
	}

	// Motor de reglas para riego — evalúa umbrales FAO simplificados
	// Acceso package-private para permitir pruebas unitarias directas
	RuleResult computeIrrigationByRules(ClimateData climate, CropParameter params, Crop crop) {
		BigDecimal temp = climate.temperature();
		BigDecimal hum = climate.humidity();
		BigDecimal maxTemp = params.getOptimalTempMax();
		BigDecimal minHum = params.getHumidityMin();

		boolean tempExceeded = temp.compareTo(maxTemp) > 0;
		boolean humLow = hum.compareTo(minHum) < 0;
		boolean tempNearMax = temp.compareTo(maxTemp.subtract(props.irrigationNearMaxDelta())) > 0;

		if (tempExceeded || humLow) {
			String msg = String.format(
					"¡Alerta! Temperatura: %.1f°C (máx óptima: %.1f°C), Humedad: %.1f%% (mín óptima: %.1f%%). " +
					"Se recomienda riego inmediato. Referencia de riego para %s: %s.",
					temp, maxTemp, hum, minHum, crop.getCropType(), params.getIrrigationNeeds());
			return new RuleResult(RiskLevel.HIGH, msg);
		} else if (tempNearMax) {
			String msg = String.format(
					"La temperatura actual (%.1f°C) se acerca a la máxima óptima (%.1f°C). " +
					"Considere programar riego en las próximas horas. Referencia: %s.",
					temp, maxTemp, params.getIrrigationNeeds());
			return new RuleResult(RiskLevel.MEDIUM, msg);
		} else {
			String msg = String.format(
					"Condiciones dentro del rango óptimo (Temp: %.1f°C, Hum: %.1f%%). " +
					"No se requiere riego adicional por ahora.",
					temp, hum);
			return new RuleResult(RiskLevel.LOW, msg);
		}
	}

	// Motor de reglas fitosanitario — temp > 28°C Y humedad > 80% → ALTO; una condición → MEDIO; ninguna → BAJO
	// Acceso package-private para permitir pruebas unitarias directas
	PhytosanitaryRuleResult computePhytosanitaryByRules(ClimateData climate, Crop crop) {
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
			return new PhytosanitaryRuleResult(RiskLevel.HIGH, msg, pests);
		} else if (highTemp || highHum) {
			String condition = highTemp
					? String.format("temperatura alta (%.1f°C)", temp)
					: String.format("humedad alta (%.1f%%)", hum);
			String msg = String.format(
					"Condición de riesgo moderado: %s. Monitoree su cultivo de %s. Posibles plagas: %s.",
					condition, crop.getCropType(), pests);
			return new PhytosanitaryRuleResult(RiskLevel.MEDIUM, msg, pests);
		} else {
			String msg = String.format(
					"Condiciones climáticas dentro del rango normal (Temp: %.1f°C, Hum: %.1f%%). " +
					"No se detectan condiciones de riesgo fitosanitario elevado.",
					temp, hum);
			return new PhytosanitaryRuleResult(RiskLevel.LOW, msg, null);
		}
	}

	// Motor de reglas para fertilización — calcula etapa del cultivo según semanas desde la siembra
	// Los umbrales son genéricos; la IA proporciona precisión específica por cultivo
	// Acceso package-private para permitir pruebas unitarias directas
	FertilizerRuleResult computeFertilizerByRules(Crop crop, CropParameter params, LocalDateTime now) {
		long weeks = ChronoUnit.WEEKS.between(crop.getSownDate(), LocalDate.now());

		String cropStage;
		String nutrient;
		String dose;
		RiskLevel level;
		String message;

		if (weeks < props.fertilizerInitialStageWeeks()) {
			cropStage = "Germinación / Establecimiento";
			nutrient = "Nitrógeno (N)";
			dose = "30-40 kg N/ha";
			level = RiskLevel.HIGH;
			message = String.format(
					"Etapa inicial (%d semanas). Se sugiere aplicar nitrógeno (N) para estimular el " +
					"crecimiento radicular. Referencia base: %s.", weeks, params.getRecommendedFertilizer());
		} else if (weeks <= props.fertilizerVegetativeStageWeeks()) {
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

		return new FertilizerRuleResult(level, message, cropStage, nutrient, dose, (int) weeks);
	}

	// Busca los parámetros agronómicos del cultivo o lanza 404
	private CropParameter requireParams(Crop crop) {
		return cropParameterRepository.findByCropType(crop.getCropType())
				.orElseThrow(() -> new ResourceNotFoundException("Parámetros del cultivo", crop.getCropType()));
	}

	// Persiste la recomendación generada en la base de datos
	private void persist(PersistRequest req) {
		Recommendation rec = new Recommendation();
		rec.setId(req.id());
		rec.setCrop(req.crop());
		rec.setType(req.type());
		rec.setLevel(req.level());
		rec.setMessage(req.message());
		rec.setTemperature(req.temperature());
		rec.setHumidity(req.humidity());
		rec.setSource(req.source());
		rec.setGeneratedAt(req.generatedAt());
		rec.setSyncStatus(SyncStatus.SYNCED);
		recommendationRepository.save(rec);
	}

	// Datos necesarios para persistir una recomendación — reemplaza la lista larga de parámetros
	private record PersistRequest(
			UUID id,
			Crop crop,
			RecommendationType type,
			RiskLevel level,
			String message,
			BigDecimal temperature,
			BigDecimal humidity,
			RecommendationSource source,
			LocalDateTime generatedAt
	) {}

	// Resultado interno del motor de reglas de riego
	record RuleResult(RiskLevel level, String message) {}

	// Resultado interno del motor de reglas fitosanitario — incluye plagas sospechosas
	record PhytosanitaryRuleResult(RiskLevel level, String message, String suspectedPests) {}

	// Resultado interno del motor de reglas de fertilización — incluye etapa, nutriente, dosis y semanas
	record FertilizerRuleResult(
			RiskLevel level, String message,
			String cropStage, String nutrient, String dose, int weeks
	) {}
}
