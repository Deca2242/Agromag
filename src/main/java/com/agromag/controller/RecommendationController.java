package com.agromag.controller;

import com.agromag.dto.request.RecommendationDecisionRequest;
import com.agromag.dto.response.FertilizerRecommendationResponse;
import com.agromag.dto.response.IrrigationRecommendationResponse;
import com.agromag.dto.response.PhytosanitaryRecommendationResponse;
import com.agromag.dto.response.RecommendationResponse;
import com.agromag.service.RecommendationService;
import com.agromag.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Tag(name = "Recomendaciones", description = "Generación y gestión de recomendaciones agronómicas (riego, fertilización, fitosanitarios)")
@RestController
@RequestMapping("/api")
public class RecommendationController {

	private final RecommendationService recommendationService;

	public RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@Operation(summary = "Listar recomendaciones", description = "Retorna el historial de recomendaciones de un cultivo")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Lista de recomendaciones"),
		@ApiResponse(responseCode = "404", description = "Cultivo no encontrado")
	})
	@GetMapping("/crops/{cropId}/recommendations")
	public ResponseEntity<List<RecommendationResponse>> getRecommendations(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(recommendationService.getRecommendationsByCrop(cropId, userId));
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
