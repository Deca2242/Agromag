package com.agromag.controller;

import com.agromag.dto.request.ChatRequest;
import com.agromag.dto.response.ChatResponse;
import com.agromag.service.AssistantService;
import com.agromag.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "Asistente IA", description = "Chat conversacional con asistente agrícola")
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

	private final AssistantService assistantService;

	public AssistantController(AssistantService assistantService) {
		this.assistantService = assistantService;
	}

	@Operation(
		summary = "Enviar mensaje al asistente",
		description = "Envía un mensaje y el historial previo; devuelve la respuesta del asistente."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Respuesta del asistente"),
		@ApiResponse(responseCode = "400", description = "Datos inválidos"),
		@ApiResponse(responseCode = "401", description = "Token JWT inválido o ausente")
	})
	@PostMapping("/chat")
	public ResponseEntity<ChatResponse> chat(
			Principal principal,
			@Valid @RequestBody ChatRequest request) {
		var profileId = SecurityUtils.getCurrentUserId(principal);
		var email = SecurityUtils.getEmail(principal);
		return ResponseEntity.ok(assistantService.respond(profileId, email, request));
	}
}
