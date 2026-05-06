package com.agromag.exception;

import java.util.UUID;

// Se lanza cuando un usuario intenta acceder a un cultivo que no le pertenece (→ HTTP 403)
public class UnauthorizedCropAccessException extends RuntimeException {

	public UnauthorizedCropAccessException(UUID cropId, UUID profileId) {
		super(String.format("El usuario %s no tiene acceso al cultivo %s", profileId, cropId));
	}
}
