package io.github.ifamneme.mneme_core.infrastructure.persistence;

import io.github.ifamneme.mneme_core.domain.Review;

interface ReviewMapper {

	ReviewEntity toEntity(Review review);

	Review toDomain(ReviewEntity entity);

}
