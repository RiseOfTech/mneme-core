package io.github.ifamneme.mneme_core.application;

import org.springframework.stereotype.Service;

import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.UserId;

@Service
public class DefaultGetCardUseCase implements GetCardUseCase {

	private final CardRepository cardRepository;

	public DefaultGetCardUseCase(CardRepository cardRepository) {
		this.cardRepository = cardRepository;
	}

	@Override
	public Card get(UserId owner, CardId id) {
		return cardRepository.findByIdAndOwner(id, owner).orElseThrow(() -> new CardNotFoundException(id, owner));
	}

}
