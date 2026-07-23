package io.github.ifamneme.mneme_core.infrastructure.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ifamneme.mneme_core.infrastructure.config.SecurityConfig;
import io.github.ifamneme.mneme_core.support.JwtTestTokens;

@WebMvcTest(MeApi.class)
@Import({ SecurityConfig.class, JwtAuthenticator.class, ProblemJsonAuthenticationEntryPoint.class })
class MeControllerTest {

	@DynamicPropertySource
	static void jwtPublicKey(DynamicPropertyRegistry registry) {
		registry.add("app.jwt.public-key", JwtTestTokens::publicKeyLocation);
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void whenValidToken_returns200WithUserId() throws Exception {
		UUID knownUuid = UUID.randomUUID();
		String token = JwtTestTokens.validToken(knownUuid);

		mockMvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(knownUuid.toString()));
	}

	@Test
	void whenMissingAuthorizationHeader_returns401() throws Exception {
		mockMvc.perform(get("/v1/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void whenMalformedToken_returns401() throws Exception {
		mockMvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
				.andExpect(status().isUnauthorized());
	}

}
