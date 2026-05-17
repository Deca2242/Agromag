package com.agromag.controller;

import com.agromag.dto.request.ProfileUpdateRequest;
import com.agromag.dto.response.ProfileResponse;
import com.agromag.service.ProfileService;
import com.agromag.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@Tag(name = "Perfil", description = "Gestión del perfil del usuario autenticado")
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@Operation(
		summary = "Obtener perfil",
		description = "Retorna el perfil del usuario autenticado. Si es la primera vez, lo crea automáticamente."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Perfil del usuario"),
		@ApiResponse(responseCode = "401", description = "Token JWT inválido o ausente")
	})
	@GetMapping
	public ResponseEntity<ProfileResponse> getProfile(Principal principal) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		String email = SecurityUtils.getEmail(principal);
		ProfileResponse profile = profileService.getOrCreateProfile(userId, email);
		return ResponseEntity.ok(profile);
	}

	@Operation(summary = "Actualizar perfil", description = "Actualiza el nombre completo y municipio del perfil")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Perfil actualizado"),
		@ApiResponse(responseCode = "400", description = "Datos inválidos"),
		@ApiResponse(responseCode = "404", description = "Perfil no encontrado")
	})
	@PutMapping
	public ResponseEntity<ProfileResponse> updateProfile(
			Principal principal,
			@Valid @RequestBody ProfileUpdateRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		ProfileResponse profile = profileService.updateProfile(userId, request.fullName(), request.municipality());
		return ResponseEntity.ok(profile);
	}
}
