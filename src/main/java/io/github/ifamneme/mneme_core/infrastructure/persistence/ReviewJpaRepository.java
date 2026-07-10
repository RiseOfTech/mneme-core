package io.github.ifamneme.mneme_core.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID> {

	Optional<ReviewEntity> findByCardIdAndOwnerId(UUID cardId, UUID ownerId);

}
