package io.github.ifamneme.mneme_core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

@ExtendWith(MockitoExtension.class)
class DefaultListCardsUseCaseTest {

	@Mock
	private CardRepository cardRepository;

	private DefaultListCardsUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = new DefaultListCardsUseCase(cardRepository);
	}

	@Test
	void delegatesToRepositoryAndReturnsResult() {
		UserId owner = new UserId(UUID.randomUUID());
		Card card = new Card(owner, new Word("word"), new Translation("translation"), new Association("association"),
				null, Visibility.PRIVATE);
		when(cardRepository.findAllByOwner(owner)).thenReturn(List.of(card));

		List<Card> result = useCase.list(owner);

		assertThat(result).containsExactly(card);
	}

	@Test
	void returnsEmptyListWhenOwnerHasNoCards() {
		UserId owner = new UserId(UUID.randomUUID());
		when(cardRepository.findAllByOwner(owner)).thenReturn(List.of());

		List<Card> result = useCase.list(owner);

		assertThat(result).isEmpty();
	}

}
