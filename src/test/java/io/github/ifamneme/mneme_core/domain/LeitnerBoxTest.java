package io.github.ifamneme.mneme_core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LeitnerBoxTest {

	@Test
	void rejectsBelowRange() {
		assertThatThrownBy(() -> new LeitnerBox(0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsAboveRange() {
		assertThatThrownBy(() -> new LeitnerBox(LeitnerBox.MAX_BOX + 1)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void firstIsBoxOne() {
		assertThat(LeitnerBox.first().number()).isEqualTo(1);
	}

	@Test
	void nextPromotes() {
		assertThat(new LeitnerBox(2).next().number()).isEqualTo(3);
	}

	@Test
	void nextCapsAtMaxBox() {
		assertThat(new LeitnerBox(LeitnerBox.MAX_BOX).next().number()).isEqualTo(LeitnerBox.MAX_BOX);
	}

	@Test
	void resetReturnsBoxOne() {
		assertThat(new LeitnerBox(4).reset().number()).isEqualTo(1);
	}

}
