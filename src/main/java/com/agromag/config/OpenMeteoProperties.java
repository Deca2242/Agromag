package com.agromag.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;

// Binding type-safe de las propiedades de Open-Meteo
@ConfigurationProperties(prefix = "openmeteo")
public record OpenMeteoProperties(
		@NotBlank String url
) {
}
