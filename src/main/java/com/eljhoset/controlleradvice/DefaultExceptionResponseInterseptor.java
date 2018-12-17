package com.eljhoset.controlleradvice;

import com.eljhoset.controlleradvice.model.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

public class DefaultExceptionResponseInterseptor implements ExceptionResponseInterseptor {

	@Override
	public Object handle(Exception ex) {
		return Optional.ofNullable(ex.getClass().getAnnotation(ApiException.class))
				.map(e -> buildApiError(e.status(), e.code(), e.message().isEmpty() ? ex.getMessage() : e.message()))
				.orElse(buildApiError(HttpStatus.BAD_REQUEST, null, "Request could no be processed"));
	}

	private ResponseEntity<ApiError> buildApiError(HttpStatus status, Integer code, String message) {
		return ResponseEntity.status(status).body(ApiError.builder().code(code).message(message).build());
	}
}
