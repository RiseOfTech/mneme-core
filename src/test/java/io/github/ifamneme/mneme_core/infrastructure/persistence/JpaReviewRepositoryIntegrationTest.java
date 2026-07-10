package io.github.ifamneme.mneme_core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.ifamneme.mneme_core.AbstractIntegrationTest;
import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.LeitnerBox;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;
import io.github.ifamneme.mneme_core.domain.ReviewState;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

class JpaReviewRepositoryIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@Test
	void savesAndLoadsReviewRoundTrip() {
		UserId owner = new UserId(UUID.randomUUID());
		Card card = cardRepository.save(new Card(owner, new Word("word"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		Instant dueAt = Instant.parse("2026-07-10T00:00:00Z");
		Review review = new Review(card.id(), owner, new ReviewState(new LeitnerBox(1), dueAt));

		reviewRepository.save(review);

		Review loaded = reviewRepository.findByCardIdAndOwner(card.id(), owner).orElseThrow();
		assertThat(loaded.cardId()).isEqualTo(card.id());
		assertThat(loaded.owner()).isEqualTo(owner);
		assertThat(loaded.state().box().number()).isEqualTo(1);
		assertThat(loaded.state().dueAt()).isEqualTo(dueAt);
	}

	@Test
	void findByCardIdAndOwnerReturnsEmptyForAnotherOwner() {
		UserId owner = new UserId(UUID.randomUUID());
		UserId otherOwner = new UserId(UUID.randomUUID());
		Card card = cardRepository.save(new Card(owner, new Word("word"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		Review review = new Review(card.id(), owner, new ReviewState(new LeitnerBox(1), Instant.parse("2026-07-10T00:00:00Z")));
		reviewRepository.save(review);

		assertThat(reviewRepository.findByCardIdAndOwner(card.id(), otherOwner)).isEmpty();
	}

}
