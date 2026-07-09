package io.github.ifamneme.mneme_core.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;

import io.github.ifamneme.mneme_core.domain.UserId;

public interface MeApi {

	@GetMapping
	MeResponseDto getMe(@AuthenticatedUser UserId userId);

}
