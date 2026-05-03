package com.agromag.exception;

import java.util.UUID;

public class UnauthorizedCropAccessException extends RuntimeException {

	public UnauthorizedCropAccessException(UUID cropId, UUID profileId) {
		super(String.format("El usuario %s no tiene acceso al cultivo %s", profileId, cropId));
	}
}
