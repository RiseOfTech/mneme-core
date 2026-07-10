package io.github.ifamneme.mneme_core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

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

	@Test
	void findDueReturnsOnlyReviewsDueNowOrEarlier() {
		UserId owner = new UserId(UUID.randomUUID());
		Instant now = Instant.parse("2026-07-10T00:00:00Z");

		Card dueCard = cardRepository.save(new Card(owner, new Word("due"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		reviewRepository.save(new Review(dueCard.id(), owner, new ReviewState(new LeitnerBox(1), now.minus(Duration.ofDays(1)))));

		Card futureCard = cardRepository.save(new Card(owner, new Word("future"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		reviewRepository.save(new Review(futureCard.id(), owner, new ReviewState(new LeitnerBox(1), now.plus(Duration.ofDays(1)))));

		List<Review> due = reviewRepository.findDue(owner, now);

		assertThat(due).extracting(Review::cardId).containsExactly(dueCard.id());
	}

	@Test
	void findDueOrdersByDueAtAscending() {
		UserId owner = new UserId(UUID.randomUUID());
		Instant now = Instant.parse("2026-07-10T00:00:00Z");

		Card latestCard = cardRepository.save(new Card(owner, new Word("latest"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		reviewRepository.save(new Review(latestCard.id(), owner, new ReviewState(new LeitnerBox(1), now.minus(Duration.ofHours(1)))));

		Card earliestCard = cardRepository.save(new Card(owner, new Word("earliest"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		reviewRepository.save(new Review(earliestCard.id(), owner, new ReviewState(new LeitnerBox(1), now.minus(Duration.ofDays(3)))));

		Card middleCard = cardRepository.save(new Card(owner, new Word("middle"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		reviewRepository.save(new Review(middleCard.id(), owner, new ReviewState(new LeitnerBox(1), now.minus(Duration.ofDays(1)))));

		List<Review> due = reviewRepository.findDue(owner, now);

		assertThat(due).extracting(Review::cardId).containsExactly(earliestCard.id(), middleCard.id(), latestCard.id());
	}

	@Test
	void findDueExcludesAnotherOwnersReviews() {
		UserId owner = new UserId(UUID.randomUUID());
		UserId otherOwner = new UserId(UUID.randomUUID());
		Instant now = Instant.parse("2026-07-10T00:00:00Z");

		Card otherCard = cardRepository.save(new Card(otherOwner, new Word("word"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		reviewRepository.save(new Review(otherCard.id(), otherOwner, new ReviewState(new LeitnerBox(1), now.minus(Duration.ofDays(1)))));

		assertThat(reviewRepository.findDue(owner, now)).isEmpty();
	}

	@Test
	void deleteByCardIdAndOwnerRemovesReview() {
		UserId owner = new UserId(UUID.randomUUID());
		Card card = cardRepository.save(new Card(owner, new Word("word"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		reviewRepository
			.save(new Review(card.id(), owner, new ReviewState(new LeitnerBox(1), Instant.parse("2026-07-10T00:00:00Z"))));

		reviewRepository.deleteByCardIdAndOwner(card.id(), owner);

		assertThat(reviewRepository.findByCardIdAndOwner(card.id(), owner)).isEmpty();
	}

	@Test
	void deleteByCardIdAndOwnerIsNoOpWhenReviewDoesNotExist() {
		UserId owner = new UserId(UUID.randomUUID());
		Card card = cardRepository.save(new Card(owner, new Word("word"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));

		reviewRepository.deleteByCardIdAndOwner(card.id(), owner);
	}

	@Test
	void deletingCardWhileReviewStillExistsViolatesForeignKeyConstraint() {
		UserId owner = new UserId(UUID.randomUUID());
		Card card = cardRepository.save(new Card(owner, new Word("word"), new Translation("translation"),
				new Association("association"), null, Visibility.PRIVATE));
		reviewRepository
			.save(new Review(card.id(), owner, new ReviewState(new LeitnerBox(1), Instant.parse("2026-07-10T00:00:00Z"))));

		assertThatThrownBy(() -> cardRepository.deleteByIdAndOwner(card.id(), owner)).isInstanceOf(DataAccessException.class);
	}

}
