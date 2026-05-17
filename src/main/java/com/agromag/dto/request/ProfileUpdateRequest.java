package com.agromag.dto.request;

import com.agromag.domain.enums.Municipality;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Request para actualizar datos del perfil
public record ProfileUpdateRequest(
		@NotBlank @Size(min = 2, max = 100) String fullName,
		@NotNull Municipality municipality
) {
}
