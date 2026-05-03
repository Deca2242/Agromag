package com.agromag.service;

import com.agromag.domain.entities.Profile;
import com.agromag.domain.enums.Municipality;
import com.agromag.domain.enums.Role;
import com.agromag.exception.ResourceNotFoundException;
import com.agromag.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProfileService {

	private final ProfileRepository profileRepository;

	public ProfileService(ProfileRepository profileRepository) {
		this.profileRepository = profileRepository;
	}

	/**
	 * Busca un perfil por ID. Si no existe, lo crea automáticamente
	 * con rol PRODUCER (auto-registro desde el primer login con JWT).
	 */
	@Transactional
	public Profile getOrCreateProfile(UUID userId, String email) {
		return profileRepository.findById(userId).orElseGet(() -> {
			Profile profile = new Profile();
			profile.setId(userId);
			profile.setEmail(email);
			profile.setRole(Role.PRODUCER);
			profile.setFullName("");
			profile.setMunicipality(Municipality.SANTA_MARTA);
			return profileRepository.save(profile);
		});
	}

	/**
	 * Obtiene un perfil existente o lanza 404.
	 */
	public Profile getProfileById(UUID userId) {
		return profileRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Perfil", userId));
	}

	/**
	 * Actualiza los datos editables del perfil.
	 */
	@Transactional
	public Profile updateProfile(UUID userId, String fullName, Municipality municipality) {
		Profile profile = getProfileById(userId);
		profile.setFullName(fullName);
		profile.setMunicipality(municipality);
		return profileRepository.save(profile);
	}
}
