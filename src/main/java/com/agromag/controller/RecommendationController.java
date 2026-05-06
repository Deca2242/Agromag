package com.agromag.controller;

import com.agromag.dto.request.RecommendationDecisionRequest;
import com.agromag.dto.response.FertilizerRecommendationResponse;
import com.agromag.dto.response.IrrigationRecommendationResponse;
import com.agromag.dto.response.RecommendationResponse;
import com.agromag.service.RecommendationService;
import com.agromag.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

// Endpoints de recomendaciones de riego, fertilización y decisiones
@RestController
@RequestMapping("/api")
public class RecommendationController {

	private final RecommendationService recommendationService;

	public RecommendationController(RecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@GetMapping("/crops/{cropId}/recommendations")
	public ResponseEntity<List<RecommendationResponse>> getRecommendations(
			Principal principal,
			@PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(recommendationService.getRecommendationsByCrop(cropId, userId));
	}

	@PostMapping("/crops/{cropId}/recommendations/irrigation")
	public ResponseEntity<IrrigationRecommendationResponse> generateIrrigation(
			Principal principal,
			@PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		IrrigationRecommendationResponse response = recommendationService.generateIrrigation(cropId, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/crops/{cropId}/recommendations/fertilizer")
	public ResponseEntity<FertilizerRecommendationResponse> generateFertilizer(
			Principal principal,
			@PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		FertilizerRecommendationResponse response = recommendationService.generateFertilizer(cropId, userId);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PatchMapping("/recommendations/decision")
	public ResponseEntity<Void> markDecision(
			Principal principal,
			@Valid @RequestBody RecommendationDecisionRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		recommendationService.markDecision(userId, request);
		return ResponseEntity.noContent().build();
	}
}
