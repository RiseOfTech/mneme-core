package io.github.ifamneme.mneme_core.application;

import io.github.ifamneme.mneme_core.domain.Card;

public interface UpdateCardUseCase {

	Card update(UpdateCardCommand command);

}
