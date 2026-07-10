package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ifamneme.mneme_core.application.GradeReviewCommand;
import io.github.ifamneme.mneme_core.application.GradeReviewUseCase;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.UserId;

@RestController
@RequestMapping("/v1/reviews")
public class ReviewController implements ReviewApi {

	private final GradeReviewUseCase gradeReviewUseCase;

	public ReviewController(GradeReviewUseCase gradeReviewUseCase) {
		this.gradeReviewUseCase = gradeReviewUseCase;
	}

	@Override
	public ReviewResponseDto gradeReview(UUID cardId, GradeRequestDto request, UserId userId) {
		GradeReviewCommand command = new GradeReviewCommand(userId, CardId.of(cardId), request.grade().toDomain());
		Review review = gradeReviewUseCase.grade(command);
		return ReviewResponseDto.from(review);
	}

}
