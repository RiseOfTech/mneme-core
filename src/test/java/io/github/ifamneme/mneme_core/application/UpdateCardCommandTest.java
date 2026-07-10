package io.github.ifamneme.mneme_core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

class UpdateCardCommandTest {

	private final UserId owner = new UserId(UUID.randomUUID());
	private final CardId cardId = CardId.newId();
	private final Word word = new Word("word");
	private final Translation translation = new Translation("translation");
	private final Association association = new Association("association");

	@Test
	void rejectsNullOwner() {
		assertThatThrownBy(() -> new UpdateCardCommand(null, cardId, word, translation, association, null,
				Visibility.PRIVATE)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsNullCardId() {
		assertThatThrownBy(() -> new UpdateCardCommand(owner, null, word, translation, association, null,
				Visibility.PRIVATE)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsNullWord() {
		assertThatThrownBy(() -> new UpdateCardCommand(owner, cardId, null, translation, association, null,
				Visibility.PRIVATE)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsNullTranslation() {
		assertThatThrownBy(() -> new UpdateCardCommand(owner, cardId, word, null, association, null,
				Visibility.PRIVATE)).isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsNullAssociation() {
		assertThatThrownBy(
				() -> new UpdateCardCommand(owner, cardId, word, translation, null, null, Visibility.PRIVATE))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void nullVisibilityNormalisesToPrivate() {
		UpdateCardCommand command = new UpdateCardCommand(owner, cardId, word, translation, association, null, null);
		assertThat(command.visibility()).isEqualTo(Visibility.PRIVATE);
	}

	@Test
	void nonNullVisibilityPassesThroughUnchanged() {
		UpdateCardCommand command = new UpdateCardCommand(owner, cardId, word, translation, association, null,
				Visibility.PUBLIC);
		assertThat(command.visibility()).isEqualTo(Visibility.PUBLIC);
	}

	@Test
	void nullImagePassesThroughUnchanged() {
		UpdateCardCommand command = new UpdateCardCommand(owner, cardId, word, translation, association, null,
				Visibility.PRIVATE);
		assertThat(command.image()).isNull();
	}

}
