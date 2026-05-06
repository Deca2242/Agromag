package com.agromag.controller;

import com.agromag.dto.request.SyncBatchRequest;
import com.agromag.dto.response.SyncBatchResponse;
import com.agromag.service.SyncService;
import com.agromag.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

// Endpoint de sincronización batch (offline → servidor)
@RestController
@RequestMapping("/api/sync")
public class SyncController {

	private final SyncService syncService;

	public SyncController(SyncService syncService) {
		this.syncService = syncService;
	}

	@PostMapping("/batch")
	public ResponseEntity<SyncBatchResponse> syncBatch(
			Principal principal,
			@Valid @RequestBody SyncBatchRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		SyncBatchResponse response = syncService.processBatch(userId, request);
		return ResponseEntity.ok(response);
	}
}
