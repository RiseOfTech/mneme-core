package io.github.ifamneme.mneme_core.application;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.LeitnerStrategy;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;
import io.github.ifamneme.mneme_core.domain.ReviewState;

@Service
public class DefaultCreateCardUseCase implements CreateCardUseCase {

	private final CardRepository cardRepository;
	private final ReviewRepository reviewRepository;
	private final LeitnerStrategy leitnerStrategy;
	private final Clock clock;

	public DefaultCreateCardUseCase(CardRepository cardRepository, ReviewRepository reviewRepository,
			LeitnerStrategy leitnerStrategy, Clock clock) {
		this.cardRepository = cardRepository;
		this.reviewRepository = reviewRepository;
		this.leitnerStrategy = leitnerStrategy;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Card create(CreateCardCommand command) {
		Card card = new Card(command.owner(), command.word(), command.translation(), command.association(),
				command.image(), command.visibility());
		Card savedCard = cardRepository.save(card);

		ReviewState initialState = leitnerStrategy.initialState(clock.instant());
		Review review = new Review(savedCard.id(), command.owner(), initialState);
		reviewRepository.save(review);

		return savedCard;
	}

}
