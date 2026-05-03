package com.agromag.domain.entities;

import com.agromag.domain.enums.Municipality;
import com.agromag.domain.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Perfil de aplicación enlazado al usuario de Supabase Auth ({@code auth.users}).
 * {@code id} debe coincidir con el claim {@code sub} del JWT (UUID).
 */
@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor
public class Profile {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(nullable = false, unique = true)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private Role role;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Municipality municipality;

	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = false)
	private List<Crop> crops = new ArrayList<>();

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
	}
}
