package io.github.ifamneme.mneme_core.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class LeitnerScheduler implements LeitnerStrategy {

	private final List<Duration> intervals;

	public LeitnerScheduler(List<Duration> intervals) {
		Objects.requireNonNull(intervals, "intervals must not be null");
		if (intervals.size() != LeitnerBox.MAX_BOX) {
			throw new IllegalArgumentException("intervals must contain exactly " + LeitnerBox.MAX_BOX + " entries");
		}
		for (Duration interval : intervals) {
			if (interval == null || interval.isNegative()) {
				throw new IllegalArgumentException("intervals must not contain null or negative durations");
			}
		}
		this.intervals = List.copyOf(intervals);
	}

	@Override
	public ReviewState initialState(Instant now) {
		LeitnerBox box = LeitnerBox.first();
		return new ReviewState(box, now.plus(intervalFor(box)));
	}

	@Override
	public ReviewState next(ReviewState current, Grade grade, Instant now) {
		LeitnerBox newBox = grade == Grade.CORRECT ? current.box().next() : LeitnerBox.first();
		return new ReviewState(newBox, now.plus(intervalFor(newBox)));
	}

	private Duration intervalFor(LeitnerBox box) {
		return intervals.get(box.number() - 1);
	}

}
