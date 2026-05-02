package com.agromag.util;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.security.Principal;
import java.util.UUID;

public class SecurityUtils {

	public static UUID getCurrentUserId(Principal principal) {
		if (principal instanceof JwtAuthenticationToken) {
			JwtAuthenticationToken auth = (JwtAuthenticationToken) principal;
			return UUID.fromString(auth.getToken().getSubject());
		}
		throw new IllegalStateException("Usuario no autenticado");
	}
}
