package com.agromag.service;

import com.agromag.domain.entities.Profile;
import com.agromag.domain.enums.Municipality;
import com.agromag.domain.enums.Role;
import com.agromag.dto.response.ProfileResponse;
import com.agromag.exception.ResourceNotFoundException;
import com.agromag.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Lógica de negocio para perfiles de usuario
@Service
public class ProfileService {

	private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

	private final ProfileRepository profileRepository;

	public ProfileService(ProfileRepository profileRepository) {
		this.profileRepository = profileRepository;
	}

	// Busca un perfil por ID; si no existe, lo crea con rol PRODUCER (auto-registro desde JWT)
	@Transactional
	public ProfileResponse getOrCreateProfile(UUID userId, String email) {
		Profile profile = profileRepository.findById(userId).orElseGet(() -> {
			// No logueamos el email por ser dato personal 
			log.info("auto_register_profile userId={}", userId);
			Profile newProfile = new Profile();
			newProfile.setId(userId);
			newProfile.setEmail(email);
			newProfile.setRole(Role.PRODUCER);
			newProfile.setFullName("");
			newProfile.setMunicipality(Municipality.SANTA_MARTA);
			return profileRepository.save(newProfile);
		});
		return ProfileResponse.from(profile);
	}

	// Obtiene un perfil existente o lanza 404
	// Acceso package-private: devuelve la entidad JPA para uso interno entre servicios
	@Transactional(readOnly = true)
	Profile getProfileById(UUID userId) {
		return profileRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Perfil", userId));
	}

	// Actualiza los datos editables del perfil
	@Transactional
	public ProfileResponse updateProfile(UUID userId, String fullName, Municipality municipality) {
		Profile profile = getProfileById(userId);
		profile.setFullName(fullName);
		profile.setMunicipality(municipality);
		log.info("update_profile userId={}", userId);
		return ProfileResponse.from(profileRepository.save(profile));
	}
}
