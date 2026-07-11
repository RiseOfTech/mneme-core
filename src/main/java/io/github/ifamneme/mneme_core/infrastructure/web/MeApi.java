package io.github.ifamneme.mneme_core.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;

import io.github.ifamneme.mneme_core.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "bearerAuth")
public interface MeApi {

	@Operation(summary = "Get the authenticated caller's identity")
	@GetMapping
	MeResponseDto getMe(@Parameter(hidden = true) @AuthenticatedUser UserId userId);

}
