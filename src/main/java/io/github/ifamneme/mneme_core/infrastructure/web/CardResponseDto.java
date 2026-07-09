package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.UUID;

import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.Image;
import io.github.ifamneme.mneme_core.domain.Visibility;

public record CardResponseDto(UUID id, String word, String translation, String association, String imageUrl,
		Visibility visibility) {

	public static CardResponseDto from(Card card) {
		String imageUrl = card.image().map(Image::url).orElse(null);
		return new CardResponseDto(card.id().value(), card.word().value(), card.translation().value(),
				card.association().value(), imageUrl, card.visibility());
	}

}
