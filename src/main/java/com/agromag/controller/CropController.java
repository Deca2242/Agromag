package com.agromag.controller;

import com.agromag.dto.request.CropRequest;
import com.agromag.dto.response.CropResponse;
import com.agromag.service.CropService;
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

@Tag(name = "Cultivos", description = "CRUD de cultivos del usuario autenticado")
@RestController
@RequestMapping("/api/crops")
public class CropController {

	private final CropService cropService;

	public CropController(CropService cropService) {
		this.cropService = cropService;
	}

	@Operation(summary = "Crear cultivo", description = "Registra un nuevo cultivo para el usuario autenticado")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Cultivo creado exitosamente"),
		@ApiResponse(responseCode = "400", description = "Datos inválidos"),
		@ApiResponse(responseCode = "401", description = "Token JWT inválido o ausente")
	})
	@PostMapping
	public ResponseEntity<CropResponse> createCrop(
			Principal principal,
			@Valid @RequestBody CropRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		CropResponse response = cropService.createCrop(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Listar cultivos", description = "Retorna todos los cultivos del usuario autenticado")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Lista de cultivos"),
		@ApiResponse(responseCode = "401", description = "Token JWT inválido o ausente")
	})
	@GetMapping
	public ResponseEntity<List<CropResponse>> getCrops(Principal principal) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropService.getCropsByProfile(userId));
	}

	@Operation(summary = "Obtener cultivo", description = "Retorna un cultivo por ID (debe pertenecer al usuario)")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Cultivo encontrado"),
		@ApiResponse(responseCode = "403", description = "El cultivo no pertenece al usuario"),
		@ApiResponse(responseCode = "404", description = "Cultivo no encontrado")
	})
	@GetMapping("/{cropId}")
	public ResponseEntity<CropResponse> getCrop(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropService.getCropById(cropId, userId));
	}

	@Operation(summary = "Actualizar cultivo", description = "Actualiza los datos de un cultivo existente")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Cultivo actualizado"),
		@ApiResponse(responseCode = "400", description = "Datos inválidos"),
		@ApiResponse(responseCode = "403", description = "El cultivo no pertenece al usuario"),
		@ApiResponse(responseCode = "404", description = "Cultivo no encontrado")
	})
	@PutMapping("/{cropId}")
	public ResponseEntity<CropResponse> updateCrop(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId,
			@Valid @RequestBody CropRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropService.updateCrop(cropId, userId, request));
	}

	@Operation(summary = "Eliminar cultivo", description = "Elimina un cultivo y todos sus datos asociados")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "Cultivo eliminado"),
		@ApiResponse(responseCode = "403", description = "El cultivo no pertenece al usuario"),
		@ApiResponse(responseCode = "404", description = "Cultivo no encontrado")
	})
	@DeleteMapping("/{cropId}")
	public ResponseEntity<Void> deleteCrop(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		cropService.deleteCrop(cropId, userId);
		return ResponseEntity.noContent().build();
	}
}
