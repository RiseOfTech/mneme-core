package io.github.ifamneme.mneme_core.infrastructure.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ifamneme.mneme_core.domain.UserId;

@WebMvcTest(MeApi.class)
class MeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private Authenticator authenticator;

	@Test
	void whenValidToken_returns200WithUserId() throws Exception {
		UUID knownUuid = UUID.randomUUID();
		when(authenticator.authenticate("valid-token")).thenReturn(Optional.of(new UserId(knownUuid)));

		mockMvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer valid-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(knownUuid.toString()));
	}

	@Test
	void whenMissingAuthorizationHeader_returns401() throws Exception {
		mockMvc.perform(get("/v1/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void whenUnknownToken_returns401() throws Exception {
		when(authenticator.authenticate(any())).thenReturn(Optional.empty());

		mockMvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
				.andExpect(status().isUnauthorized());
	}

}
