package io.github.ifamneme.mneme_core.infrastructure.web;

import org.springframework.security.oauth2.jwt.Jwt;

import io.github.ifamneme.mneme_core.domain.UserId;

public interface Authenticator {

	UserId resolve(Jwt jwt);

}
