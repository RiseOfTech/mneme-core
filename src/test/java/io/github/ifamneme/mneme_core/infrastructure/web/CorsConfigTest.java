package io.github.ifamneme.mneme_core.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ifamneme.mneme_core.application.CreateCardUseCase;
import io.github.ifamneme.mneme_core.application.DeleteCardUseCase;
import io.github.ifamneme.mneme_core.application.GetCardUseCase;
import io.github.ifamneme.mneme_core.application.ListCardsUseCase;
import io.github.ifamneme.mneme_core.application.UpdateCardUseCase;
import io.github.ifamneme.mneme_core.domain.UserId;

@WebMvcTest(CardApi.class)
class CorsConfigTest {

	private static final String ALLOWED_ORIGIN = "http://localhost:5173";

	private static final String DISALLOWED_ORIGIN = "http://evil.example";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Authenticator authenticator;

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

	private final UserId owner = new UserId(UUID.randomUUID());

	@Test
	void whenPreflightFromAllowedOrigin_returnsAllowedCorsHeaders() throws Exception {
		mockMvc.perform(options("/v1/cards").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
				.andExpect(
						header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("authorization")));
	}

	@Test
	void whenPreflightFromDisallowedOrigin_doesNotEchoOrigin() throws Exception {
		mockMvc.perform(options("/v1/cards").header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void whenRealCrossOriginRequestWithValidToken_succeedsWithCorsHeaders() throws Exception {
		when(authenticator.authenticate("valid-token")).thenReturn(Optional.of(owner));
		when(listCardsUseCase.list(owner)).thenReturn(List.of());

		mockMvc.perform(get("/v1/cards").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
				.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
	}

	@Test
	void whenOptionsPreflightWithoutAuthorizationHeader_isNotUnauthorized() throws Exception {
		mockMvc.perform(options("/v1/cards").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")).andExpect(status().isOk());
	}

}
