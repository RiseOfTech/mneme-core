package io.github.ifamneme.mneme_core.domain;

import java.util.Optional;

public interface ReviewRepository {

	Review save(Review review);

	Optional<Review> findByCardId(CardId cardId);

}
