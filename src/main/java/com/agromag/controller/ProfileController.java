package com.agromag.controller;

import com.agromag.dto.request.ProfileUpdateRequest;
import com.agromag.dto.response.ProfileResponse;
import com.agromag.service.ProfileService;
import com.agromag.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

// Endpoints de perfil del usuario autenticado
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	// Obtiene el perfil del usuario autenticado (auto-registro si es la primera vez)
	@GetMapping
	public ResponseEntity<ProfileResponse> getProfile(Principal principal) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		String email = ((JwtAuthenticationToken) principal).getToken().getClaimAsString("email");
		ProfileResponse profile = profileService.getOrCreateProfile(userId, email);
		return ResponseEntity.ok(profile);
	}

	// Actualiza los datos del perfil (fullName y municipality)
	@PutMapping
	public ResponseEntity<ProfileResponse> updateProfile(
			Principal principal,
			@Valid @RequestBody ProfileUpdateRequest request) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		ProfileResponse profile = profileService.updateProfile(userId, request.fullName(), request.municipality());
		return ResponseEntity.ok(profile);
	}
}
