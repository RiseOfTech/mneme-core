package io.github.ifamneme.mneme_core.domain;

import java.util.Objects;
import java.util.Optional;

public final class Card {

	private final CardId id;
	private final UserId owner;
	private final Word word;
	private final Translation translation;
	private final Association association;
	private final Image image;
	private final Visibility visibility;

	public Card(UserId owner, Word word, Translation translation, Association association, Image image,
			Visibility visibility) {
		this(CardId.newId(), owner, word, translation, association, image, visibility);
	}

	private Card(CardId id, UserId owner, Word word, Translation translation, Association association, Image image,
			Visibility visibility) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.owner = Objects.requireNonNull(owner, "owner must not be null");
		this.word = Objects.requireNonNull(word, "word must not be null");
		this.translation = Objects.requireNonNull(translation, "translation must not be null");
		this.association = Objects.requireNonNull(association, "association must not be null");
		this.image = image;
		this.visibility = Objects.requireNonNull(visibility, "visibility must not be null");
	}

	public static Card reconstitute(CardId id, UserId owner, Word word, Translation translation,
			Association association, Image image, Visibility visibility) {
		return new Card(id, owner, word, translation, association, image, visibility);
	}

	public Card update(Word word, Translation translation, Association association, Image image,
			Visibility visibility) {
		return new Card(this.id, this.owner, word, translation, association, image, visibility);
	}

	public CardId id() {
		return id;
	}

	public UserId owner() {
		return owner;
	}

	public Word word() {
		return word;
	}

	public Translation translation() {
		return translation;
	}

	public Association association() {
		return association;
	}

	public Optional<Image> image() {
		return Optional.ofNullable(image);
	}

	public Visibility visibility() {
		return visibility;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Card other)) {
			return false;
		}
		return id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "Card{id=" + id + "}";
	}

}
