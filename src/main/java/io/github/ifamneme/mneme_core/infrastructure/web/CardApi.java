package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.github.ifamneme.mneme_core.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@SecurityRequirement(name = "bearerAuth")
public interface CardApi {

	@Operation(summary = "Create a card")
	@ApiResponse(responseCode = "201", description = "Card created")
	@ApiResponse(responseCode = "400", description = "Validation failed",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@PostMapping
	ResponseEntity<CardResponseDto> createCard(@Valid @RequestBody CreateCardRequestDto request,
			@Parameter(hidden = true) @AuthenticatedUser UserId userId);

	@Operation(summary = "Get a card by id")
	@ApiResponse(responseCode = "404", description = "Card not found",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@GetMapping("/{id}")
	CardResponseDto getCard(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticatedUser UserId userId);

	@Operation(summary = "List the caller's cards")
	@GetMapping
	List<CardResponseDto> listCards(@Parameter(hidden = true) @AuthenticatedUser UserId userId);

	@Operation(summary = "Update a card's content")
	@ApiResponse(responseCode = "404", description = "Card not found",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "400", description = "Validation failed",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@PutMapping("/{id}")
	CardResponseDto updateCard(@PathVariable UUID id, @Valid @RequestBody UpdateCardRequestDto request,
			@Parameter(hidden = true) @AuthenticatedUser UserId userId);

	@Operation(summary = "Delete a card")
	@ApiResponse(responseCode = "204", description = "Card deleted")
	@ApiResponse(responseCode = "404", description = "Card not found",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@DeleteMapping("/{id}")
	ResponseEntity<Void> deleteCard(@PathVariable UUID id, @Parameter(hidden = true) @AuthenticatedUser UserId userId);

}
