package io.github.ifamneme.mneme_core.domain;

import java.util.Objects;
import java.util.UUID;

public final class CardId {

	private final UUID value;

	private CardId(UUID value) {
		this.value = Objects.requireNonNull(value, "value must not be null");
	}

	public static CardId newId() {
		return new CardId(UUID.randomUUID());
	}

	public static CardId of(UUID value) {
		return new CardId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CardId other)) {
			return false;
		}
		return value.equals(other.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value.toString();
	}

}
