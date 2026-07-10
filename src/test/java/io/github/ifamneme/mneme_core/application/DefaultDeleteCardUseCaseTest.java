package io.github.ifamneme.mneme_core.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

@ExtendWith(MockitoExtension.class)
class DefaultDeleteCardUseCaseTest {

	@Mock
	private CardRepository cardRepository;

	@Mock
	private ReviewRepository reviewRepository;

	private DefaultDeleteCardUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = new DefaultDeleteCardUseCase(cardRepository, reviewRepository);
	}

	@Test
	void deletesReviewBeforeCard() {
		UserId owner = new UserId(UUID.randomUUID());
		Card card = new Card(owner, new Word("word"), new Translation("translation"), new Association("association"),
				null, Visibility.PRIVATE);
		when(cardRepository.findByIdAndOwner(card.id(), owner)).thenReturn(Optional.of(card));

		useCase.delete(owner, card.id());

		InOrder order = inOrder(cardRepository, reviewRepository);
		order.verify(reviewRepository).deleteByCardIdAndOwner(card.id(), owner);
		order.verify(cardRepository).deleteByIdAndOwner(card.id(), owner);
	}

	@Test
	void throwsNotFoundWhenRepositoryReturnsEmptyAndDeletesNothing() {
		UserId owner = new UserId(UUID.randomUUID());
		CardId cardId = CardId.newId();
		when(cardRepository.findByIdAndOwner(cardId, owner)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> useCase.delete(owner, cardId)).isInstanceOf(CardNotFoundException.class);

		verify(reviewRepository, never()).deleteByCardIdAndOwner(any(), any());
		verify(cardRepository, never()).deleteByIdAndOwner(any(), any());
	}

}
