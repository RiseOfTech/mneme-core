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

import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.ReviewRepository;

class CardEndpointIntegrationTest extends AbstractIntegrationTest {

	private static final String OWNER_TOKEN = "card-owner-token";
	private static final String OTHER_TOKEN = "card-other-token";
	private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final Duration FIRST_INTERVAL = Duration.ofDays(1);

	@DynamicPropertySource
	static void registerExtraTokens(DynamicPropertyRegistry registry) {
		registry.add("app.auth.tokens." + OWNER_TOKEN, () -> OWNER_ID.toString());
		registry.add("app.auth.tokens." + OTHER_TOKEN, () -> OTHER_ID.toString());
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

		var review = reviewRepository.findByCardId(CardId.of(UUID.fromString(id))).orElseThrow();
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

}
