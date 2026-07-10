package io.github.ifamneme.mneme_core.application;

import java.time.Clock;

import org.springframework.stereotype.Service;

import io.github.ifamneme.mneme_core.domain.LeitnerStrategy;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;

@Service
public class DefaultGradeReviewUseCase implements GradeReviewUseCase {

	private final ReviewRepository reviewRepository;
	private final LeitnerStrategy leitnerStrategy;
	private final Clock clock;

	public DefaultGradeReviewUseCase(ReviewRepository reviewRepository, LeitnerStrategy leitnerStrategy, Clock clock) {
		this.reviewRepository = reviewRepository;
		this.leitnerStrategy = leitnerStrategy;
		this.clock = clock;
	}

	@Override
	public Review grade(GradeReviewCommand command) {
		Review review = reviewRepository.findByCardIdAndOwner(command.cardId(), command.owner())
				.orElseThrow(() -> new ReviewNotFoundException(command.cardId(), command.owner()));
		Review graded = review.grade(command.grade(), leitnerStrategy, clock.instant());
		return reviewRepository.save(graded);
	}

}
