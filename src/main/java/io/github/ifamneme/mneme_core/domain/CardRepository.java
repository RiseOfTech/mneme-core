package io.github.ifamneme.mneme_core.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CardRepository {

	Card save(Card card);

	Optional<Card> findByIdAndOwner(CardId id, UserId owner);

	List<Card> findAllByIdsAndOwner(Collection<CardId> ids, UserId owner);

}
