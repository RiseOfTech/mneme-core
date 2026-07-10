package io.github.ifamneme.mneme_core.application;

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.UserId;

public class ReviewNotFoundException extends RuntimeException {

	public ReviewNotFoundException(CardId id, UserId owner) {
		super("Review " + id + " not found for owner " + owner);
	}

}
