package io.github.ifamneme.mneme_core.domain;

import java.util.Optional;

public interface CardRepository {

	Card save(Card card);

	Optional<Card> findByIdAndOwner(CardId id, UserId owner);

}
