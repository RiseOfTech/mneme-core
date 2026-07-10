package io.github.ifamneme.mneme_core.infrastructure.web;

import jakarta.validation.constraints.NotNull;

public record GradeRequestDto(@NotNull GradeDto grade) {
}
