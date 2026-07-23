package io.github.ifamneme.mneme_core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import io.github.ifamneme.mneme_core.support.JwtTestTokens;

class ReviewEndpointIntegrationTest extends AbstractIntegrationTest {

	// Distinct from CardEndpointIntegrationTest's owner UUIDs - both classes share the
	// same singleton Postgres container for the whole JVM, so reusing a UUID across test
	// classes lets one class's cards leak into another's list/count assertions.
	private static final UUID OWNER_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
	private static final UUID OTHER_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
	private static final String OWNER_TOKEN = JwtTestTokens.validToken(OWNER_ID);
	private static final String OTHER_TOKEN = JwtTestTokens.validToken(OTHER_ID);
	private static final Duration FIRST_INTERVAL = Duration.ofDays(1);
	private static final Duration SECOND_INTERVAL = Duration.ofDays(3);

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	private String createCard(String token) throws Exception {
		String createResponse = mockMvc
				.perform(post("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"word":"word","translation":"translation","association":"association"}
								"""))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		JsonNode created = objectMapper.readTree(createResponse);
		return created.get("id").asString();
	}

	@Test
	void correctThenIncorrectMovesReviewThroughBoxes() throws Exception {
		String id = createCard(OWNER_TOKEN);

		mockMvc.perform(post("/v1/reviews/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"grade":"CORRECT"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.box").value(2));

		mockMvc.perform(post("/v1/reviews/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"grade":"INCORRECT"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.box").value(1));

		// Card content is unchanged by either grading call — the grading path never
		// touches the cards table.
		mockMvc.perform(get("/v1/cards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.word").value("word"))
				.andExpect(jsonPath("$.translation").value("translation"))
				.andExpect(jsonPath("$.association").value("association"));
	}

	@Test
	void correctGradeReturnsBoxAndDueAtFromSecondInterval() throws Exception {
		String id = createCard(OWNER_TOKEN);

		Instant before = Instant.now();
		String responseBody = mockMvc
				.perform(post("/v1/reviews/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"grade":"CORRECT"}
								"""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.box").value(2)).andReturn().getResponse()
				.getContentAsString();
		Instant after = Instant.now();

		Instant dueAt = Instant.parse(objectMapper.readTree(responseBody).get("dueAt").asString());
		assertThat(dueAt).isBetween(before.plus(SECOND_INTERVAL), after.plus(SECOND_INTERVAL));
	}

	@Test
	void missingReviewAndForeignReviewReturnByteIdentical404Bodies() throws Exception {
		String id = createCard(OWNER_TOKEN);

		String missingReviewBody = mockMvc
				.perform(post("/v1/reviews/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION,
						"Bearer " + OWNER_TOKEN).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"grade":"CORRECT"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();

		String foreignReviewBody = mockMvc
				.perform(post("/v1/reviews/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OTHER_TOKEN)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"grade":"CORRECT"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();

		assertThat(foreignReviewBody).isEqualTo(missingReviewBody);
		assertThat(missingReviewBody).doesNotContain(id);
	}

	@Test
	void malformedGradeReturns400() throws Exception {
		String id = createCard(OWNER_TOKEN);

		mockMvc.perform(post("/v1/reviews/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN)
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	// Regression test for the ERROR-dispatch trap: Spring Security re-authorizes Boot's
	// internal forward to /error against the path "/error" itself, which matches no
	// permitAll rule unless DispatcherType.ERROR is explicitly permitted - otherwise this
	// comes back 401 instead of the correct 4xx. Only reproducible over a full context;
	// invisible to a @WebMvcTest slice.
	@Test
	void getOnPostOnlyReviewRouteReturnsMethodNotAllowedNotUnauthorized() throws Exception {
		String id = createCard(OWNER_TOKEN);

		mockMvc.perform(get("/v1/reviews/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
	}

}
