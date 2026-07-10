package io.github.ifamneme.mneme_core.application;

import org.springframework.stereotype.Service;

import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardRepository;

@Service
public class DefaultUpdateCardUseCase implements UpdateCardUseCase {

	private final CardRepository cardRepository;

	public DefaultUpdateCardUseCase(CardRepository cardRepository) {
		this.cardRepository = cardRepository;
	}

	@Override
	public Card update(UpdateCardCommand command) {
		Card card = cardRepository.findByIdAndOwner(command.cardId(), command.owner())
			.orElseThrow(() -> new CardNotFoundException(command.cardId(), command.owner()));
		Card updated = card.update(command.word(), command.translation(), command.association(), command.image(),
				command.visibility());
		return cardRepository.save(updated);
	}

}
