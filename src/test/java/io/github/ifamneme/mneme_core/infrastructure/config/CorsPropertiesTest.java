package io.github.ifamneme.mneme_core.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class CorsPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfig.class);

	@Test
	void whenPropertyConfigured_bindsAllowedOrigins() {
		contextRunner.withPropertyValues("app.cors.allowed-origins=http://localhost:5173,https://example.com")
				.run(context -> assertThat(context.getBean(CorsProperties.class).getAllowedOrigins())
						.containsExactly("http://localhost:5173", "https://example.com"));
	}

	@Test
	void whenPropertyNotConfigured_defaultsToEmptyList() {
		contextRunner.run(context -> assertThat(context.getBean(CorsProperties.class).getAllowedOrigins()).isEmpty());
	}

	@Test
	void whenPropertyExplicitlyEmpty_bindsToEmptyList() {
		contextRunner.withPropertyValues("app.cors.allowed-origins=")
				.run(context -> assertThat(context.getBean(CorsProperties.class).getAllowedOrigins()).isEmpty());
	}

	@Configuration
	@EnableConfigurationProperties(CorsProperties.class)
	static class TestConfig {

	}

}
