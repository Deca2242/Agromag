package com.agromag.dto.response;

import java.time.Instant;
import java.util.List;

// Respuesta del asistente IA conversacional
public record ChatResponse(
		String reply,
		Instant timestamp,
		List<String> suggestions
) {
	public ChatResponse(String reply, Instant timestamp) {
		this(reply, timestamp, List.of());
	}
}
