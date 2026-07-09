package io.github.ifamneme.mneme_core.domain;

import java.util.Objects;

public final class Word {

	private final String value;

	public Word(String value) {
		Objects.requireNonNull(value, "value must not be null");
		if (value.isBlank()) {
			throw new IllegalArgumentException("value must not be blank");
		}
		this.value = value;
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Word other)) {
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
		return value;
	}

}
