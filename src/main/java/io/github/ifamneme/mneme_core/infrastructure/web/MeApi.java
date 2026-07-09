package io.github.ifamneme.mneme_core.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

public interface MeApi {

	@GetMapping
	MeResponseDto getMe(HttpServletRequest request);

}
