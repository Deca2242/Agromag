package com.agromag.controller;

import com.agromag.dto.request.CropTechnicalDataRequest;
import com.agromag.dto.response.CropTechnicalDataResponse;
import com.agromag.service.CropTechnicalDataService;
import com.agromag.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * REST controller for managing technical data sheets attached to crops.
 * All endpoints require JWT authentication.
 *
 * @since 1.1
 */
@RestController
@RequestMapping("/api/crops/{cropId}/technical-data")
public class CropTechnicalDataController {

	private final CropTechnicalDataService technicalDataService;

	public CropTechnicalDataController(CropTechnicalDataService technicalDataService) {
		this.technicalDataService = technicalDataService;
	}

	/**
	 * Creates or updates the technical data sheet for a crop (idempotent upsert).
	 *
	 * @param principal the authenticated user
	 * @param cropId the crop identifier
	 * @param request the technical data payload
	 * @return the persisted technical data
	 */
	@PutMapping
	public ResponseEntity<CropTechnicalDataResponse> saveTechnicalData(
			Principal principal,
			@PathVariable UUID cropId,
			@Valid @RequestBody CropTechnicalDataRequest request) {
		UUID profileId = SecurityUtils.getCurrentUserId(principal);
		CropTechnicalDataResponse response = technicalDataService.saveTechnicalData(cropId, profileId, request);
		return ResponseEntity.ok(response);
	}

	/**
	 * Returns the technical data sheet for a crop.
	 *
	 * @param principal the authenticated user
	 * @param cropId the crop identifier
	 * @return the technical data or 404 if none exists
	 */
	@GetMapping
	public ResponseEntity<CropTechnicalDataResponse> getTechnicalData(
			Principal principal,
			@PathVariable UUID cropId) {
		UUID profileId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(technicalDataService.getTechnicalData(cropId, profileId));
	}

	/**
	 * Deletes the technical data sheet for a crop.
	 *
	 * @param principal the authenticated user
	 * @param cropId the crop identifier
	 * @return 204 No Content on success
	 */
	@DeleteMapping
	public ResponseEntity<Void> deleteTechnicalData(
			Principal principal,
			@PathVariable UUID cropId) {
		UUID profileId = SecurityUtils.getCurrentUserId(principal);
		technicalDataService.deleteTechnicalData(cropId, profileId);
		return ResponseEntity.noContent().build();
	}
}
