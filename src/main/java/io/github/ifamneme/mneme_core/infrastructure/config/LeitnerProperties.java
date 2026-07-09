package io.github.ifamneme.mneme_core.infrastructure.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.leitner")
public class LeitnerProperties {

	private List<Duration> intervals = List.of(Duration.ofDays(1), Duration.ofDays(3), Duration.ofDays(7),
			Duration.ofDays(16), Duration.ofDays(30));

	public List<Duration> getIntervals() {
		return intervals;
	}

	public void setIntervals(List<Duration> intervals) {
		this.intervals = intervals;
	}

}
