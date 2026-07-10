package io.github.ifamneme.mneme_core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.Grade;
import io.github.ifamneme.mneme_core.domain.LeitnerBox;
import io.github.ifamneme.mneme_core.domain.LeitnerStrategy;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;
import io.github.ifamneme.mneme_core.domain.ReviewState;
import io.github.ifamneme.mneme_core.domain.UserId;

@ExtendWith(MockitoExtension.class)
class DefaultGradeReviewUseCaseTest {

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private LeitnerStrategy leitnerStrategy;

	@Mock
	private CardRepository cardRepository;

	private final Instant now = Instant.parse("2026-07-09T00:00:00Z");
	private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

	private DefaultGradeReviewUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = new DefaultGradeReviewUseCase(reviewRepository, leitnerStrategy, clock);
	}

	@Test
	void gradesReviewAndSavesResult() {
		UserId owner = new UserId(UUID.randomUUID());
		CardId cardId = CardId.newId();
		ReviewState currentState = new ReviewState(LeitnerBox.first(), now);
		Review review = new Review(cardId, owner, currentState);
		ReviewState nextState = new ReviewState(new LeitnerBox(2), now.plus(Duration.ofDays(3)));

		when(reviewRepository.findByCardIdAndOwner(cardId, owner)).thenReturn(Optional.of(review));
		when(leitnerStrategy.next(currentState, Grade.CORRECT, now)).thenReturn(nextState);
		when(reviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		GradeReviewCommand command = new GradeReviewCommand(owner, cardId, Grade.CORRECT);
		Review result = useCase.grade(command);

		ArgumentCaptor<Review> savedCaptor = ArgumentCaptor.forClass(Review.class);
		verify(reviewRepository).save(savedCaptor.capture());
		Review saved = savedCaptor.getValue();
		assertThat(saved.cardId()).isEqualTo(cardId);
		assertThat(saved.owner()).isEqualTo(owner);
		assertThat(saved.state()).isEqualTo(nextState);
		assertThat(result).isEqualTo(saved);

		verifyNoInteractions(cardRepository);
	}

	@Test
	void whenReviewNotFound_throwsReviewNotFoundException() {
		UserId owner = new UserId(UUID.randomUUID());
		CardId cardId = CardId.newId();
		when(reviewRepository.findByCardIdAndOwner(cardId, owner)).thenReturn(Optional.empty());

		GradeReviewCommand command = new GradeReviewCommand(owner, cardId, Grade.CORRECT);

		assertThatThrownBy(() -> useCase.grade(command)).isInstanceOf(ReviewNotFoundException.class);

		verifyNoInteractions(cardRepository);
	}

}
