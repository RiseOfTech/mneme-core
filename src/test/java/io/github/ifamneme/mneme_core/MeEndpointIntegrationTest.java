package io.github.ifamneme.mneme_core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

class MeEndpointIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void validTokenReturns200WithSeededUserId() throws Exception {
		mockMvc.perform(get("/v1/me").header(HttpHeaders.AUTHORIZATION, "Bearer demo-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value("00000000-0000-0000-0000-000000000001"));
	}

}
