package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ifamneme.mneme_core.application.CreateCardCommand;
import io.github.ifamneme.mneme_core.application.CreateCardUseCase;
import io.github.ifamneme.mneme_core.application.GetCardUseCase;
import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Image;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Word;

@RestController
@RequestMapping("/v1/cards")
public class CardController implements CardApi {

	private final CreateCardUseCase createCardUseCase;
	private final GetCardUseCase getCardUseCase;

	public CardController(CreateCardUseCase createCardUseCase, GetCardUseCase getCardUseCase) {
		this.createCardUseCase = createCardUseCase;
		this.getCardUseCase = getCardUseCase;
	}

	@Override
	public ResponseEntity<CardResponseDto> createCard(CreateCardRequestDto request, UserId userId) {
		Image image = request.imageUrl() == null ? null : new Image(request.imageUrl());
		CreateCardCommand command = new CreateCardCommand(userId, new Word(request.word()),
				new Translation(request.translation()), new Association(request.association()), image,
				request.visibility());
		Card card = createCardUseCase.create(command);
		return ResponseEntity.status(HttpStatus.CREATED).body(CardResponseDto.from(card));
	}

	@Override
	public CardResponseDto getCard(UUID id, UserId userId) {
		Card card = getCardUseCase.get(userId, CardId.of(id));
		return CardResponseDto.from(card);
	}

}
