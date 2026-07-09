package io.github.ifamneme.mneme_core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.LeitnerBox;
import io.github.ifamneme.mneme_core.domain.LeitnerStrategy;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;
import io.github.ifamneme.mneme_core.domain.ReviewState;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

@ExtendWith(MockitoExtension.class)
class DefaultCreateCardUseCaseTest {

	@Mock
	private CardRepository cardRepository;

	@Mock
	private ReviewRepository reviewRepository;

	@Mock
	private LeitnerStrategy leitnerStrategy;

	private final Instant now = Instant.parse("2026-07-09T00:00:00Z");
	private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

	private DefaultCreateCardUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = new DefaultCreateCardUseCase(cardRepository, reviewRepository, leitnerStrategy, clock);
	}

	@Test
	void savesCardAndInitialReview() {
		UserId owner = new UserId(UUID.randomUUID());
		CreateCardCommand command = new CreateCardCommand(owner, new Word("word"), new Translation("translation"),
				new Association("association"), null, Visibility.PUBLIC);
		ReviewState initialState = new ReviewState(LeitnerBox.first(), now.plus(Duration.ofDays(1)));

		when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(leitnerStrategy.initialState(now)).thenReturn(initialState);

		Card result = useCase.create(command);

		ArgumentCaptor<Card> cardCaptor = ArgumentCaptor.forClass(Card.class);
		verify(cardRepository).save(cardCaptor.capture());
		Card savedCard = cardCaptor.getValue();
		assertThat(savedCard.owner()).isEqualTo(owner);
		assertThat(savedCard.word()).isEqualTo(new Word("word"));
		assertThat(savedCard.translation()).isEqualTo(new Translation("translation"));
		assertThat(savedCard.association()).isEqualTo(new Association("association"));
		assertThat(savedCard.visibility()).isEqualTo(Visibility.PUBLIC);

		verify(leitnerStrategy).initialState(now);

		ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
		verify(reviewRepository).save(reviewCaptor.capture());
		Review savedReview = reviewCaptor.getValue();
		assertThat(savedReview.cardId()).isEqualTo(savedCard.id());
		assertThat(savedReview.owner()).isEqualTo(owner);
		assertThat(savedReview.state()).isEqualTo(initialState);

		assertThat(result).isEqualTo(savedCard);
	}

}
