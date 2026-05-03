package com.agromag.controller;

import com.agromag.dto.request.CropRequest;
import com.agromag.dto.response.CropResponse;
import com.agromag.service.CropService;
import com.agromag.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/crops")
public class CropController {

	private final CropService cropService;

	public CropController(CropService cropService) {
		this.cropService = cropService;
	}

	@PostMapping
	public ResponseEntity<CropResponse> createCrop(
			Principal principal,
			@Valid @RequestBody CropRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		CropResponse response = cropService.createCrop(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<CropResponse>> getCrops(Principal principal) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropService.getCropsByProfile(userId));
	}

	@GetMapping("/{cropId}")
	public ResponseEntity<CropResponse> getCrop(
			Principal principal,
			@PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropService.getCropById(cropId, userId));
	}

	@PutMapping("/{cropId}")
	public ResponseEntity<CropResponse> updateCrop(
			Principal principal,
			@PathVariable UUID cropId,
			@Valid @RequestBody CropRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropService.updateCrop(cropId, userId, request));
	}

	@DeleteMapping("/{cropId}")
	public ResponseEntity<Void> deleteCrop(
			Principal principal,
			@PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		cropService.deleteCrop(cropId, userId);
		return ResponseEntity.noContent().build();
	}
}
