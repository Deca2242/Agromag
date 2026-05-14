package com.agromag.dto.response;

import java.time.Instant;

// Respuesta del asistente IA conversacional
public record ChatResponse(String reply, Instant timestamp) {}
