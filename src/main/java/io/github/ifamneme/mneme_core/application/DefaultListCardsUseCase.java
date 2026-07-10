package io.github.ifamneme.mneme_core.application;

import java.util.List;

import org.springframework.stereotype.Service;

import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.UserId;

@Service
public class DefaultListCardsUseCase implements ListCardsUseCase {

	private final CardRepository cardRepository;

	public DefaultListCardsUseCase(CardRepository cardRepository) {
		this.cardRepository = cardRepository;
	}

	@Override
	public List<Card> list(UserId owner) {
		return cardRepository.findAllByOwner(owner);
	}

}
