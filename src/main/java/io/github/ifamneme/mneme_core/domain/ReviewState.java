package io.github.ifamneme.mneme_core.domain;

import java.time.Instant;
import java.util.Objects;

public final class ReviewState {

	private final LeitnerBox box;
	private final Instant dueAt;

	public ReviewState(LeitnerBox box, Instant dueAt) {
		this.box = Objects.requireNonNull(box, "box must not be null");
		this.dueAt = Objects.requireNonNull(dueAt, "dueAt must not be null");
	}

	public LeitnerBox box() {
		return box;
	}

	public Instant dueAt() {
		return dueAt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ReviewState other)) {
			return false;
		}
		return box.equals(other.box) && dueAt.equals(other.dueAt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(box, dueAt);
	}

	@Override
	public String toString() {
		return "ReviewState{box=" + box + ", dueAt=" + dueAt + "}";
	}

}
