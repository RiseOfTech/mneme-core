package io.github.ifamneme.mneme_core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CardTest {

	private final UserId owner = new UserId(UUID.randomUUID());
	private final Word word = new Word("surround");
	private final Translation translation = new Translation("окружать");
	private final Association association = new Association("сир, нас окружают");

	@Test
	void rejectsNullOwner() {
		assertThatThrownBy(() -> new Card(null, word, translation, association, null, Visibility.PRIVATE))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsNullWord() {
		assertThatThrownBy(() -> new Card(owner, null, translation, association, null, Visibility.PRIVATE))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsNullTranslation() {
		assertThatThrownBy(() -> new Card(owner, word, null, association, null, Visibility.PRIVATE))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsNullAssociation() {
		assertThatThrownBy(() -> new Card(owner, word, translation, null, null, Visibility.PRIVATE))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsNullVisibility() {
		assertThatThrownBy(() -> new Card(owner, word, translation, association, null, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void roundTripsAllFieldsAndGeneratesId() {
		Card card = new Card(owner, word, translation, association, null, Visibility.PRIVATE);

		assertThat(card.id()).isNotNull();
		assertThat(card.owner()).isEqualTo(owner);
		assertThat(card.word()).isEqualTo(word);
		assertThat(card.translation()).isEqualTo(translation);
		assertThat(card.association()).isEqualTo(association);
		assertThat(card.visibility()).isEqualTo(Visibility.PRIVATE);
	}

	@Test
	void imageIsEmptyWhenNotProvided() {
		Card card = new Card(owner, word, translation, association, null, Visibility.PRIVATE);
		assertThat(card.image()).isEmpty();
	}

	@Test
	void imageIsPresentWhenProvided() {
		Image image = new Image("https://example.com/card.png");
		Card card = new Card(owner, word, translation, association, image, Visibility.PRIVATE);
		assertThat(card.image()).contains(image);
	}

}
