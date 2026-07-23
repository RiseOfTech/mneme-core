package io.github.ifamneme.mneme_core.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class JwtPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfig.class);

	// Regression test: app.jwt.public-key binds to a Resource, not raw PEM text, so the
	// classpath location application.yaml defaults to must actually resolve to real
	// content - a wrong/missing path here previously surfaced as an opaque
	// "Illegal base64 character" failure at JwtDecoder construction time instead.
	@Test
	void whenPropertyPointsAtClasspathKey_resolvesToRealPemContent() {
		contextRunner.withPropertyValues("app.jwt.public-key=classpath:keys/mneme-auth-public.pem").run(context -> {
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
