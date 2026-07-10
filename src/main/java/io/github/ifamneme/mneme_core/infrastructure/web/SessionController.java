package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ifamneme.mneme_core.application.GetReviewSessionUseCase;
import io.github.ifamneme.mneme_core.application.ReviewSessionView;
import io.github.ifamneme.mneme_core.domain.UserId;

@RestController
@RequestMapping("/v1/session")
public class SessionController implements SessionApi {

	private final GetReviewSessionUseCase getReviewSessionUseCase;

	public SessionController(GetReviewSessionUseCase getReviewSessionUseCase) {
		this.getReviewSessionUseCase = getReviewSessionUseCase;
	}

	@Override
	public List<SessionCardDto> getSessionCards(UserId userId) {
		List<ReviewSessionView> views = getReviewSessionUseCase.get(userId);
		return views.stream().map(SessionCardDto::from).toList();
	}

}
