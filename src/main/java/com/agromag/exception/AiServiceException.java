package com.agromag.exception;

// Error al comunicarse con el servicio de IA (→ HTTP 502)
public class AiServiceException extends RuntimeException {

	public AiServiceException(String message) {
		super(message);
	}

	public AiServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
