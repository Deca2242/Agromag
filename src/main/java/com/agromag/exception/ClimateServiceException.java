package com.agromag.exception;

public class ClimateServiceException extends RuntimeException {

	public ClimateServiceException(String message) {
		super(message);
	}

	public ClimateServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
