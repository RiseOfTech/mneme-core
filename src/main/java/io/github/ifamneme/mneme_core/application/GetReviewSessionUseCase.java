package io.github.ifamneme.mneme_core.application;

import java.util.List;

import io.github.ifamneme.mneme_core.domain.UserId;

public interface GetReviewSessionUseCase {

	List<ReviewSessionView> get(UserId owner);

}
