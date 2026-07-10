package io.github.ifamneme.mneme_core.application;

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.UserId;

public interface DeleteCardUseCase {

	void delete(UserId owner, CardId cardId);

}
