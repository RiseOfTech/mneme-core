package io.github.ifamneme.mneme_core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.ifamneme.mneme_core.AbstractIntegrationTest;
import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.Image;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

class JpaCardRepositoryIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private CardRepository cardRepository;

	@Test
	void savesAndLoadsCardRoundTripWithImage() {
		UserId owner = new UserId(UUID.randomUUID());
		Card card = new Card(owner, new Word("surround"), new Translation("окружать"),
				new Association("сир, нас окружают"), new Image("https://example.com/card.png"), Visibility.PUBLIC);

		Card saved = cardRepository.save(card);

		Card loaded = cardRepository.findByIdAndOwner(saved.id(), owner).orElseThrow();
		assertThat(loaded.id()).isEqualTo(saved.id());
		assertThat(loaded.owner()).isEqualTo(owner);
		assertThat(loaded.word()).isEqualTo(new Word("surround"));
		assertThat(loaded.translation()).isEqualTo(new Translation("окружать"));
		assertThat(loaded.association()).isEqualTo(new Association("сир, нас окружают"));
		assertThat(loaded.image()).contains(new Image("https://example.com/card.png"));
		assertThat(loaded.visibility()).isEqualTo(Visibility.PUBLIC);
	}

	@Test
	void savesAndLoadsCardRoundTripWithoutImage() {
		UserId owner = new UserId(UUID.randomUUID());
		Card card = new Card(owner, new Word("word"), new Translation("translation"), new Association("association"),
				null, Visibility.PRIVATE);

		Card saved = cardRepository.save(card);

		Card loaded = cardRepository.findByIdAndOwner(saved.id(), owner).orElseThrow();
		assertThat(loaded.image()).isEmpty();
	}

	@Test
	void findByIdAndOwnerReturnsEmptyForAnotherOwner() {
		UserId owner = new UserId(UUID.randomUUID());
		UserId otherOwner = new UserId(UUID.randomUUID());
		Card card = new Card(owner, new Word("word"), new Translation("translation"), new Association("association"),
				null, Visibility.PRIVATE);

		Card saved = cardRepository.save(card);

		assertThat(cardRepository.findByIdAndOwner(saved.id(), otherOwner)).isEmpty();
	}

	@Test
	void findByIdAndOwnerReturnsEmptyForUnknownId() {
		UserId owner = new UserId(UUID.randomUUID());
		Card card = new Card(owner, new Word("word"), new Translation("translation"), new Association("association"),
				null, Visibility.PRIVATE);
		cardRepository.save(card);

		assertThat(cardRepository.findByIdAndOwner(CardId.newId(), owner)).isEmpty();
	}

	@Test
	void findAllByIdsAndOwnerReturnsMatchingCards() {
		UserId owner = new UserId(UUID.randomUUID());
		Card first = cardRepository.save(new Card(owner, new Word("first"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		Card second = cardRepository.save(new Card(owner, new Word("second"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));

		List<Card> found = cardRepository.findAllByIdsAndOwner(List.of(first.id(), second.id()), owner);

		assertThat(found).extracting(Card::id).containsExactlyInAnyOrder(first.id(), second.id());
	}

	@Test
	void findAllByIdsAndOwnerExcludesAnotherOwnersCards() {
		UserId owner = new UserId(UUID.randomUUID());
		UserId otherOwner = new UserId(UUID.randomUUID());
		Card ownCard = cardRepository.save(new Card(owner, new Word("own"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		Card otherCard = cardRepository.save(new Card(otherOwner, new Word("other"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));

		List<Card> found = cardRepository.findAllByIdsAndOwner(List.of(ownCard.id(), otherCard.id()), owner);

		assertThat(found).extracting(Card::id).containsExactly(ownCard.id());
	}

}
