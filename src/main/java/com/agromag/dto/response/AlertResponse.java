package com.agromag.dto.response;

import com.agromag.domain.entities.Alert;
import com.agromag.domain.enums.AlertSeverity;
import com.agromag.domain.enums.RecommendationType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

// DTO de respuesta para alertas — enviado al frontend
public record AlertResponse(
		UUID id,
		UUID cropId,
		String cropTag,
		RecommendationType type,
		AlertSeverity severity,
		String title,
		String message,
		Integer iconCode,
		String createdAt,
		boolean isRead
) {
	private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

	public static AlertResponse from(Alert alert) {
		return new AlertResponse(
				alert.getId(),
				alert.getCrop().getId(),
				alert.getCropTag(),
				alert.getType(),
				alert.getSeverity(),
				alert.getTitle(),
				alert.getMessage(),
				alert.getIconCode(),
				alert.getCreatedAt().format(FMT),
				alert.getReadAt() != null
		);
	}
}
