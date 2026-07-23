package io.github.ifamneme.mneme_core.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ifamneme.mneme_core.application.GradeReviewUseCase;
import io.github.ifamneme.mneme_core.application.ReviewNotFoundException;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.LeitnerBox;
import io.github.ifamneme.mneme_core.domain.Review;
import io.github.ifamneme.mneme_core.domain.ReviewState;
import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.infrastructure.config.SecurityConfig;
import io.github.ifamneme.mneme_core.support.JwtTestTokens;

@WebMvcTest(ReviewApi.class)
@Import({ SecurityConfig.class, JwtAuthenticator.class, ProblemJsonAuthenticationEntryPoint.class })
class ReviewControllerTest {

	@DynamicPropertySource
	static void jwtPublicKey(DynamicPropertyRegistry registry) {
		registry.add("app.jwt.public-key", JwtTestTokens::publicKeyLocation);
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GradeReviewUseCase gradeReviewUseCase;

	private final UUID knownUuid = UUID.randomUUID();

	private final UserId owner = new UserId(knownUuid);

	private final String validToken = JwtTestTokens.validToken(knownUuid);

	@Test
	void whenValidTokenAndBody_gradesAndReturns200() throws Exception {
		CardId cardId = CardId.newId();
		Instant dueAt = Instant.parse("2026-07-12T00:00:00Z");
		Review graded = new Review(cardId, owner, new ReviewState(new LeitnerBox(2), dueAt));
		when(gradeReviewUseCase.grade(any())).thenReturn(graded);

		mockMvc.perform(post("/v1/reviews/" + cardId.value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"grade":"CORRECT"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.box").value(2))
				.andExpect(jsonPath("$.dueAt").value(dueAt.toString()));
	}

	@Test
	void whenReviewNotFound_returns404() throws Exception {
		CardId cardId = CardId.newId();
		when(gradeReviewUseCase.grade(any())).thenThrow(new ReviewNotFoundException(cardId, owner));

		String body = mockMvc
				.perform(post("/v1/reviews/" + cardId.value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"grade":"CORRECT"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"))
				.andReturn().getResponse().getContentAsString();

		assertThat(body).doesNotContain(cardId.value().toString());
	}

	@Test
	void whenMissingGrade_returns400() throws Exception {
		CardId cardId = CardId.newId();

		mockMvc.perform(post("/v1/reviews/" + cardId.value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void whenUnrecognisedGrade_returns400() throws Exception {
		CardId cardId = CardId.newId();

		mockMvc.perform(post("/v1/reviews/" + cardId.value()).header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"grade":"MAYBE"}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void whenMissingAuthorizationHeader_returns401() throws Exception {
		CardId cardId = CardId.newId();

		mockMvc.perform(post("/v1/reviews/" + cardId.value()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"grade":"CORRECT"}
						"""))
				.andExpect(status().isUnauthorized());
	}

}
