package io.github.ifamneme.mneme_core.infrastructure.persistence;

import io.github.ifamneme.mneme_core.domain.Card;

interface CardMapper {

	CardEntity toEntity(Card card);

	Card toDomain(CardEntity entity);

}
