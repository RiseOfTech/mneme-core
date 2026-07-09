package io.github.ifamneme.mneme_core.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record CreateCardRequestDto(@NotBlank String word, @NotBlank String translation, @NotBlank String association,
		String imageUrl, VisibilityDto visibility) {
}
