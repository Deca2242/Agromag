package com.agromag.service;

import java.math.BigDecimal;

// Datos clim\u00e1ticos actuales obtenidos de Open-Meteo
public record ClimateData(BigDecimal temperature, BigDecimal humidity) {
}
