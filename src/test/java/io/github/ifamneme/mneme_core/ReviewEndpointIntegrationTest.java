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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class ReviewEndpointIntegrationTest extends AbstractIntegrationTest {

	private static final String OWNER_TOKEN = "review-owner-token";
	private static final String OTHER_TOKEN = "review-other-token";
	private static final UUID OWNER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID OTHER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
	private static final Duration FIRST_INTERVAL = Duration.ofDays(1);
	private static final Duration SECOND_INTERVAL = Duration.ofDays(3);

	@DynamicPropertySource
	static void registerExtraTokens(DynamicPropertyRegistry registry) {
		registry.add("app.auth.tokens." + OWNER_TOKEN, () -> OWNER_ID.toString());
		registry.add("app.auth.tokens." + OTHER_TOKEN, () -> OTHER_ID.toString());
	}

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

}
