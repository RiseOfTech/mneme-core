package io.github.ifamneme.mneme_core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WordTest {

	@Test
	void rejectsNullValue() {
		assertThatThrownBy(() -> new Word(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsBlankValue() {
		assertThatThrownBy(() -> new Word("   ")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void roundTripsValidValue() {
		Word word = new Word("surround");
		assertThat(word.value()).isEqualTo("surround");
	}

	@Test
	void equalsAndHashCodeByValue() {
		assertThat(new Word("surround")).isEqualTo(new Word("surround"));
		assertThat(new Word("surround")).hasSameHashCodeAs(new Word("surround"));
	}

}
