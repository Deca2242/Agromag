package com.agromag.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Propiedades de configuración de Open-Meteo, enlazadas de forma type-safe desde application.properties
@ConfigurationProperties(prefix = "openmeteo")
public record OpenMeteoProperties(
		@NotBlank String url
) {
}
