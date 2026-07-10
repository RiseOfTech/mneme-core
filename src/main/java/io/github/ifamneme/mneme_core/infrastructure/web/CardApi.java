package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.github.ifamneme.mneme_core.domain.UserId;
import jakarta.validation.Valid;

public interface CardApi {

	@PostMapping
	ResponseEntity<CardResponseDto> createCard(@Valid @RequestBody CreateCardRequestDto request,
			@AuthenticatedUser UserId userId);

	@GetMapping("/{id}")
	CardResponseDto getCard(@PathVariable UUID id, @AuthenticatedUser UserId userId);

	@GetMapping
	List<CardResponseDto> listCards(@AuthenticatedUser UserId userId);

	@PutMapping("/{id}")
	CardResponseDto updateCard(@PathVariable UUID id, @Valid @RequestBody UpdateCardRequestDto request,
			@AuthenticatedUser UserId userId);

	@DeleteMapping("/{id}")
	ResponseEntity<Void> deleteCard(@PathVariable UUID id, @AuthenticatedUser UserId userId);

}
