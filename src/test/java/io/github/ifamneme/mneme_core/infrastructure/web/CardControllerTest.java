package io.github.ifamneme.mneme_core.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ifamneme.mneme_core.application.CardNotFoundException;
import io.github.ifamneme.mneme_core.application.CreateCardUseCase;
import io.github.ifamneme.mneme_core.application.GetCardUseCase;
import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;

@WebMvcTest(CardApi.class)
class CardControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Authenticator authenticator;

	@MockitoBean
	private CreateCardUseCase createCardUseCase;

	@MockitoBean
	private GetCardUseCase getCardUseCase;

	private final UUID knownUuid = UUID.randomUUID();
	private final UserId owner = new UserId(knownUuid);

	@Test
	void whenValidTokenAndBody_createsCardAndReturns201() throws Exception {
		when(authenticator.authenticate("valid-token")).thenReturn(Optional.of(owner));
		Card card = new Card(owner, new Word("surround"), new Translation("окружать"),
				new Association("сир, нас окружают"), null, Visibility.PRIVATE);
		when(createCardUseCase.create(any())).thenReturn(card);

		mockMvc.perform(post("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"word":"surround","translation":"окружать","association":"сир, нас окружают"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(card.id().value().toString()))
				.andExpect(jsonPath("$.word").value("surround"));
	}

	@Test
	void whenBlankWord_returns400() throws Exception {
		when(authenticator.authenticate("valid-token")).thenReturn(Optional.of(owner));

		mockMvc.perform(post("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"word":"   ","translation":"окружать","association":"сир, нас окружают"}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void whenMissingAuthorizationHeader_returns401OnCreate() throws Exception {
		mockMvc.perform(post("/v1/cards").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"word":"surround","translation":"окружать","association":"сир, нас окружают"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void whenExistingCard_returns200() throws Exception {
		when(authenticator.authenticate("valid-token")).thenReturn(Optional.of(owner));
		Card card = new Card(owner, new Word("surround"), new Translation("окружать"),
				new Association("сир, нас окружают"), null, Visibility.PRIVATE);
		when(getCardUseCase.get(eq(owner), eq(card.id()))).thenReturn(card);

		mockMvc.perform(get("/v1/cards/" + card.id().value()).header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(card.id().value().toString()));
	}

	@Test
	void whenCardNotFound_returns404() throws Exception {
		when(authenticator.authenticate("valid-token")).thenReturn(Optional.of(owner));
		CardId id = CardId.newId();
		when(getCardUseCase.get(eq(owner), eq(id))).thenThrow(new CardNotFoundException(id, owner));

		mockMvc.perform(get("/v1/cards/" + id.value()).header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isNotFound());
	}

	@Test
	void whenMissingAuthorizationHeader_returns401OnGet() throws Exception {
		mockMvc.perform(get("/v1/cards/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
	}

}
