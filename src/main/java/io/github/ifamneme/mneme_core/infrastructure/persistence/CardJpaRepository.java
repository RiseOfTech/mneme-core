package io.github.ifamneme.mneme_core.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CardJpaRepository extends JpaRepository<CardEntity, UUID> {

	Optional<CardEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

}
