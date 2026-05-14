package com.agromag.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

// Manejo centralizado de excepciones — evita que stacktraces lleguen al cliente
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// Respuesta estándar de error
	public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
	}

	// Respuesta específica para errores de validación de DTOs
	public record ValidationErrorResponse(int status, String error, Map<String, String> fieldErrors,
			LocalDateTime timestamp) {
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
		log.error("climate_service_error", ex);
		return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
	}

	@ExceptionHandler(AiServiceException.class)
	public ResponseEntity<ErrorResponse> handleAiError(AiServiceException ex) {
		log.error("ai_service_error", ex);
		return buildResponse(HttpStatus.BAD_GATEWAY, ex.getMessage());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "inválido",
						(first, second) -> first // si hay dos errores para el mismo campo, queda el primero
				));

		ValidationErrorResponse body = new ValidationErrorResponse(
				HttpStatus.BAD_REQUEST.value(),
				"Validation Error",
				fieldErrors,
				LocalDateTime.now());

		return ResponseEntity.badRequest().body(body);
	}

	// Fallback genérico — atrapa cualquier excepción no mapeada
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
		log.error("unexpected_error", ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
	}

	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(
				new ErrorResponse(status.value(), status.getReasonPhrase(), message, LocalDateTime.now()));
	}
}
