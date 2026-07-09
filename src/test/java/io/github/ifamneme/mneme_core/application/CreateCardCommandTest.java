package io.github.ifamneme.mneme_core.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

class CreateCardCommandTest {

	private final UserId owner = new UserId(UUID.randomUUID());
	private final Word word = new Word("word");
	private final Translation translation = new Translation("translation");
	private final Association association = new Association("association");

	@Test
	void nullVisibilityNormalisesToPrivate() {
		CreateCardCommand command = new CreateCardCommand(owner, word, translation, association, null, null);
		assertThat(command.visibility()).isEqualTo(Visibility.PRIVATE);
	}

	@Test
	void nonNullVisibilityPassesThroughUnchanged() {
		CreateCardCommand command = new CreateCardCommand(owner, word, translation, association, null,
				Visibility.PUBLIC);
		assertThat(command.visibility()).isEqualTo(Visibility.PUBLIC);
	}

}
