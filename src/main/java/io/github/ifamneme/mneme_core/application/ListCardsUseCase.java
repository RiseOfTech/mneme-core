package io.github.ifamneme.mneme_core.application;

import java.util.List;

import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.UserId;

public interface ListCardsUseCase {

	List<Card> list(UserId owner);

}
