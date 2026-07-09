package io.github.ifamneme.mneme_core.domain;

public final class LeitnerBox {

	public static final int MAX_BOX = 5;

	private final int number;

	public LeitnerBox(int number) {
		if (number < 1 || number > MAX_BOX) {
			throw new IllegalArgumentException("number must be between 1 and " + MAX_BOX);
		}
		this.number = number;
	}

	public static LeitnerBox first() {
		return new LeitnerBox(1);
	}

	public int number() {
		return number;
	}

	public LeitnerBox next() {
		return new LeitnerBox(Math.min(number + 1, MAX_BOX));
	}

	public LeitnerBox reset() {
		return LeitnerBox.first();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof LeitnerBox other)) {
			return false;
		}
		return number == other.number;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(number);
	}

	@Override
	public String toString() {
		return Integer.toString(number);
	}

}
