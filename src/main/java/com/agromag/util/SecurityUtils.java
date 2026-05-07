package com.agromag.util;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;
import java.util.UUID;

// Utilidades de seguridad — extrae datos del usuario autenticado desde el JWT
public final class SecurityUtils {

	private SecurityUtils() {
		// Utility class — no instanciar
	}

	public static UUID getCurrentUserId(Principal principal) {
		if (principal instanceof JwtAuthenticationToken auth) {
			return UUID.fromString(auth.getToken().getSubject());
		}
		throw new IllegalStateException("Usuario no autenticado");
	}
}
