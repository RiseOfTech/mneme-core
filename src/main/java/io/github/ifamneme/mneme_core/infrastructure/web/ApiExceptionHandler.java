package io.github.ifamneme.mneme_core.infrastructure.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.ifamneme.mneme_core.application.CardNotFoundException;
import io.github.ifamneme.mneme_core.application.ReviewNotFoundException;

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

	@ExceptionHandler(ReviewNotFoundException.class)
	public ProblemDetail handleReviewNotFound(ReviewNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
				"The requested review was not found.");
		problem.setProperty("code", ErrorCode.REVIEW_NOT_FOUND.name());
		// Spring fills a null "instance" with the request URI, which contains the review's
		// cardId and would make a foreign-review response distinguishable from a missing-review
		// one. Fix it to a card-id-free value so both are byte-identical.
		problem.setInstance(URI.create("/v1/reviews"));
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

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException ex) {
		// Malformed JSON and unrecognised enum values (e.g. an unknown grade) fail here,
		// before Bean Validation ever runs, so MethodArgumentNotValidException never fires.
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed.");
		problem.setProperty("code", ErrorCode.VALIDATION_FAILED.name());
		return problem;
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		// Without this, the generic Exception.class handler below would swallow the
		// method mismatch into a 500 before Spring's own resolver could ever set the
		// correct 405 - which also means Boot's container-level /error dispatch (and
		// the Security ERROR-dispatch permit that guards it) never gets exercised.
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED,
				"Request method '" + ex.getMethod() + "' is not supported for this endpoint.");
		problem.setProperty("code", ErrorCode.METHOD_NOT_ALLOWED.name());
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
