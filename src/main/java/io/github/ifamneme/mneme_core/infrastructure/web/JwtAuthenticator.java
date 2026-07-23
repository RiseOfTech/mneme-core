package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import io.github.ifamneme.mneme_core.domain.UserId;

@Component
public class JwtAuthenticator implements Authenticator {

	@Override
	public UserId resolve(Jwt jwt) {
		return new UserId(UUID.fromString(jwt.getSubject()));
	}

}
