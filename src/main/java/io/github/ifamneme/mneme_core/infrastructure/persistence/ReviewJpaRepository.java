package io.github.ifamneme.mneme_core.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID> {

	Optional<ReviewEntity> findByCardIdAndOwnerId(UUID cardId, UUID ownerId);

	List<ReviewEntity> findByOwnerIdAndDueAtLessThanEqualOrderByDueAtAsc(UUID ownerId, Instant now);

	@Transactional
	void deleteByCardIdAndOwnerId(UUID cardId, UUID ownerId);

}
