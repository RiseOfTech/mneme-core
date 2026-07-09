package io.github.ifamneme.mneme_core.infrastructure.persistence;

import org.springframework.stereotype.Component;

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.LeitnerBox;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewState;
import io.github.ifamneme.mneme_core.domain.UserId;

@Component
class DefaultReviewMapper implements ReviewMapper {

	@Override
	public ReviewEntity toEntity(Review review) {
		return new ReviewEntity(review.cardId().value(), review.owner().value(), review.state().box().number(),
				review.state().dueAt());
	}

	@Override
	public Review toDomain(ReviewEntity entity) {
		ReviewState state = new ReviewState(new LeitnerBox(entity.getBox()), entity.getDueAt());
		return new Review(CardId.of(entity.getCardId()), new UserId(entity.getOwnerId()), state);
	}

}
