package io.github.ifamneme.mneme_core.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.ifamneme.mneme_core.application.CardNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(CardNotFoundException.class)
	public ResponseEntity<Void> handleCardNotFound(CardNotFoundException ex) {
		return ResponseEntity.notFound().build();
	}

}
