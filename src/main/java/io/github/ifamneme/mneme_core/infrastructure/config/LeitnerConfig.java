package io.github.ifamneme.mneme_core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.ifamneme.mneme_core.domain.LeitnerScheduler;
import io.github.ifamneme.mneme_core.domain.LeitnerStrategy;

@Configuration
public class LeitnerConfig {

	@Bean
	public LeitnerStrategy leitnerStrategy(LeitnerProperties properties) {
		return new LeitnerScheduler(properties.getIntervals());
	}

}
