package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ifamneme.mneme_core.application.CreateCardCommand;
import io.github.ifamneme.mneme_core.application.CreateCardUseCase;
import io.github.ifamneme.mneme_core.application.DeleteCardUseCase;
import io.github.ifamneme.mneme_core.application.GetCardUseCase;
import io.github.ifamneme.mneme_core.application.ListCardsUseCase;
import io.github.ifamneme.mneme_core.application.UpdateCardCommand;
import io.github.ifamneme.mneme_core.application.UpdateCardUseCase;
import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Image;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

@RestController
@RequestMapping("/v1/cards")
public class CardController implements CardApi {

	private final CreateCardUseCase createCardUseCase;
	private final GetCardUseCase getCardUseCase;
	private final ListCardsUseCase listCardsUseCase;
	private final UpdateCardUseCase updateCardUseCase;
	private final DeleteCardUseCase deleteCardUseCase;

	public CardController(CreateCardUseCase createCardUseCase, GetCardUseCase getCardUseCase,
			ListCardsUseCase listCardsUseCase, UpdateCardUseCase updateCardUseCase,
			DeleteCardUseCase deleteCardUseCase) {
		this.createCardUseCase = createCardUseCase;
		this.getCardUseCase = getCardUseCase;
		this.listCardsUseCase = listCardsUseCase;
		this.updateCardUseCase = updateCardUseCase;
		this.deleteCardUseCase = deleteCardUseCase;
	}

	@Override
	public ResponseEntity<CardResponseDto> createCard(CreateCardRequestDto request, UserId userId) {
		Image image = request.imageUrl() == null ? null : new Image(request.imageUrl());
		Visibility visibility = request.visibility() == null ? null : request.visibility().toDomain();
		CreateCardCommand command = new CreateCardCommand(userId, new Word(request.word()),
				new Translation(request.translation()), new Association(request.association()), image, visibility);
		Card card = createCardUseCase.create(command);
		return ResponseEntity.status(HttpStatus.CREATED).body(CardResponseDto.from(card));
	}

	@Override
	public CardResponseDto getCard(UUID id, UserId userId) {
		Card card = getCardUseCase.get(userId, CardId.of(id));
		return CardResponseDto.from(card);
	}

	@Override
	public List<CardResponseDto> listCards(UserId userId) {
		return listCardsUseCase.list(userId).stream().map(CardResponseDto::from).toList();
	}

	@Override
	public CardResponseDto updateCard(UUID id, UpdateCardRequestDto request, UserId userId) {
		Image image = request.imageUrl() == null ? null : new Image(request.imageUrl());
		Visibility visibility = request.visibility() == null ? null : request.visibility().toDomain();
		UpdateCardCommand command = new UpdateCardCommand(userId, CardId.of(id), new Word(request.word()),
				new Translation(request.translation()), new Association(request.association()), image, visibility);
		Card card = updateCardUseCase.update(command);
		return CardResponseDto.from(card);
	}

	@Override
	public ResponseEntity<Void> deleteCard(UUID id, UserId userId) {
		deleteCardUseCase.delete(userId, CardId.of(id));
		return ResponseEntity.noContent().build();
	}

}
