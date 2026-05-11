package com.agromag.domain.model;

import java.math.BigDecimal;

// Datos climáticos actuales obtenidos de Open-Meteo (temperatura en °C, humedad en %)
public record ClimateData(BigDecimal temperature, BigDecimal humidity) {
}
