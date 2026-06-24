package com.interviewhelper.common;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiError> handleBusinessException(BusinessException exception) {
		return ResponseEntity
			.status(exception.getStatus())
			.body(new ApiError(exception.getCode(), exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.collect(Collectors.joining(", "));

		if (message.isBlank()) {
			message = "요청 값이 올바르지 않습니다.";
		}

		return ResponseEntity
			.badRequest()
			.body(new ApiError("INVALID_REQUEST", message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException exception) {
		return ResponseEntity
			.badRequest()
			.body(new ApiError("INVALID_REQUEST", exception.getMessage()));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiError> handleNoResourceFoundException(NoResourceFoundException exception) {
		return ResponseEntity
			.status(HttpStatus.NOT_FOUND)
			.body(new ApiError("NOT_FOUND", "요청한 경로를 찾을 수 없습니다."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleException(Exception exception) {
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new ApiError("INTERNAL_SERVER_ERROR", "서버 처리 중 오류가 발생했습니다."));
	}
}
