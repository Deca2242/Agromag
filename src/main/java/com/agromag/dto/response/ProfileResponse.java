package com.agromag.dto.response;

import com.agromag.domain.entities.Profile;
import com.agromag.domain.enums.Municipality;
import com.agromag.domain.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

// Respuesta con datos del perfil del usuario
public record ProfileResponse(
		UUID id,
		String email,
		Role role,
		String fullName,
		Municipality municipality,
		LocalDateTime createdAt
) {
	public static ProfileResponse from(Profile profile) {
		return new ProfileResponse(
				profile.getId(),
				profile.getEmail(),
				profile.getRole(),
				profile.getFullName(),
				profile.getMunicipality(),
				profile.getCreatedAt()
		);
	}
}
