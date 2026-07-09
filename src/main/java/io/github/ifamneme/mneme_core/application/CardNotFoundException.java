package io.github.ifamneme.mneme_core.application;

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.UserId;

public class CardNotFoundException extends RuntimeException {

	public CardNotFoundException(CardId id, UserId owner) {
		super("Card " + id + " not found for owner " + owner);
	}

}
