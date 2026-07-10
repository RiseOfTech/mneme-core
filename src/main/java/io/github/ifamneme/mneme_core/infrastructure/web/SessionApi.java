package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;

import io.github.ifamneme.mneme_core.domain.UserId;

public interface SessionApi {

	@GetMapping("/cards")
	List<SessionCardDto> getSessionCards(@AuthenticatedUser UserId userId);

}
