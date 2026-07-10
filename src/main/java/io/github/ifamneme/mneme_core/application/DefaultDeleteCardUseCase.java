package io.github.ifamneme.mneme_core.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;
import io.github.ifamneme.mneme_core.domain.UserId;

@Service
public class DefaultDeleteCardUseCase implements DeleteCardUseCase {

	private final CardRepository cardRepository;
	private final ReviewRepository reviewRepository;

	public DefaultDeleteCardUseCase(CardRepository cardRepository, ReviewRepository reviewRepository) {
		this.cardRepository = cardRepository;
		this.reviewRepository = reviewRepository;
	}

	@Override
	@Transactional
	public void delete(UserId owner, CardId cardId) {
		cardRepository.findByIdAndOwner(cardId, owner).orElseThrow(() -> new CardNotFoundException(cardId, owner));
		reviewRepository.deleteByCardIdAndOwner(cardId, owner);
		cardRepository.deleteByIdAndOwner(cardId, owner);
	}

}
