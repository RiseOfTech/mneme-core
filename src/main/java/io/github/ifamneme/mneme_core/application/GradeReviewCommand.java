package io.github.ifamneme.mneme_core.application;

import java.util.Objects;

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Grade;
import io.github.ifamneme.mneme_core.domain.UserId;

public record GradeReviewCommand(UserId owner, CardId cardId, Grade grade) {

	public GradeReviewCommand {
		Objects.requireNonNull(owner, "owner must not be null");
		Objects.requireNonNull(cardId, "cardId must not be null");
		Objects.requireNonNull(grade, "grade must not be null");
	}

}
