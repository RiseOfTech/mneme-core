package io.github.ifamneme.mneme_core.domain;

import java.time.Instant;

public interface LeitnerStrategy {

	ReviewState initialState(Instant now);

	ReviewState next(ReviewState current, Grade grade, Instant now);

}
