package io.github.ifamneme.mneme_core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
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

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;
import io.github.ifamneme.mneme_core.domain.UserId;

class CardEndpointIntegrationTest extends AbstractIntegrationTest {

	private static final String OWNER_TOKEN = "card-owner-token";
	private static final String OTHER_TOKEN = "card-other-token";
	private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final Duration FIRST_INTERVAL = Duration.ofDays(1);

	// Each list/delete-count test below needs an owner whose card set is not touched by any
	// other test method in this class (the Testcontainers Postgres instance is shared for the
	// whole JVM, so OWNER_ID/OTHER_ID accumulate cards across every other test method). Dedicated
	// tokens keep those assertions independent of test execution order.
	private static final String LIST_TOKEN = "card-list-token";
	private static final String LIST_FOREIGN_TOKEN = "card-list-foreign-token";
	private static final String EMPTY_LIST_TOKEN = "card-empty-list-token";
	private static final String DELETE_LIST_TOKEN = "card-delete-list-token";
	private static final UUID LIST_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID LIST_FOREIGN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
	private static final UUID EMPTY_LIST_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
	private static final UUID DELETE_LIST_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

	@DynamicPropertySource
	static void registerExtraTokens(DynamicPropertyRegistry registry) {
		registry.add("app.auth.tokens." + OWNER_TOKEN, () -> OWNER_ID.toString());
		registry.add("app.auth.tokens." + OTHER_TOKEN, () -> OTHER_ID.toString());
		registry.add("app.auth.tokens." + LIST_TOKEN, () -> LIST_ID.toString());
		registry.add("app.auth.tokens." + LIST_FOREIGN_TOKEN, () -> LIST_FOREIGN_ID.toString());
		registry.add("app.auth.tokens." + EMPTY_LIST_TOKEN, () -> EMPTY_LIST_ID.toString());
		registry.add("app.auth.tokens." + DELETE_LIST_TOKEN, () -> DELETE_LIST_ID.toString());
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ReviewRepository reviewRepository;

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	@Test
	void createThenGetRoundTrip() throws Exception {
		String createBody = """
				{"word":"surround","translation":"окружать","association":"сир, нас окружают"}
				""";

		Instant beforeCreate = Instant.now();
		String createResponse = mockMvc
				.perform(post("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN)
						.contentType(MediaType.APPLICATION_JSON).content(createBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.word").value("surround"))
				.andReturn().getResponse().getContentAsString();
		Instant afterCreate = Instant.now();

		JsonNode created = objectMapper.readTree(createResponse);
		String id = created.get("id").asString();

		mockMvc.perform(get("/v1/cards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.translation").value("окружать"));

		var review = reviewRepository.findByCardIdAndOwner(CardId.of(UUID.fromString(id)), new UserId(OWNER_ID))
				.orElseThrow();
		assertThat(review.state().box().number()).isEqualTo(1);
		assertThat(review.state().dueAt()).isBetween(beforeCreate.plus(FIRST_INTERVAL),
				afterCreate.plus(FIRST_INTERVAL));
	}

	@Test
	void missingCardAndForeignCardReturnByteIdentical404Bodies() throws Exception {
		String createBody = """
				{"word":"word","translation":"translation","association":"association"}
				""";

		String createResponse = mockMvc
				.perform(post("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN)
						.contentType(MediaType.APPLICATION_JSON).content(createBody))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String id = objectMapper.readTree(createResponse).get("id").asString();

		String missingCardBody = mockMvc
				.perform(get("/v1/cards/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION,
						"Bearer " + OWNER_TOKEN))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();

		String foreignCardBody = mockMvc
				.perform(get("/v1/cards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OTHER_TOKEN))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();

		assertThat(foreignCardBody).isEqualTo(missingCardBody);
		assertThat(missingCardBody).doesNotContain(id);
	}

	@Test
	void listReturnsOnlyCallersCardsNewestFirst() throws Exception {
		String firstId = createCard(LIST_TOKEN, "first", "translation", "association");
		String secondId = createCard(LIST_TOKEN, "second", "translation", "association");
		createCard(LIST_FOREIGN_TOKEN, "other", "translation", "association");

		mockMvc.perform(get("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + LIST_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(secondId))
				.andExpect(jsonPath("$[1].id").value(firstId));
	}

	@Test
	void listReturnsEmptyArrayForUserWithNoCards() throws Exception {
		mockMvc.perform(get("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + EMPTY_LIST_TOKEN))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	@Test
	void updateReplacesAllFieldsAndLeavesReviewUnchanged() throws Exception {
		String id = createCard(OWNER_TOKEN, "word", "translation", "association");
		Review before = reviewRepository.findByCardIdAndOwner(CardId.of(UUID.fromString(id)), new UserId(OWNER_ID))
				.orElseThrow();

		String updateBody = """
				{"word":"new-word","translation":"new-translation","association":"new-association","imageUrl":"https://example.com/new.png","visibility":"PUBLIC"}
				""";
		mockMvc.perform(put("/v1/cards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN)
				.contentType(MediaType.APPLICATION_JSON).content(updateBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.word").value("new-word"))
				.andExpect(jsonPath("$.translation").value("new-translation"))
				.andExpect(jsonPath("$.association").value("new-association"))
				.andExpect(jsonPath("$.imageUrl").value("https://example.com/new.png"))
				.andExpect(jsonPath("$.visibility").value("PUBLIC"));

		Review after = reviewRepository.findByCardIdAndOwner(CardId.of(UUID.fromString(id)), new UserId(OWNER_ID))
				.orElseThrow();
		assertThat(after.state().box()).isEqualTo(before.state().box());
		assertThat(after.state().dueAt()).isEqualTo(before.state().dueAt());
	}

	@Test
	void updateWithNoImageUrlClearsImageAndNoVisibilityDefaultsToPrivate() throws Exception {
		String id = createCard(OWNER_TOKEN, "word", "translation", "association");
		mockMvc.perform(put("/v1/cards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN)
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"word":"word","translation":"translation","association":"association","imageUrl":"https://example.com/first.png","visibility":"PUBLIC"}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(put("/v1/cards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN)
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"word":"word","translation":"translation","association":"association"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.imageUrl").doesNotExist())
				.andExpect(jsonPath("$.visibility").value("PRIVATE"));
	}

	@Test
	void updateOnMissingAndForeignCardReturnByteIdentical404Bodies() throws Exception {
		String id = createCard(OWNER_TOKEN, "word", "translation", "association");
		String updateBody = """
				{"word":"word","translation":"translation","association":"association"}
				""";

		String missingCardBody = mockMvc
				.perform(put("/v1/cards/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION,
						"Bearer " + OWNER_TOKEN).contentType(MediaType.APPLICATION_JSON).content(updateBody))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();

		String foreignCardBody = mockMvc
				.perform(put("/v1/cards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OTHER_TOKEN)
						.contentType(MediaType.APPLICATION_JSON).content(updateBody))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();

		assertThat(foreignCardBody).isEqualTo(missingCardBody);
		assertThat(missingCardBody).doesNotContain(id);
	}

	@Test
	void deleteRemovesCardAndReviewAndShrinksList() throws Exception {
		String keptId = createCard(DELETE_LIST_TOKEN, "kept", "translation", "association");
		String deletedId = createCard(DELETE_LIST_TOKEN, "deleted", "translation", "association");

		mockMvc.perform(
				delete("/v1/cards/" + deletedId).header(HttpHeaders.AUTHORIZATION, "Bearer " + DELETE_LIST_TOKEN))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		mockMvc.perform(
				get("/v1/cards/" + deletedId).header(HttpHeaders.AUTHORIZATION, "Bearer " + DELETE_LIST_TOKEN))
				.andExpect(status().isNotFound());
		assertThat(reviewRepository
			.findByCardIdAndOwner(CardId.of(UUID.fromString(deletedId)), new UserId(DELETE_LIST_ID))).isEmpty();

		mockMvc.perform(get("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + DELETE_LIST_TOKEN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(keptId));
	}

	@Test
	void deleteOnMissingAndForeignCardReturnByteIdentical404BodiesAndDeletesNothing() throws Exception {
		String id = createCard(OWNER_TOKEN, "word", "translation", "association");

		String missingCardBody = mockMvc
				.perform(delete("/v1/cards/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION,
						"Bearer " + OWNER_TOKEN))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();

		String foreignCardBody = mockMvc
				.perform(delete("/v1/cards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OTHER_TOKEN))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();

		assertThat(foreignCardBody).isEqualTo(missingCardBody);
		assertThat(missingCardBody).doesNotContain(id);

		mockMvc.perform(get("/v1/cards/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + OWNER_TOKEN))
				.andExpect(status().isOk());
	}

	private String createCard(String token, String word, String translation, String association) throws Exception {
		String createBody = objectMapper
			.writeValueAsString(Map.of("word", word, "translation", translation, "association", association));
		String createResponse = mockMvc
				.perform(post("/v1/cards").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON).content(createBody))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(createResponse).get("id").asString();
	}

}
