package com.agromag.exception;

import java.util.UUID;

// Conflicto de sincronización (→ HTTP 409)
public class SyncConflictException extends RuntimeException {

	public SyncConflictException(String entity, UUID id) {
		super(String.format("Conflicto de sincronización para %s con id: %s", entity, id));
	}
}
