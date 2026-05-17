package com.agromag.controller;

import com.agromag.dto.request.CropEventRequest;
import com.agromag.dto.response.CropEventResponse;
import com.agromag.service.CropEventService;
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

@Tag(name = "Eventos de Cultivo", description = "CRUD de eventos (riegos, fertilizaciones, etc.) dentro de un cultivo")
@RestController
@RequestMapping("/api/crops/{cropId}/events")
public class CropEventController {

	private final CropEventService cropEventService;

	public CropEventController(CropEventService cropEventService) {
		this.cropEventService = cropEventService;
	}

	@Operation(summary = "Crear evento", description = "Registra un nuevo evento en el cultivo indicado")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "Evento creado"),
		@ApiResponse(responseCode = "400", description = "Datos inválidos"),
		@ApiResponse(responseCode = "404", description = "Cultivo no encontrado")
	})
	@PostMapping
	public ResponseEntity<CropEventResponse> createEvent(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId,
			@Valid @RequestBody CropEventRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		CropEventResponse response = cropEventService.createEvent(userId, cropId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Listar eventos", description = "Retorna todos los eventos de un cultivo")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Lista de eventos"),
		@ApiResponse(responseCode = "404", description = "Cultivo no encontrado")
	})
	@GetMapping
	public ResponseEntity<List<CropEventResponse>> getEvents(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropEventService.getEventsByCrop(cropId, userId));
	}

	@Operation(summary = "Obtener evento", description = "Retorna un evento por ID")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Evento encontrado"),
		@ApiResponse(responseCode = "403", description = "El evento no pertenece al usuario"),
		@ApiResponse(responseCode = "404", description = "Evento no encontrado")
	})
	@GetMapping("/{eventId}")
	public ResponseEntity<CropEventResponse> getEvent(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId,
			@Parameter(description = "ID del evento") @PathVariable UUID eventId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropEventService.getEventById(eventId, userId));
	}

	@Operation(summary = "Actualizar evento", description = "Actualiza los datos de un evento existente")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Evento actualizado"),
		@ApiResponse(responseCode = "400", description = "Datos inválidos"),
		@ApiResponse(responseCode = "403", description = "El evento no pertenece al usuario"),
		@ApiResponse(responseCode = "404", description = "Evento no encontrado")
	})
	@PutMapping("/{eventId}")
	public ResponseEntity<CropEventResponse> updateEvent(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId,
			@Parameter(description = "ID del evento") @PathVariable UUID eventId,
			@Valid @RequestBody CropEventRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		return ResponseEntity.ok(cropEventService.updateEvent(eventId, userId, request));
	}

	@Operation(summary = "Eliminar evento", description = "Elimina un evento del cultivo")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "Evento eliminado"),
		@ApiResponse(responseCode = "403", description = "El evento no pertenece al usuario"),
		@ApiResponse(responseCode = "404", description = "Evento no encontrado")
	})
	@DeleteMapping("/{eventId}")
	public ResponseEntity<Void> deleteEvent(
			Principal principal,
			@Parameter(description = "ID del cultivo") @PathVariable UUID cropId,
			@Parameter(description = "ID del evento") @PathVariable UUID eventId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		cropEventService.deleteEvent(eventId, userId);
		return ResponseEntity.noContent().build();
	}
}
