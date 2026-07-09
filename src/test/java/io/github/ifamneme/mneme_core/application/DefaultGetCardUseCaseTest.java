package io.github.ifamneme.mneme_core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
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
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

@ExtendWith(MockitoExtension.class)
class DefaultGetCardUseCaseTest {

	@Mock
	private CardRepository cardRepository;

	private DefaultGetCardUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = new DefaultGetCardUseCase(cardRepository);
	}

	@Test
	void returnsCardForMatchingOwner() {
		UserId owner = new UserId(UUID.randomUUID());
		Card card = new Card(owner, new Word("word"), new Translation("translation"), new Association("association"),
				null, Visibility.PRIVATE);
		when(cardRepository.findByIdAndOwner(card.id(), owner)).thenReturn(Optional.of(card));

		Card result = useCase.get(owner, card.id());

		assertThat(result).isEqualTo(card);
	}

	@Test
	void throwsNotFoundWhenRepositoryReturnsEmpty() {
		UserId owner = new UserId(UUID.randomUUID());
		CardId id = CardId.newId();
		when(cardRepository.findByIdAndOwner(id, owner)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> useCase.get(owner, id)).isInstanceOf(CardNotFoundException.class);
	}

}
