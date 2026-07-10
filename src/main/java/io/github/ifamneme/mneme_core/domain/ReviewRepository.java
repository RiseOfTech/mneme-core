package io.github.ifamneme.mneme_core.domain;

import java.util.Optional;

public interface ReviewRepository {

	Review save(Review review);

	Optional<Review> findByCardIdAndOwner(CardId cardId, UserId owner);

}
