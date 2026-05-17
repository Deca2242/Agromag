package com.agromag.repository;

import com.agromag.domain.entities.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Acceso a datos de perfiles de usuario
public interface ProfileRepository extends JpaRepository<Profile, UUID> {

	Optional<Profile> findByEmail(String email);

	boolean existsByEmail(String email);
}
