package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.ifamneme.mneme_core.domain.UserId;
import io.github.ifamneme.mneme_core.infrastructure.config.TokenUserProperties;

@Component
public class TokenAuthenticator implements Authenticator {

	private final Map<String, UserId> tokenToUserId;

	public TokenAuthenticator(TokenUserProperties properties) {
		this.tokenToUserId = properties.getTokens().entrySet().stream()
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
						entry -> new UserId(UUID.fromString(entry.getValue()))));
	}

	@Override
	public Optional<UserId> authenticate(String token) {
		return Optional.ofNullable(tokenToUserId.get(token));
	}

}
