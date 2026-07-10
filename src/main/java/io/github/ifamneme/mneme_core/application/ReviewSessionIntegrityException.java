package io.github.ifamneme.mneme_core.application;

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.UserId;

public class ReviewSessionIntegrityException extends RuntimeException {

	public ReviewSessionIntegrityException(CardId cardId, UserId owner) {
		super("Review references card " + cardId + " for owner " + owner + " but no such card exists");
	}

}
