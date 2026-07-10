package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.github.ifamneme.mneme_core.domain.UserId;
import jakarta.validation.Valid;

public interface ReviewApi {

	@PostMapping("/{cardId}")
	ReviewResponseDto gradeReview(@PathVariable UUID cardId, @Valid @RequestBody GradeRequestDto request,
			@AuthenticatedUser UserId userId);

}
