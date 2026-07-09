package io.github.ifamneme.mneme_core.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
class ReviewEntity {

	@Id
	private UUID cardId;

	private UUID ownerId;

	private int box;

	private Instant dueAt;

	protected ReviewEntity() {
	}

	ReviewEntity(UUID cardId, UUID ownerId, int box, Instant dueAt) {
		this.cardId = cardId;
		this.ownerId = ownerId;
		this.box = box;
		this.dueAt = dueAt;
	}

	UUID getCardId() {
		return cardId;
	}

	UUID getOwnerId() {
		return ownerId;
	}

	int getBox() {
		return box;
	}

	Instant getDueAt() {
		return dueAt;
	}

}
