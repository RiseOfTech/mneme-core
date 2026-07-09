package io.github.ifamneme.mneme_core.infrastructure.persistence;

import org.springframework.stereotype.Component;

import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Image;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

@Component
class DefaultCardMapper implements CardMapper {

	@Override
	public CardEntity toEntity(Card card) {
		String imageUrl = card.image().map(Image::url).orElse(null);
		return new CardEntity(card.id().value(), card.owner().value(), card.word().value(),
				card.translation().value(), card.association().value(), imageUrl, toEntityVisibility(card.visibility()));
	}

	@Override
	public Card toDomain(CardEntity entity) {
		Image image = entity.getImageUrl() == null ? null : new Image(entity.getImageUrl());
		return Card.reconstitute(CardId.of(entity.getId()), new UserId(entity.getOwnerId()),
				new Word(entity.getWord()), new Translation(entity.getTranslation()),
				new Association(entity.getAssociation()), image, toDomainVisibility(entity.getVisibility()));
	}

	private CardVisibility toEntityVisibility(Visibility visibility) {
		return CardVisibility.valueOf(visibility.name());
	}

	private Visibility toDomainVisibility(CardVisibility visibility) {
		return Visibility.valueOf(visibility.name());
	}

}
