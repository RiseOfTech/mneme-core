package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
public interface ReviewApi {

	@Operation(summary = "Record a grade for a card's review")
	@ApiResponse(responseCode = "404", description = "Review not found",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "400", description = "Validation failed",
			content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	@PostMapping("/{cardId}")
	ReviewResponseDto gradeReview(@PathVariable UUID cardId, @Valid @RequestBody GradeRequestDto request,
			@Parameter(hidden = true) @AuthenticatedUser UserId userId);

}
