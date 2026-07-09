package io.github.ifamneme.mneme_core.infrastructure.web;

import io.github.ifamneme.mneme_core.domain.Visibility;

public enum VisibilityDto {
	PRIVATE,
	PUBLIC;

	public Visibility toDomain() {
		return Visibility.valueOf(name());
	}

	public static VisibilityDto from(Visibility visibility) {
		return VisibilityDto.valueOf(visibility.name());
	}

}
