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

	@Test
	void updateReturnsNewInstanceWithSameIdAndOwnerAndNewContent() {
		Card card = new Card(owner, word, translation, association, null, Visibility.PRIVATE);

		Word newWord = new Word("new-word");
		Translation newTranslation = new Translation("new-translation");
		Association newAssociation = new Association("new-association");
		Image newImage = new Image("https://example.com/new.png");
		Card updated = card.update(newWord, newTranslation, newAssociation, newImage, Visibility.PUBLIC);

		assertThat(updated).isNotSameAs(card);
		assertThat(updated.id()).isEqualTo(card.id());
		assertThat(updated.owner()).isEqualTo(owner);
		assertThat(updated.word()).isEqualTo(newWord);
		assertThat(updated.translation()).isEqualTo(newTranslation);
		assertThat(updated.association()).isEqualTo(newAssociation);
		assertThat(updated.image()).contains(newImage);
		assertThat(updated.visibility()).isEqualTo(Visibility.PUBLIC);
	}

	@Test
	void updateLeavesOriginalInstanceUnchanged() {
		Card card = new Card(owner, word, translation, association, null, Visibility.PRIVATE);

		card.update(new Word("new-word"), new Translation("new-translation"), new Association("new-association"),
				new Image("https://example.com/new.png"), Visibility.PUBLIC);

		assertThat(card.word()).isEqualTo(word);
		assertThat(card.translation()).isEqualTo(translation);
		assertThat(card.association()).isEqualTo(association);
		assertThat(card.image()).isEmpty();
		assertThat(card.visibility()).isEqualTo(Visibility.PRIVATE);
	}

}
