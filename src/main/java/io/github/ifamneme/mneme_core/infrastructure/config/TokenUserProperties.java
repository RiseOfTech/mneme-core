package io.github.ifamneme.mneme_core.infrastructure.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class TokenUserProperties {

	private Map<String, String> tokens = Map.of();

	public Map<String, String> getTokens() {
		return tokens;
	}

	public void setTokens(Map<String, String> tokens) {
		this.tokens = tokens;
	}

}
