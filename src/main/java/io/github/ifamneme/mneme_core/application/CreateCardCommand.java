package io.github.ifamneme.mneme_core.application;

import java.util.Objects;

import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Image;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

public record CreateCardCommand(UserId owner, Word word, Translation translation, Association association,
		Image image, Visibility visibility) {

	public CreateCardCommand {
		Objects.requireNonNull(owner, "owner must not be null");
		Objects.requireNonNull(word, "word must not be null");
		Objects.requireNonNull(translation, "translation must not be null");
		Objects.requireNonNull(association, "association must not be null");
		if (visibility == null) {
			visibility = Visibility.PRIVATE;
		}
	}

}
