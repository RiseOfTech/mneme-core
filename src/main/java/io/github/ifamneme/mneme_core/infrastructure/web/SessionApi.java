package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;

import io.github.ifamneme.mneme_core.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = "bearerAuth")
public interface SessionApi {

	@Operation(summary = "List the caller's cards due for review now")
	@GetMapping("/cards")
	List<SessionCardDto> getSessionCards(@Parameter(hidden = true) @AuthenticatedUser UserId userId);

}
