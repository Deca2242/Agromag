package com.agromag.controller;

import com.agromag.dto.request.CropEventRequest;
import com.agromag.dto.response.CropEventResponse;
import com.agromag.service.CropEventService;
import com.agromag.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/crops/{cropId}/events")
public class CropEventController {

	private final CropEventService cropEventService;

	public CropEventController(CropEventService cropEventService) {
		this.cropEventService = cropEventService;
	}

	@PostMapping
	public ResponseEntity<CropEventResponse> createEvent(
			Principal principal,
			@PathVariable UUID cropId,
			@Valid @RequestBody CropEventRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		CropEventResponse response = cropEventService.createEvent(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<CropEventResponse>> getEvents(
			Principal principal,
			@PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropEventService.getEventsByCrop(cropId, userId));
	}

	@GetMapping("/{eventId}")
	public ResponseEntity<CropEventResponse> getEvent(
			Principal principal,
			@PathVariable UUID cropId,
			@PathVariable UUID eventId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropEventService.getEventById(eventId, userId));
	}

	@PutMapping("/{eventId}")
	public ResponseEntity<CropEventResponse> updateEvent(
			Principal principal,
			@PathVariable UUID cropId,
			@PathVariable UUID eventId,
			@Valid @RequestBody CropEventRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropEventService.updateEvent(eventId, userId, request));
	}

	@DeleteMapping("/{eventId}")
	public ResponseEntity<Void> deleteEvent(
			Principal principal,
			@PathVariable UUID cropId,
			@PathVariable UUID eventId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		cropEventService.deleteEvent(eventId, userId);
		return ResponseEntity.noContent().build();
	}
}
