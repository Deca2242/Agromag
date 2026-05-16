package com.agromag.controller;

import com.agromag.domain.enums.RecommendationType;
import com.agromag.dto.response.AlertResponse;
import com.agromag.service.AlertService;
import com.agromag.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Alertas", description = "Gestion de alertas automaticas para cultivos")
@RestController
@RequestMapping("/api")
public class AlertController {

	private final AlertService alertService;

	public AlertController(AlertService alertService) {
		this.alertService = alertService;
	}

	@Operation(summary = "Listar alertas", description = "Retorna las alertas del usuario autenticado, filtrable por tipo")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Lista de alertas"),
		@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@GetMapping("/alerts")
	public ResponseEntity<Page<AlertResponse>> getAlerts(
			Principal principal,
			@Parameter(description = "Filtro por tipo: IRRIGATION, FERTILIZER, PHYTOSANITARY")
			@RequestParam(required = false) String type,
			@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		RecommendationType recommendationType = null;
		if (type != null && !type.isBlank()) {
			try {
				recommendationType = RecommendationType.valueOf(type.toUpperCase());
			} catch (IllegalArgumentException e) {
				// Si el tipo no es valido, retorna todas las alertas
			}
		}
		return ResponseEntity.ok(alertService.getAlerts(userId, recommendationType, pageable));
	}

	@Operation(summary = "Contar alertas no leidas", description = "Retorna el numero de alertas MEDIUM/HIGH no leidas")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Conteo de alertas no leidas"),
		@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@GetMapping("/alerts/unread/count")
	public ResponseEntity<Map<String, Long>> getUnreadCount(Principal principal) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		long count = alertService.countUnread(userId);
		long highCount = alertService.countUnreadHigh(userId);
		return ResponseEntity.ok(Map.of(
				"total", count,
				"high", highCount
		));
	}

	@Operation(summary = "Marcar alerta como leida", description = "Marca una alerta especifica como leida")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "Alerta marcada como leida"),
		@ApiResponse(responseCode = "404", description = "Alerta no encontrada"),
		@ApiResponse(responseCode = "403", description = "Sin acceso a esta alerta")
	})
	@PatchMapping("/alerts/{alertId}/read")
	public ResponseEntity<Void> markAsRead(
			Principal principal,
			@Parameter(description = "ID de la alerta") @PathVariable UUID alertId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		alertService.markAsRead(userId, alertId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Eliminar alerta", description = "Elimina una alerta especifica")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "Alerta eliminada"),
		@ApiResponse(responseCode = "404", description = "Alerta no encontrada"),
		@ApiResponse(responseCode = "403", description = "Sin acceso a esta alerta")
	})
	@DeleteMapping("/alerts/{alertId}")
	public ResponseEntity<Void> deleteAlert(
			Principal principal,
			@Parameter(description = "ID de la alerta") @PathVariable UUID alertId) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		alertService.deleteAlert(userId, alertId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Eliminar alertas leidas", description = "Elimina todas las alertas que ya han sido marcadas como leidas")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Numero de alertas eliminadas"),
		@ApiResponse(responseCode = "401", description = "No autenticado")
	})
	@DeleteMapping("/alerts/read")
	public ResponseEntity<Map<String, Object>> deleteAllRead(Principal principal) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		int deleted = alertService.deleteAllRead(userId);
		return ResponseEntity.ok(Map.of(
				"deleted", deleted
		));
	}
}
