package io.github.ifamneme.mneme_core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ifamneme.mneme_core.support.JwtTestTokens;

class MeEndpointIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void validTokenReturns200WithSubjectAsUserId() throws Exception {
		UUID subject = UUID.randomUUID();
		String token = JwtTestTokens.validToken(subject);

		mockMvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(subject.toString()));
	}

	@Test
	void expiredTokenReturns401() throws Exception {
		String token = JwtTestTokens.expiredToken(UUID.randomUUID());

		mockMvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void wrongIssuerReturns401() throws Exception {
		String token = JwtTestTokens.wrongIssuerToken(UUID.randomUUID());

		mockMvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void wrongAudienceReturns401() throws Exception {
		String token = JwtTestTokens.wrongAudienceToken(UUID.randomUUID());

		mockMvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenSignedByDifferentKeyReturns401() throws Exception {
		String token = JwtTestTokens.signedByOtherKeyToken(UUID.randomUUID());

		mockMvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void malformedTokenReturns401() throws Exception {
		mockMvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void missingAuthorizationHeaderReturns401() throws Exception {
		mockMvc.perform(get("/v1/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void unauthenticatedResponseIsProblemJsonWithUnauthenticatedCodeAndNoLeakedReason() throws Exception {
		mockMvc.perform(get("/v1/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
				.andExpect(jsonPath("$.detail").value("Authentication failed."));
	}

}
