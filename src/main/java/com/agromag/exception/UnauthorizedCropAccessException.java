package com.agromag.exception;

import java.util.UUID;

// Se lanza cuando un usuario intenta acceder a un cultivo que no le pertenece (→ HTTP 403)
public class UnauthorizedCropAccessException extends RuntimeException {

	public UnauthorizedCropAccessException(UUID cropId, UUID profileId) {
		// Mensaje genérico al cliente — los IDs se logean en el servidor para no filtrar información
		super("No tiene acceso a este recurso");
	}
}
