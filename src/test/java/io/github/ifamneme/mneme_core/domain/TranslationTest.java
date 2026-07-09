package io.github.ifamneme.mneme_core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TranslationTest {

	@Test
	void rejectsNullValue() {
		assertThatThrownBy(() -> new Translation(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsBlankValue() {
		assertThatThrownBy(() -> new Translation("   ")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void roundTripsValidValue() {
		Translation translation = new Translation("окружать");
		assertThat(translation.value()).isEqualTo("окружать");
	}

	@Test
	void equalsAndHashCodeByValue() {
		assertThat(new Translation("окружать")).isEqualTo(new Translation("окружать"));
		assertThat(new Translation("окружать")).hasSameHashCodeAs(new Translation("окружать"));
	}

}
