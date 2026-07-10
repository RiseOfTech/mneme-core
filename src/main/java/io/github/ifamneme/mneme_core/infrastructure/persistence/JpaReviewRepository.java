package io.github.ifamneme.mneme_core.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;
import io.github.ifamneme.mneme_core.domain.UserId;

@Repository
class JpaReviewRepository implements ReviewRepository {

	private final ReviewJpaRepository jpaRepository;
	private final ReviewMapper mapper;

	JpaReviewRepository(ReviewJpaRepository jpaRepository, ReviewMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Review save(Review review) {
		ReviewEntity saved = jpaRepository.save(mapper.toEntity(review));
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Review> findByCardIdAndOwner(CardId cardId, UserId owner) {
		return jpaRepository.findByCardIdAndOwnerId(cardId.value(), owner.value()).map(mapper::toDomain);
	}

	@Override
	public List<Review> findDue(UserId owner, Instant now) {
		return jpaRepository.findByOwnerIdAndDueAtLessThanEqualOrderByDueAtAsc(owner.value(), now).stream()
				.map(mapper::toDomain).toList();
	}

}
