package com.agromag.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// Request para el chat conversacional con el asistente IA
public record ChatRequest(
		@NotBlank @Size(max = 2000) String message,
		@Valid List<ChatTurn> history
) {
	public record ChatTurn(
			@NotBlank String role,
			@NotBlank String content
	) {}
}
