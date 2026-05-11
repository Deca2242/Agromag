package com.agromag.util;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;
import java.util.UUID;

// Utilidades de seguridad — extrae datos del usuario autenticado desde el JWT
public final class SecurityUtils {

	private SecurityUtils() {
		// Clase de utilidad — no instanciar
	}

	// Extrae el UUID del usuario desde el claim "sub" del JWT
	public static UUID getCurrentUserId(Principal principal) {
		if (principal instanceof JwtAuthenticationToken auth) {
			return UUID.fromString(auth.getToken().getSubject());
		}
		throw new IllegalStateException("Usuario no autenticado");
	}

	// Extrae el email del usuario desde el claim "email" del JWT
	public static String getEmail(Principal principal) {
		if (principal instanceof JwtAuthenticationToken auth) {
			return auth.getToken().getClaimAsString("email");
		}
		throw new IllegalStateException("Usuario no autenticado");
	}
}
