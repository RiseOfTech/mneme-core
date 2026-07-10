package io.github.ifamneme.mneme_core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.Image;
import io.github.ifamneme.mneme_core.domain.LeitnerBox;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;
import io.github.ifamneme.mneme_core.domain.ReviewState;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

@ExtendWith(MockitoExtension.class)
class DefaultGetReviewSessionUseCaseTest {

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private CardRepository cardRepository;

	private final Instant now = Instant.parse("2026-07-09T00:00:00Z");
	private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

	private DefaultGetReviewSessionUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = new DefaultGetReviewSessionUseCase(reviewRepository, cardRepository, clock);
	}

	@Test
	void zipsReviewsWithCardsPreservingDueAtOrderEvenWhenCardRepositoryReturnsDifferentOrder() {
		UserId owner = new UserId(UUID.randomUUID());

		CardId firstCardId = CardId.newId();
		Card firstCard = Card.reconstitute(firstCardId, owner, new Word("surround"), new Translation("окружать"),
				new Association("сир, нас окружают"), new Image("https://example.com/first.png"),
				Visibility.PRIVATE);
		Review firstReview = new Review(firstCardId, owner, new ReviewState(LeitnerBox.first(), now.minus(Duration.ofDays(2))));

		CardId secondCardId = CardId.newId();
		Card secondCard = Card.reconstitute(secondCardId, owner, new Word("mnemonic"), new Translation("мнемоника"),
				new Association("association"), null, Visibility.PRIVATE);
		Review secondReview = new Review(secondCardId, owner, new ReviewState(new LeitnerBox(2), now.minus(Duration.ofDays(1))));

		when(reviewRepository.findDue(owner, now)).thenReturn(List.of(firstReview, secondReview));
		// Card repository returns cards in the opposite order of the reviews.
		when(cardRepository.findAllByIdsAndOwner(List.of(firstCardId, secondCardId), owner))
				.thenReturn(List.of(secondCard, firstCard));

		List<ReviewSessionView> views = useCase.get(owner);

		assertThat(views).hasSize(2);
		assertThat(views.get(0).cardId()).isEqualTo(firstCardId);
		assertThat(views.get(0).word()).isEqualTo(firstCard.word());
		assertThat(views.get(0).translation()).isEqualTo(firstCard.translation());
		assertThat(views.get(0).association()).isEqualTo(firstCard.association());
		assertThat(views.get(0).image()).contains(new Image("https://example.com/first.png"));
		assertThat(views.get(0).box()).isEqualTo(firstReview.state().box());
		assertThat(views.get(0).dueAt()).isEqualTo(firstReview.state().dueAt());

		assertThat(views.get(1).cardId()).isEqualTo(secondCardId);
		assertThat(views.get(1).image()).isEmpty();
		assertThat(views.get(1).box()).isEqualTo(secondReview.state().box());
		assertThat(views.get(1).dueAt()).isEqualTo(secondReview.state().dueAt());
	}

	@Test
	void emptyDueList_returnsEmptyListAndCardRepositoryReceivesZeroInteractions() {
		UserId owner = new UserId(UUID.randomUUID());
		when(reviewRepository.findDue(owner, now)).thenReturn(List.of());

		List<ReviewSessionView> views = useCase.get(owner);

		assertThat(views).isEmpty();
		verifyNoInteractions(cardRepository);
	}

	@Test
	void whenReviewsCardIsMissing_throwsReviewSessionIntegrityException() {
		UserId owner = new UserId(UUID.randomUUID());
		CardId cardId = CardId.newId();
		Review review = new Review(cardId, owner, new ReviewState(LeitnerBox.first(), now.minus(Duration.ofDays(1))));

		when(reviewRepository.findDue(owner, now)).thenReturn(List.of(review));
		when(cardRepository.findAllByIdsAndOwner(List.of(cardId), owner)).thenReturn(List.of());

		assertThatThrownBy(() -> useCase.get(owner)).isInstanceOf(ReviewSessionIntegrityException.class);
	}

}
