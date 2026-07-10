package io.github.ifamneme.mneme_core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.Image;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

@ExtendWith(MockitoExtension.class)
class DefaultUpdateCardUseCaseTest {

	@Mock
	private CardRepository cardRepository;

	private DefaultUpdateCardUseCase useCase;

	@BeforeEach
	void setUp() {
		useCase = new DefaultUpdateCardUseCase(cardRepository);
	}

	@Test
	void loadsUpdatesAndSavesCard() {
		UserId owner = new UserId(UUID.randomUUID());
		Card existing = new Card(owner, new Word("old-word"), new Translation("old-translation"),
				new Association("old-association"), null, Visibility.PRIVATE);
		when(cardRepository.findByIdAndOwner(existing.id(), owner)).thenReturn(Optional.of(existing));
		when(cardRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Image newImage = new Image("https://example.com/new.png");
		UpdateCardCommand command = new UpdateCardCommand(owner, existing.id(), new Word("new-word"),
				new Translation("new-translation"), new Association("new-association"), newImage, Visibility.PUBLIC);

		Card result = useCase.update(command);

		ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
		verify(cardRepository).save(captor.capture());
		Card saved = captor.getValue();
		assertThat(saved.id()).isEqualTo(existing.id());
		assertThat(saved.owner()).isEqualTo(owner);
		assertThat(saved.word()).isEqualTo(new Word("new-word"));
		assertThat(saved.translation()).isEqualTo(new Translation("new-translation"));
		assertThat(saved.association()).isEqualTo(new Association("new-association"));
		assertThat(saved.image()).contains(newImage);
		assertThat(saved.visibility()).isEqualTo(Visibility.PUBLIC);

		assertThat(result).isSameAs(saved);
	}

	@Test
	void throwsNotFoundWhenRepositoryReturnsEmptyAndNeverSaves() {
		UserId owner = new UserId(UUID.randomUUID());
		CardId cardId = CardId.newId();
		when(cardRepository.findByIdAndOwner(cardId, owner)).thenReturn(Optional.empty());

		UpdateCardCommand command = new UpdateCardCommand(owner, cardId, new Word("word"),
				new Translation("translation"), new Association("association"), null, Visibility.PRIVATE);

		assertThatThrownBy(() -> useCase.update(command)).isInstanceOf(CardNotFoundException.class);
		verify(cardRepository, never()).save(any());
	}

}
