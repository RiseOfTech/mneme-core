package io.github.ifamneme.mneme_core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewTest {

	@Mock
	private LeitnerStrategy leitnerStrategy;

	@Test
	void gradeReturnsNewInstanceWithStrategyProducedState() {
		CardId cardId = CardId.newId();
		UserId owner = new UserId(UUID.randomUUID());
		ReviewState currentState = new ReviewState(LeitnerBox.first(), Instant.parse("2026-07-09T00:00:00Z"));
		Review review = new Review(cardId, owner, currentState);

		Instant now = Instant.parse("2026-07-09T12:00:00Z");
		ReviewState nextState = new ReviewState(new LeitnerBox(2), Instant.parse("2026-07-12T00:00:00Z"));
		when(leitnerStrategy.next(currentState, Grade.CORRECT, now)).thenReturn(nextState);

		Review graded = review.grade(Grade.CORRECT, leitnerStrategy, now);

		verify(leitnerStrategy).next(currentState, Grade.CORRECT, now);
		assertThat(graded).isNotSameAs(review);
		assertThat(graded.cardId()).isEqualTo(cardId);
		assertThat(graded.owner()).isEqualTo(owner);
		assertThat(graded.state()).isEqualTo(nextState);

		assertThat(review.state()).isEqualTo(currentState);
	}

}
