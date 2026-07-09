package io.github.ifamneme.mneme_core.infrastructure.web;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import io.github.ifamneme.mneme_core.domain.UserId;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType() == UserId.class && parameter.hasParameterAnnotation(AuthenticatedUser.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		Object attribute = request.getAttribute(AuthenticationFilter.USER_ID_ATTRIBUTE);
		if (!(attribute instanceof UserId userId)) {
			throw new IllegalStateException("No authenticated UserId found on the request");
		}
		return userId;
	}

}
