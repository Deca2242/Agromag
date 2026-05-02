package com.agromag.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
		return new JwtAuthenticationToken(jwt, authorities);
	}

	private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
		// El rol vive en app_metadata — nunca en user_metadata (editable por el cliente)
		Map<String, Object> appMetadata = jwt.getClaim("app_metadata");
		if (appMetadata != null) {
			String role = (String) appMetadata.get("role");
			if (role != null) {
				return Collections.singletonList(
					new SimpleGrantedAuthority("ROLE_" + role)
				);
			}
		}
		return Collections.emptyList();
	}
}
