package io.github.ifamneme.mneme_core;

import static org.hamcrest.Matchers.nullValue;
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

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.LeitnerBox;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;
import io.github.ifamneme.mneme_core.domain.ReviewState;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.support.JwtTestTokens;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Each test method uses its own dedicated owner token/id. The Testcontainers Postgres
 * instance and Spring context are shared (singleton-container pattern, no per-test
 * rollback), and this endpoint lists *all* due reviews for an owner — reusing one owner
 * across methods would let one test's past-due review leak into another's assertions.
 */
class SessionEndpointIntegrationTest extends AbstractIntegrationTest {

	private static final UUID FRESH_ID = UUID.randomUUID();
	private static final UUID DUE_ID = UUID.randomUUID();
	private static final UUID GRADE_CORRECT_ID = UUID.randomUUID();
	private static final UUID GRADE_INCORRECT_ID = UUID.randomUUID();
	private static final UUID SELF_ID = UUID.randomUUID();
	private static final UUID FOREIGN_ID = UUID.randomUUID();

	private static final String FRESH_TOKEN = JwtTestTokens.validToken(FRESH_ID);
	private static final String DUE_TOKEN = JwtTestTokens.validToken(DUE_ID);
	private static final String GRADE_CORRECT_TOKEN = JwtTestTokens.validToken(GRADE_CORRECT_ID);
	private static final String GRADE_INCORRECT_TOKEN = JwtTestTokens.validToken(GRADE_INCORRECT_ID);
	private static final String SELF_TOKEN = JwtTestTokens.validToken(SELF_ID);
	private static final String FOREIGN_TOKEN = JwtTestTokens.validToken(FOREIGN_ID);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReviewRepository reviewRepository;

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

	private void makeDueNow(String cardId, UserId owner) {
		CardId id = CardId.of(UUID.fromString(cardId));
		Review current = reviewRepository.findByCardIdAndOwner(id, owner).orElseThrow();
		Review pastDue = new Review(id, owner,
				new ReviewState(current.state().box(), Instant.now().minus(Duration.ofDays(1))));
		reviewRepository.save(pastDue);
	}

	@Test
	void freshlyCreatedCardIsNotInSession() throws Exception {
		createCard(FRESH_TOKEN);

		mockMvc.perform(get("/v1/session/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + FRESH_TOKEN))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	void dueCardAppearsWithFullFields() throws Exception {
		String id = createCard(DUE_TOKEN);
		makeDueNow(id, new UserId(DUE_ID));

		mockMvc.perform(get("/v1/session/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + DUE_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].cardId").value(id))
				.andExpect(jsonPath("$[0].word").value("word"))
				.andExpect(jsonPath("$[0].translation").value("translation"))
				.andExpect(jsonPath("$[0].association").value("association"))
				.andExpect(jsonPath("$[0].imageUrl").value(nullValue()))
				.andExpect(jsonPath("$[0].box").value(LeitnerBox.first().number()));
	}

	@Test
	void gradingDueCardCorrectRemovesItFromSession() throws Exception {
		String id = createCard(GRADE_CORRECT_TOKEN);
		makeDueNow(id, new UserId(GRADE_CORRECT_ID));

		mockMvc.perform(get("/v1/session/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + GRADE_CORRECT_TOKEN))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(post("/v1/reviews/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + GRADE_CORRECT_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"grade":"CORRECT"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/v1/session/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + GRADE_CORRECT_TOKEN))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	void gradingDueCardIncorrectAlsoRemovesItFromSession() throws Exception {
		String id = createCard(GRADE_INCORRECT_TOKEN);
		makeDueNow(id, new UserId(GRADE_INCORRECT_ID));

		mockMvc.perform(post("/v1/reviews/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + GRADE_INCORRECT_TOKEN)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"grade":"INCORRECT"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/v1/session/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + GRADE_INCORRECT_TOKEN))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	void anotherUsersDueCardNeverAppears() throws Exception {
		String id = createCard(FOREIGN_TOKEN);
		makeDueNow(id, new UserId(FOREIGN_ID));

		mockMvc.perform(get("/v1/session/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + SELF_TOKEN))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	void missingAuthorizationHeaderReturns401() throws Exception {
		mockMvc.perform(get("/v1/session/cards")).andExpect(status().isUnauthorized());
	}

}
