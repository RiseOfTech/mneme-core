package io.github.ifamneme.mneme_core.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

	Review save(Review review);

	Optional<Review> findByCardIdAndOwner(CardId cardId, UserId owner);

	List<Review> findDue(UserId owner, Instant now);

	void deleteByCardIdAndOwner(CardId cardId, UserId owner);

}
