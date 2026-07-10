package io.github.ifamneme.mneme_core.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import io.github.ifamneme.mneme_core.application.ReviewSessionView;
import io.github.ifamneme.mneme_core.domain.Image;

public record SessionCardDto(UUID cardId, String word, String translation, String association, String imageUrl,
		int box, Instant dueAt) {

	public static SessionCardDto from(ReviewSessionView view) {
		String imageUrl = view.image().map(Image::url).orElse(null);
		return new SessionCardDto(view.cardId().value(), view.word().value(), view.translation().value(),
				view.association().value(), imageUrl, view.box().number(), view.dueAt());
	}

}
