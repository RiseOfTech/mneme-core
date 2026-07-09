package io.github.ifamneme.mneme_core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AssociationTest {

	@Test
	void rejectsNullValue() {
		assertThatThrownBy(() -> new Association(null)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsBlankValue() {
		assertThatThrownBy(() -> new Association("   ")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void roundTripsValidValue() {
		Association association = new Association("сир, нас окружают");
		assertThat(association.value()).isEqualTo("сир, нас окружают");
	}

	@Test
	void equalsAndHashCodeByValue() {
		assertThat(new Association("x")).isEqualTo(new Association("x"));
		assertThat(new Association("x")).hasSameHashCodeAs(new Association("x"));
	}

}
