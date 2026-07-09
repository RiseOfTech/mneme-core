package io.github.ifamneme.mneme_core.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cards")
class CardEntity {

	@Id
	private UUID id;

	private UUID ownerId;

	private String word;

	private String translation;

	private String association;

	private String imageUrl;

	@Enumerated(EnumType.STRING)
	private CardVisibility visibility;

	@CreationTimestamp
	private Instant createdAt;

	@UpdateTimestamp
	private Instant updatedAt;

	protected CardEntity() {
	}

	CardEntity(UUID id, UUID ownerId, String word, String translation, String association, String imageUrl,
			CardVisibility visibility) {
		this.id = id;
		this.ownerId = ownerId;
		this.word = word;
		this.translation = translation;
		this.association = association;
		this.imageUrl = imageUrl;
		this.visibility = visibility;
	}

	UUID getId() {
		return id;
	}

	UUID getOwnerId() {
		return ownerId;
	}

	String getWord() {
		return word;
	}

	String getTranslation() {
		return translation;
	}

	String getAssociation() {
		return association;
	}

	String getImageUrl() {
		return imageUrl;
	}

	CardVisibility getVisibility() {
		return visibility;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

}
