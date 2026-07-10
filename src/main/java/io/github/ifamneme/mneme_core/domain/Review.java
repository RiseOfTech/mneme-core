package io.github.ifamneme.mneme_core.domain;

import java.time.Instant;
import java.util.Objects;

public final class Review {

	private final CardId cardId;
	private final UserId owner;
	private final ReviewState state;

	public Review(CardId cardId, UserId owner, ReviewState state) {
		this.cardId = Objects.requireNonNull(cardId, "cardId must not be null");
		this.owner = Objects.requireNonNull(owner, "owner must not be null");
		this.state = Objects.requireNonNull(state, "state must not be null");
	}

	public CardId cardId() {
		return cardId;
	}

	public UserId owner() {
		return owner;
	}

	public ReviewState state() {
		return state;
	}

	public Review grade(Grade grade, LeitnerStrategy strategy, Instant now) {
		ReviewState newState = strategy.next(this.state, grade, now);
		return new Review(this.cardId, this.owner, newState);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Review other)) {
			return false;
		}
		return cardId.equals(other.cardId);
	}

	@Override
	public int hashCode() {
		return cardId.hashCode();
	}

	@Override
	public String toString() {
		return "Review{cardId=" + cardId + "}";
	}

}
