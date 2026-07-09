package io.github.ifamneme.mneme_core.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class LeitnerSchedulerTest {

	private static final List<Duration> INTERVALS = List.of(Duration.ofDays(1), Duration.ofDays(3),
			Duration.ofDays(7), Duration.ofDays(16), Duration.ofDays(30));

	private final Instant now = Instant.parse("2026-07-09T00:00:00Z");
	private final LeitnerScheduler scheduler = new LeitnerScheduler(INTERVALS);

	@Test
	void constructorRejectsWrongSizedIntervalList() {
		assertThatThrownBy(() -> new LeitnerScheduler(List.of(Duration.ofDays(1))))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void initialStateStartsAtBoxOneWithFirstInterval() {
		ReviewState state = scheduler.initialState(now);

		assertThat(state.box().number()).isEqualTo(1);
		assertThat(state.dueAt()).isEqualTo(now.plus(INTERVALS.get(0)));
	}

	@Test
	void correctPromotesFromBoxOneToBoxTwo() {
		ReviewState current = new ReviewState(new LeitnerBox(1), now);

		ReviewState next = scheduler.next(current, Grade.CORRECT, now);

		assertThat(next.box().number()).isEqualTo(2);
		assertThat(next.dueAt()).isEqualTo(now.plus(INTERVALS.get(1)));
	}

	@Test
	void correctAtLastBoxStaysAtLastBox() {
		ReviewState current = new ReviewState(new LeitnerBox(LeitnerBox.MAX_BOX), now);

		ReviewState next = scheduler.next(current, Grade.CORRECT, now);

		assertThat(next.box().number()).isEqualTo(LeitnerBox.MAX_BOX);
		assertThat(next.dueAt()).isEqualTo(now.plus(INTERVALS.get(LeitnerBox.MAX_BOX - 1)));
	}

	@Test
	void incorrectResetsToBoxOneFromAnyBox() {
		ReviewState current = new ReviewState(new LeitnerBox(3), now);

		ReviewState next = scheduler.next(current, Grade.INCORRECT, now);

		assertThat(next.box().number()).isEqualTo(1);
		assertThat(next.dueAt()).isEqualTo(now.plus(INTERVALS.get(0)));
	}

}
