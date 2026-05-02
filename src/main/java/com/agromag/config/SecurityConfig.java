package com.agromag.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

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
				.requestMatchers("/api/auth/**").permitAll()
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
