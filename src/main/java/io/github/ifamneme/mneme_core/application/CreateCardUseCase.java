package io.github.ifamneme.mneme_core.application;

import io.github.ifamneme.mneme_core.domain.Card;

public interface CreateCardUseCase {

	Card create(CreateCardCommand command);

}
