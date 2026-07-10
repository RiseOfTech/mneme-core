package io.github.ifamneme.mneme_core.infrastructure.web;

import java.time.Instant;

import io.github.ifamneme.mneme_core.domain.Review;

public record ReviewResponseDto(int box, Instant dueAt) {

	public static ReviewResponseDto from(Review review) {
		return new ReviewResponseDto(review.state().box().number(), review.state().dueAt());
	}

}
