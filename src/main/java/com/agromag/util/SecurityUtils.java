package com.agromag.util;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;
import java.util.UUID;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static UUID getCurrentUserId(Principal principal) {
		if (principal instanceof JwtAuthenticationToken auth) {
			return UUID.fromString(auth.getToken().getSubject());
		}
		throw new IllegalStateException("Usuario no autenticado");
	}

	public static String getEmail(Principal principal) {
		if (principal instanceof JwtAuthenticationToken auth) {
			return auth.getToken().getClaimAsString("email");
		}
		throw new IllegalStateException("Usuario no autenticado");
	}
}
