package io.github.ifamneme.mneme_core.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import io.github.ifamneme.mneme_core.support.JwtTestTokens;

class JwtPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfig.class);

	// Regression test: app.jwt.public-key binds to a Resource, not raw PEM text - binding
	// it as text previously surfaced as an opaque "Illegal base64 character" failure at
	// JwtDecoder construction time. Uses a generated key: *.pem files are not in git.
	@Test
	void whenPropertyPointsAtKeyLocation_resolvesToRealPemContent() {
		contextRunner.withPropertyValues("app.jwt.public-key=" + JwtTestTokens.publicKeyLocation()).run(context -> {
			var resource = context.getBean(JwtProperties.class).getPublicKey();
			assertThat(resource.exists()).isTrue();
			try (var in = resource.getInputStream()) {
				String content = new String(in.readAllBytes());
				assertThat(content).contains("-----BEGIN PUBLIC KEY-----").contains("-----END PUBLIC KEY-----");
			}
			catch (IOException ex) {
				throw new AssertionError(ex);
			}
		});
	}

	@Configuration
	@EnableConfigurationProperties(JwtProperties.class)
	static class TestConfig {

	}

}
