package io.github.ifamneme.mneme_core.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface CardJpaRepository extends JpaRepository<CardEntity, UUID> {

	Optional<CardEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

	List<CardEntity> findByIdInAndOwnerId(Collection<UUID> ids, UUID ownerId);

	List<CardEntity> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

	@Transactional
	void deleteByIdAndOwnerId(UUID id, UUID ownerId);

}
