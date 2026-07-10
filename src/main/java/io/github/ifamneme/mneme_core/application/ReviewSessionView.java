package io.github.ifamneme.mneme_core.application;

import java.time.Instant;
import java.util.Optional;

import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Image;
import io.github.ifamneme.mneme_core.domain.LeitnerBox;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.Word;

public record ReviewSessionView(CardId cardId, Word word, Translation translation, Association association,
		Optional<Image> image, LeitnerBox box, Instant dueAt) {

}
