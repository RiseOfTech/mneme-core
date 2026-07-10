package io.github.ifamneme.mneme_core.application;

import io.github.ifamneme.mneme_core.domain.Review;

public interface GradeReviewUseCase {
	Review grade(GradeReviewCommand command);
}
