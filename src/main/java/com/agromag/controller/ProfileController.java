package com.agromag.controller;

import com.agromag.domain.entities.Profile;
import com.agromag.domain.enums.Municipality;
import com.agromag.service.ProfileService;
import com.agromag.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	/**
	 * Obtiene el perfil del usuario autenticado.
	 * Si es la primera vez, lo crea automáticamente (auto-registro).
	 */
	@GetMapping
	public ResponseEntity<Profile> getProfile(Principal principal) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		String email = ((JwtAuthenticationToken) principal).getToken().getClaimAsString("email");
		Profile profile = profileService.getOrCreateProfile(userId, email);
		return ResponseEntity.ok(profile);
	}

	/**
	 * Actualiza los datos del perfil (fullName y municipality).
	 */
	@PutMapping
	public ResponseEntity<Profile> updateProfile(
			Principal principal,
			@RequestParam String fullName,
			@RequestParam Municipality municipality) {
		UUID userId = SecurityUtils.getCurrentUserId(principal);
		Profile profile = profileService.updateProfile(userId, fullName, municipality);
		return ResponseEntity.ok(profile);
	}
}
