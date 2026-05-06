package com.agromag.exception;

// Error al consultar la API de Open-Meteo (→ HTTP 502)
public class ClimateServiceException extends RuntimeException {

	public ClimateServiceException(String message) {
		super(message);
	}

	public ClimateServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
