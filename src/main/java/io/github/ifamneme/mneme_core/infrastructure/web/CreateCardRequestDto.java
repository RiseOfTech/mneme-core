package io.github.ifamneme.mneme_core.infrastructure.web;

import io.github.ifamneme.mneme_core.domain.Visibility;
import jakarta.validation.constraints.NotBlank;

public record CreateCardRequestDto(@NotBlank String word, @NotBlank String translation, @NotBlank String association,
		String imageUrl, Visibility visibility) {
}
