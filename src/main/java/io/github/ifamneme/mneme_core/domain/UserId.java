package io.github.ifamneme.mneme_core.domain;

import java.util.Objects;
import java.util.UUID;

public final class UserId {

	private final UUID value;

	public UserId(UUID value) {
		this.value = Objects.requireNonNull(value, "value must not be null");
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof UserId other)) {
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
