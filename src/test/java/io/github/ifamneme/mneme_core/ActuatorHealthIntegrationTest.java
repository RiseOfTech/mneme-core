package io.github.ifamneme.mneme_core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class ActuatorHealthIntegrationTest extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthEndpointReturns200WithoutAuthorizationHeader() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

}
