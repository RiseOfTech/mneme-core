package io.github.ifamneme.mneme_core.application;

import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.UserId;

public interface GetCardUseCase {

	Card get(UserId owner, CardId id);

}
