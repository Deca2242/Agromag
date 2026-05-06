package com.agromag.exception;

// Se lanza cuando un recurso no se encuentra en la base de datos (→ HTTP 404)
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resourceName, Object identifier) {
		super(String.format("%s no encontrado con identificador: %s", resourceName, identifier));
	}
}
