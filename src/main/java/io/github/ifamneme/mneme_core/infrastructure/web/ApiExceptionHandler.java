package io.github.ifamneme.mneme_core.infrastructure.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.ifamneme.mneme_core.application.CardNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(CardNotFoundException.class)
	public ProblemDetail handleCardNotFound(CardNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested card was not found.");
		problem.setProperty("code", ErrorCode.CARD_NOT_FOUND.name());
		// Spring fills a null "instance" with the request URI, which contains the card
		// id and would make a foreign-card response distinguishable from a missing-card
		// one. Fix it to a card-id-free value so both are byte-identical.
		problem.setInstance(URI.create("/v1/cards"));
		return problem;
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidationFailed(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			errors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed.");
		problem.setProperty("code", ErrorCode.VALIDATION_FAILED.name());
		problem.setProperty("errors", errors);
		return problem;
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred.");
		problem.setProperty("code", ErrorCode.INTERNAL_ERROR.name());
		return problem;
	}

}
