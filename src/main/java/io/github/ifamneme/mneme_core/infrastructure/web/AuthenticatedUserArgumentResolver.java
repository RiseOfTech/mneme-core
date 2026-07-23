package io.github.ifamneme.mneme_core.infrastructure.web;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import io.github.ifamneme.mneme_core.domain.UserId;

@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

	private final Authenticator authenticator;

	public AuthenticatedUserArgumentResolver(Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType() == UserId.class && parameter.hasParameterAnnotation(AuthenticatedUser.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
			throw new IllegalStateException("No authenticated JWT found on the security context");
		}
		Jwt jwt = jwtAuthentication.getToken();
		return authenticator.resolve(jwt);
	}

}
