package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.UUID;

public class MeResponseDto {

	private final UUID userId;

	public MeResponseDto(UUID userId) {
		this.userId = userId;
	}

	public UUID getUserId() {
		return userId;
	}

}
