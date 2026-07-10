package io.github.ifamneme.mneme_core.infrastructure.web;

import io.github.ifamneme.mneme_core.domain.Grade;

public enum GradeDto {
	CORRECT,
	INCORRECT;

	public Grade toDomain() {
		return Grade.valueOf(name());
	}

}
