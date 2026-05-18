package com.agromag.controller;

import com.agromag.config.RecommendationProperties;
import com.agromag.dto.request.RecommendationDecisionRequest;
import com.agromag.dto.response.FertilizerRecommendationResponse;
import com.agromag.dto.response.IrrigationRecommendationResponse;
import com.agromag.dto.response.PhytosanitaryRecommendationResponse;
import com.agromag.dto.response.RecommendationParametersResponse;
import com.agromag.dto.response.RecommendationResponse;
import com.agromag.repository.CropParameterRepository;
import com.agromag.service.RecommendationService;
import com.agromag.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Tag(name = "Recomendaciones", description = "Generación y gestión de recomendaciones agronómicas (riego, fertilización, fitosanitarios)")
@RestController
@RequestMapping("/api")
public class RecommendationController {

	private final RecommendationService recommendationService;
	private final RecommendationProperties recommendationProperties;
	private final CropParameterRepository cropParameterRepository;

	public RecommendationController(
			RecommendationService recommendationService,
			RecommendationProperties recommendationProperties,
			CropParameterRepository cropParameterRepository) {
		this.recommendationService = recommendationService;
		this.recommendationProperties = recommendationProperties;
		this.cropParameterRepository = cropParameterRepository;
	}

	@Operation(
		summary = "Parámetros del motor de reglas",
		description = "Retorna los umbrales de reglas y parámetros agronómicos para sincronizar el motor offline del cliente"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Parámetros del motor de reglas")
	})
	@GetMapping("/recommendations/parameters")
	public ResponseEntity<java.util.Map<String, String>> getRecommendationParameters() {
		var cropParams = cropParameterRepository.findAllByOrderByCropTypeAsc();
		var response = RecommendationParametersResponse.from(recommendationProperties, cropParams);
		return ResponseEntity.ok(response.toFlatMap());
	}

	@Operation(summary = "Listar recomendaciones", description = "Retorna el historial de recomendaciones de un cultivo")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Lista de recomendaciones"),
		@ApiResponse(responseCode = "404", description = "Cultivo no encontrado")
	})
	@GetMapping("/crops/{cropId}/recommendations")
	public ResponseEntity<Page<RecommendationResponse>> getRecommendations(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId,
			@Parameter(description = "Filtro: any, pending, decided") @RequestParam(defaultValue = "any") String followed,
			@PageableDefault(size = 10, sort = "generatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		var filter = RecommendationService.RecommendationFollowedFilter.fromParam(followed);
		return ResponseEntity.ok(recommendationService.getRecommendationsPage(cropId, userId, filter, pageable));
	}

	@Operation(summary = "Anular decisión", description = "Deja la recomendación otra vez sin decisión (followed=null)")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Decisión anulada"),
			@ApiResponse(responseCode = "404", description = "Recomendación no encontrada")
	})
	@DeleteMapping("/recommendations/{recommendationId}/decision")
	public ResponseEntity<Void> resetRecommendationDecision(
			Principal principal,
			@Parameter(description = "ID de la recomendación") @PathVariable UUID recommendationId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		recommendationService.resetDecision(userId, recommendationId);
		return ResponseEntity.noContent().build();
	}

	@Operation(
		summary = "Generar recomendación de riego",
		description = "Genera una recomendación de riego basada en datos climáticos actuales y el estado del cultivo"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Recomendación generada"),
		@ApiResponse(responseCode = "404", description = "Cultivo no encontrado")
	})
	@PostMapping("/crops/{cropId}/recommendations/irrigation")
	public ResponseEntity<IrrigationRecommendationResponse> generateIrrigation(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		IrrigationRecommendationResponse response = recommendationService.generateIrrigation(cropId, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(
		summary = "Generar recomendación de fertilización",
		description = "Genera una recomendación de fertilización según la etapa fenológica del cultivo"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Recomendación generada"),
		@ApiResponse(responseCode = "404", description = "Cultivo no encontrado")
	})
	@PostMapping("/crops/{cropId}/recommendations/fertilizer")
	public ResponseEntity<FertilizerRecommendationResponse> generateFertilizer(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		FertilizerRecommendationResponse response = recommendationService.generateFertilizer(cropId, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(
		summary = "Generar recomendación fitosanitaria",
		description = "Genera una alerta fitosanitaria basada en temperatura y humedad relativa del cultivo"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Recomendación generada"),
		@ApiResponse(responseCode = "404", description = "Cultivo no encontrado")
	})
	@PostMapping("/crops/{cropId}/recommendations/phytosanitary")
	public ResponseEntity<PhytosanitaryRecommendationResponse> generatePhytosanitary(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		PhytosanitaryRecommendationResponse response = recommendationService.generatePhytosanitary(cropId, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(
		summary = "Registrar decisión sobre recomendación",
		description = "Marca una recomendación como ACEPTADA o RECHAZADA por el agricultor"
	)
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "Decisión registrada"),
		@ApiResponse(responseCode = "400", description = "Datos inválidos"),
		@ApiResponse(responseCode = "404", description = "Recomendación no encontrada")
	})
	@PatchMapping("/recommendations/decision")
	public ResponseEntity<Void> markDecision(
			Principal principal,
			@Valid @RequestBody RecommendationDecisionRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		recommendationService.markDecision(userId, request);
		return ResponseEntity.noContent().build();
	}
}
