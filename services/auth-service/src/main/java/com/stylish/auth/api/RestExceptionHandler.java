package com.stylish.auth.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class RestExceptionHandler {
	@ExceptionHandler(ApiException.class)
	public ProblemDetail handleApiException(ApiException ex, HttpServletRequest request) {
		return problem(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request.getRequestURI(), null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<Map<String, Object>> violations = ex.getBindingResult().getFieldErrors().stream()
				.map(this::fieldErrorToViolation)
				.collect(Collectors.toList());
		return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Validation failed", request.getRequestURI(), violations);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolations(ConstraintViolationException ex, HttpServletRequest request) {
		List<Map<String, Object>> violations = ex.getConstraintViolations().stream()
				.map(v -> Map.<String, Object>of(
						"path", String.valueOf(v.getPropertyPath()),
						"message", v.getMessage(),
						"invalidValue", v.getInvalidValue()
				))
				.toList();
		return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Validation failed", request.getRequestURI(), violations);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Malformed JSON request", request.getRequestURI(), null);
	}

	@ExceptionHandler(ErrorResponseException.class)
	public ProblemDetail handleErrorResponseException(ErrorResponseException ex, HttpServletRequest request) {
		ProblemDetail pd = ex.getBody();
		if (pd == null) {
			return problem(HttpStatus.valueOf(ex.getStatusCode().value()), ErrorCode.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
		}
		pd.setProperty("timestamp", Instant.now().toString());
		pd.setProperty("path", request.getRequestURI());
		return pd;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleAny(Exception ex, HttpServletRequest request) {
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Unexpected error", request.getRequestURI(), null);
	}

	private Map<String, Object> fieldErrorToViolation(FieldError fe) {
		return Map.of(
				"field", fe.getField(),
				"message", fe.getDefaultMessage(),
				"rejectedValue", fe.getRejectedValue()
		);
	}

	private ProblemDetail problem(HttpStatus status, ErrorCode errorCode, String message, String path, Object violations) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
		pd.setTitle(status.getReasonPhrase());
		pd.setProperty("errorCode", errorCode.name());
		pd.setProperty("timestamp", Instant.now().toString());
		pd.setProperty("path", path);
		if (violations != null) {
			pd.setProperty("violations", violations);
		}
		return pd;
	}
}

