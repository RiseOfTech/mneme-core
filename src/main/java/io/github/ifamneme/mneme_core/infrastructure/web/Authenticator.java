package io.github.ifamneme.mneme_core.infrastructure.web;

import java.util.Optional;

import io.github.ifamneme.mneme_core.domain.UserId;

public interface Authenticator {

	Optional<UserId> authenticate(String token);

}
