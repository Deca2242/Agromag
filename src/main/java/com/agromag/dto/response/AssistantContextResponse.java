package com.agromag.dto.response;

import java.util.List;
import java.util.UUID;

// Diagnostico del contexto real que recibe el asistente IA para el usuario actual
public record AssistantContextResponse(
		UUID profileId,
		String fullName,
		String municipality,
		int cropsCount,
		int alertsCount,
		int recommendationsCount,
		int eventsCount,
		String climateInfo,
		List<String> crops,
		List<String> alerts,
		List<String> recommendations,
		List<String> events
) {}
