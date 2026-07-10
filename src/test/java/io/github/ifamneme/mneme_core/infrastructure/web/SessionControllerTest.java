package io.github.ifamneme.mneme_core.infrastructure.web;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ifamneme.mneme_core.application.GetReviewSessionUseCase;
import io.github.ifamneme.mneme_core.application.ReviewSessionView;
import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Image;
import io.github.ifamneme.mneme_core.domain.LeitnerBox;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Word;

@WebMvcTest(SessionApi.class)
class SessionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Authenticator authenticator;

	@MockitoBean
	private GetReviewSessionUseCase getReviewSessionUseCase;

	private final UserId owner = new UserId(UUID.randomUUID());

	@Test
	void whenDueCardsExist_mapsViewsToDtosInOrder() throws Exception {
		when(authenticator.authenticate("valid-token")).thenReturn(Optional.of(owner));

		ReviewSessionView first = new ReviewSessionView(CardId.newId(), new Word("surround"),
				new Translation("окружать"), new Association("сир, нас окружают"),
				Optional.of(new Image("https://example.com/image.png")), LeitnerBox.first(),
				Instant.parse("2026-07-01T00:00:00Z"));
		ReviewSessionView second = new ReviewSessionView(CardId.newId(), new Word("mnemonic"),
				new Translation("мнемоника"), new Association("association"), Optional.empty(), new LeitnerBox(2),
				Instant.parse("2026-07-02T00:00:00Z"));
		when(getReviewSessionUseCase.get(owner)).thenReturn(List.of(first, second));

		mockMvc.perform(get("/v1/session/cards").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].cardId").value(first.cardId().value().toString()))
				.andExpect(jsonPath("$[0].word").value("surround"))
				.andExpect(jsonPath("$[0].translation").value("окружать"))
				.andExpect(jsonPath("$[0].association").value("сир, нас окружают"))
				.andExpect(jsonPath("$[0].imageUrl").value("https://example.com/image.png"))
				.andExpect(jsonPath("$[0].box").value(1))
				.andExpect(jsonPath("$[1].cardId").value(second.cardId().value().toString()))
				.andExpect(jsonPath("$[1].box").value(2));
	}

	@Test
	void whenCardHasNoImage_imageUrlSerialisesAsNull() throws Exception {
		when(authenticator.authenticate("valid-token")).thenReturn(Optional.of(owner));

		ReviewSessionView view = new ReviewSessionView(CardId.newId(), new Word("mnemonic"),
				new Translation("мнемоника"), new Association("association"), Optional.empty(), LeitnerBox.first(),
				Instant.parse("2026-07-01T00:00:00Z"));
		when(getReviewSessionUseCase.get(owner)).thenReturn(List.of(view));

		mockMvc.perform(get("/v1/session/cards").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].imageUrl").value(nullValue()));
	}

	@Test
	void whenNothingDue_returnsEmptyJsonArray() throws Exception {
		when(authenticator.authenticate("valid-token")).thenReturn(Optional.of(owner));
		when(getReviewSessionUseCase.get(owner)).thenReturn(List.of());

		mockMvc.perform(get("/v1/session/cards").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void whenMissingAuthorizationHeader_returns401() throws Exception {
		mockMvc.perform(get("/v1/session/cards")).andExpect(status().isUnauthorized());
	}

}
