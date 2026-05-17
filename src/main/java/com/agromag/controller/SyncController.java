package com.agromag.controller;

import com.agromag.dto.request.SyncBatchRequest;
import com.agromag.dto.response.SyncBatchResponse;
import com.agromag.service.SyncService;
import com.agromag.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Tag(name = "Sincronización", description = "Sincronización batch de datos generados en modo offline")
@RestController
@RequestMapping("/api/sync")
public class SyncController {

	private final SyncService syncService;

	public SyncController(SyncService syncService) {
		this.syncService = syncService;
	}

	@Operation(
		summary = "Sincronizar lote offline",
		description = "Recibe y procesa un lote de operaciones generadas mientras el dispositivo estaba sin conexión. "
			+ "Retorna el resultado de cada operación procesada (exitosa o fallida)."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Lote procesado (puede contener errores parciales)"),
		@ApiResponse(responseCode = "400", description = "Formato del lote inválido"),
		@ApiResponse(responseCode = "401", description = "Token JWT inválido o ausente")
	})
	@PostMapping("/batch")
	public ResponseEntity<SyncBatchResponse> syncBatch(
			Principal principal,
			@Valid @RequestBody SyncBatchRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		SyncBatchResponse response = syncService.processBatch(userId, request);
		return ResponseEntity.ok(response);
	}
}
