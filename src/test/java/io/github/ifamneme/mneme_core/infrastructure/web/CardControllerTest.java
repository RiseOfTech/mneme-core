package io.github.ifamneme.mneme_core.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ifamneme.mneme_core.application.CardNotFoundException;
import io.github.ifamneme.mneme_core.application.CreateCardUseCase;
import io.github.ifamneme.mneme_core.application.DeleteCardUseCase;
import io.github.ifamneme.mneme_core.application.GetCardUseCase;
import io.github.ifamneme.mneme_core.application.ListCardsUseCase;
import io.github.ifamneme.mneme_core.application.UpdateCardUseCase;
import io.github.ifamneme.mneme_core.domain.Association;
import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Translation;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.domain.Visibility;
import io.github.ifamneme.mneme_core.domain.Word;
import io.github.ifamneme.mneme_core.infrastructure.config.SecurityConfig;
import io.github.ifamneme.mneme_core.support.JwtTestTokens;

@WebMvcTest(CardApi.class)
@Import({ SecurityConfig.class, JwtAuthenticator.class, ProblemJsonAuthenticationEntryPoint.class })
class CardControllerTest {

	@DynamicPropertySource
	static void jwtPublicKey(DynamicPropertyRegistry registry) {
		registry.add("app.jwt.public-key", JwtTestTokens::publicKeyLocation);
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CreateCardUseCase createCardUseCase;

	@MockitoBean
	private GetCardUseCase getCardUseCase;

	@MockitoBean
	private ListCardsUseCase listCardsUseCase;

	@MockitoBean
	private UpdateCardUseCase updateCardUseCase;

	@MockitoBean
	private DeleteCardUseCase deleteCardUseCase;

	private final UUID knownUuid = UUID.randomUUID();
	private final UserId owner = new UserId(knownUuid);
	private final String validToken = JwtTestTokens.validToken(knownUuid);

	@Test
	void whenValidTokenAndBody_createsCardAndReturns201() throws Exception {
		Card card = new Card(owner, new Word("surround"), new Translation("окружать"),
				new Association("сир, нас окружают"), null, Visibility.PRIVATE);
		when(createCardUseCase.create(any())).thenReturn(card);

		mockMvc.perform(post("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
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
		mockMvc.perform(post("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"word":"   ","translation":"окружать","association":"сир, нас окружают"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors.word").exists());
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
		Card card = new Card(owner, new Word("surround"), new Translation("окружать"),
				new Association("сир, нас окружают"), null, Visibility.PRIVATE);
		when(getCardUseCase.get(eq(owner), eq(card.id()))).thenReturn(card);

		mockMvc.perform(get("/v1/cards/" + card.id().value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(card.id().value().toString()));
	}

	@Test
	void whenCardNotFound_returns404() throws Exception {
		CardId id = CardId.newId();
		when(getCardUseCase.get(eq(owner), eq(id))).thenThrow(new CardNotFoundException(id, owner));

		String body = mockMvc
				.perform(get("/v1/cards/" + id.value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain(id.value().toString());
	}

	@Test
	void whenMissingAuthorizationHeader_returns401OnGet() throws Exception {
		mockMvc.perform(get("/v1/cards/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
	}

	@Test
	void whenUseCaseThrowsUnexpectedException_returns500() throws Exception {
		CardId id = CardId.newId();
		when(getCardUseCase.get(eq(owner), eq(id))).thenThrow(new RuntimeException("boom: connection refused"));

		String body = mockMvc
				.perform(get("/v1/cards/" + id.value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain("boom").doesNotContain("RuntimeException");
	}

	@Test
	void whenCardsExist_listReturns200WithCards() throws Exception {
		Card card = new Card(owner, new Word("surround"), new Translation("окружать"),
				new Association("сир, нас окружают"), null, Visibility.PRIVATE);
		when(listCardsUseCase.list(owner)).thenReturn(List.of(card));

		mockMvc.perform(get("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(card.id().value().toString()))
				.andExpect(jsonPath("$[0].word").value("surround"));
	}

	@Test
	void whenNoCards_listReturns200WithEmptyArray() throws Exception {
		when(listCardsUseCase.list(owner)).thenReturn(List.of());

		mockMvc.perform(get("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void whenMissingAuthorizationHeader_returns401OnList() throws Exception {
		mockMvc.perform(get("/v1/cards")).andExpect(status().isUnauthorized());
	}

	@Test
	void whenValidTokenAndBody_updatesCardAndReturns200() throws Exception {
		CardId id = CardId.newId();
		Card updated = Card.reconstitute(id, owner, new Word("new-word"), new Translation("new-translation"),
				new Association("new-association"), null, Visibility.PUBLIC);
		when(updateCardUseCase.update(any())).thenReturn(updated);

		mockMvc.perform(put("/v1/cards/" + id.value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"word":"new-word","translation":"new-translation","association":"new-association","visibility":"PUBLIC"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.value().toString()))
				.andExpect(jsonPath("$.word").value("new-word"))
				.andExpect(jsonPath("$.visibility").value("PUBLIC"));
	}

	@Test
	void whenUpdateBodyHasBlankWord_returns400() throws Exception {
		CardId id = CardId.newId();

		mockMvc.perform(put("/v1/cards/" + id.value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"word":"   ","translation":"translation","association":"association"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.errors.word").exists());
	}

	@Test
	void whenUpdatingNonExistentCard_returns404() throws Exception {
		CardId id = CardId.newId();
		when(updateCardUseCase.update(any())).thenThrow(new CardNotFoundException(id, owner));

		String body = mockMvc
				.perform(put("/v1/cards/" + id.value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"word":"word","translation":"translation","association":"association"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain(id.value().toString());
	}

	@Test
	void whenMissingAuthorizationHeader_returns401OnUpdate() throws Exception {
		mockMvc.perform(put("/v1/cards/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"word":"word","translation":"translation","association":"association"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void whenValidToken_deletesCardAndReturns204() throws Exception {
		CardId id = CardId.newId();

		mockMvc.perform(delete("/v1/cards/" + id.value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
	}

	@Test
	void whenDeletingNonExistentCard_returns404() throws Exception {
		CardId id = CardId.newId();
		doThrow(new CardNotFoundException(id, owner)).when(deleteCardUseCase).delete(owner, id);

		mockMvc.perform(delete("/v1/cards/" + id.value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
	}

	@Test
	void whenMissingAuthorizationHeader_returns401OnDelete() throws Exception {
		mockMvc.perform(delete("/v1/cards/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
	}

}
