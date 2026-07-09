package io.github.ifamneme.mneme_core.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;

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
	public Optional<Review> findByCardId(CardId cardId) {
		return jpaRepository.findByCardId(cardId.value()).map(mapper::toDomain);
	}

}
