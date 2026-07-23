package io.github.ifamneme.mneme_core.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ifamneme.mneme_core.application.CreateCardUseCase;
import io.github.ifamneme.mneme_core.application.DeleteCardUseCase;
import io.github.ifamneme.mneme_core.application.GetCardUseCase;
import io.github.ifamneme.mneme_core.application.ListCardsUseCase;
import io.github.ifamneme.mneme_core.application.UpdateCardUseCase;
import io.github.ifamneme.mneme_core.infrastructure.config.SecurityConfig;
import io.github.ifamneme.mneme_core.support.JwtTestTokens;

/**
 * Simulates the production posture (no configured origins) without needing the
 * {@code prod} profile: {@code app.cors.allowed-origins=} binds to an empty list, and
 * the resulting deny-all mapping must not fall back to any permissive default.
 */
@WebMvcTest(CardApi.class)
@Import({ SecurityConfig.class, JwtAuthenticator.class, ProblemJsonAuthenticationEntryPoint.class })
@TestPropertySource(properties = "app.cors.allowed-origins=")
class CorsConfigEmptyOriginsTest {

	private static final String ANY_ORIGIN = "http://localhost:5173";

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

	@Test
	void whenNoOriginsConfigured_noOriginIsEverAllowed() throws Exception {
		mockMvc.perform(options("/v1/cards").header(HttpHeaders.ORIGIN, ANY_ORIGIN)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

}
