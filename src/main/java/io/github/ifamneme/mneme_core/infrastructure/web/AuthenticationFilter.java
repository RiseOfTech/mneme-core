package io.github.ifamneme.mneme_core.infrastructure.web;

import java.io.IOException;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.ifamneme.mneme_core.domain.UserId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {

	public static final String USER_ID_ATTRIBUTE = "userId";

	private static final String BEARER_PREFIX = "Bearer ";

	private final Authenticator authenticator;

	public AuthenticationFilter(Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/actuator");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		String token = header.substring(BEARER_PREFIX.length());
		Optional<UserId> userId = authenticator.authenticate(token);
		if (userId.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		request.setAttribute(USER_ID_ATTRIBUTE, userId.get());
		filterChain.doFilter(request, response);
	}

}
