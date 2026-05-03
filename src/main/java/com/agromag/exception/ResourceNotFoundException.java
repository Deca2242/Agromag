package com.agromag.exception;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resourceName, Object identifier) {
		super(String.format("%s no encontrado con identificador: %s", resourceName, identifier));
	}
}
