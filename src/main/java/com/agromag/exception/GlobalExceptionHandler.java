package com.agromag.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
		return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(UnauthorizedCropAccessException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedCropAccessException ex) {
		return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(SyncConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflict(SyncConflictException ex) {
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(ClimateServiceException.class)
	public ResponseEntity<ErrorResponse> handleClimateError(ClimateServiceException ex) {
		return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
	}

	@ExceptionHandler(AiServiceException.class)
	public ResponseEntity<ErrorResponse> handleAiError(AiServiceException ex) {
		return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new HashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.put(error.getField(), error.getDefaultMessage())
		);

		Map<String, Object> body = new HashMap<>();
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", "Validation Error");
		body.put("fieldErrors", fieldErrors);
		body.put("timestamp", LocalDateTime.now());
		return ResponseEntity.badRequest().body(body);
	}

	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(
				new ErrorResponse(status.value(), status.getReasonPhrase(), message, LocalDateTime.now())
		);
	}
}
