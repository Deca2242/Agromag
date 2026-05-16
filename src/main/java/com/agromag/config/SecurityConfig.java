package com.agromag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Seguridad: JWT stateless + roles desde app_metadata de Supabase
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthenticationConverter jwtAuthenticationConverter;

	public SecurityConfig(JwtAuthenticationConverter jwtAuthenticationConverter) {
		this.jwtAuthenticationConverter = jwtAuthenticationConverter;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/api/health").permitAll()
					.requestMatchers("/api/auth/**").permitAll()
					.requestMatchers("/error").permitAll()
					// Swagger UI y OpenAPI docs — requieren autenticación
					.requestMatchers(
						"/swagger-ui.html",
						"/swagger-ui/**",
						"/v3/api-docs",
						"/v3/api-docs/**",
						"/v3/api-docs.yaml"
					).authenticated()
					.requestMatchers("/api/adr/**").hasRole("ADR_TECHNICIAN")
					.requestMatchers("/api/**").authenticated()
					.anyRequest().denyAll()
				)
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
			);

		return http.build();
	}
}
