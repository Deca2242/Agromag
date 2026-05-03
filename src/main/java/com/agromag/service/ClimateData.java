package com.agromag.service;

import java.math.BigDecimal;

/**
 * Datos climáticos actuales obtenidos de Open-Meteo.
 */
public record ClimateData(BigDecimal temperature, BigDecimal humidity) {
}
