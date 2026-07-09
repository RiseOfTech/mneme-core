package io.github.ifamneme.mneme_core.infrastructure.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.ifamneme.mneme_core.infrastructure.web.AuthenticatedUserArgumentResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;

	public WebMvcConfig(AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver) {
		this.authenticatedUserArgumentResolver = authenticatedUserArgumentResolver;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(authenticatedUserArgumentResolver);
	}

}
