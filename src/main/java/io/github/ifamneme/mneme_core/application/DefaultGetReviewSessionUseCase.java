package io.github.ifamneme.mneme_core.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;
import io.github.ifamneme.mneme_core.domain.UserId;

@Service
public class DefaultGetReviewSessionUseCase implements GetReviewSessionUseCase {

	private final ReviewRepository reviewRepository;
	private final CardRepository cardRepository;
	private final Clock clock;

	public DefaultGetReviewSessionUseCase(ReviewRepository reviewRepository, CardRepository cardRepository,
			Clock clock) {
		this.reviewRepository = reviewRepository;
		this.cardRepository = cardRepository;
		this.clock = clock;
	}

	@Override
	public List<ReviewSessionView> get(UserId owner) {
		List<Review> dueReviews = reviewRepository.findDue(owner, clock.instant());
		if (dueReviews.isEmpty()) {
			return List.of();
		}

		List<CardId> cardIds = dueReviews.stream().map(Review::cardId).toList();
		List<Card> cards = cardRepository.findAllByIdsAndOwner(cardIds, owner);
		Map<CardId, Card> cardsById = cards.stream().collect(Collectors.toMap(Card::id, Function.identity()));

		return dueReviews.stream().map(review -> {
			Card card = cardsById.get(review.cardId());
			if (card == null) {
				throw new ReviewSessionIntegrityException(review.cardId(), owner);
			}
			return new ReviewSessionView(card.id(), card.word(), card.translation(), card.association(),
					card.image(), review.state().box(), review.state().dueAt());
		}).toList();
	}

}
