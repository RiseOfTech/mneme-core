package io.github.ifamneme.mneme_core.infrastructure.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ifamneme.mneme_core.domain.UserId;

@RestController
@RequestMapping("/v1/me")
public class MeController implements MeApi {

	@Override
	public MeResponseDto getMe(UserId userId) {
		return new MeResponseDto(userId.value());
	}

}
