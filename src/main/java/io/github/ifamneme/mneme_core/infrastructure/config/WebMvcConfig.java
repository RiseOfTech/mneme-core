package io.github.ifamneme.mneme_core.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.github.ifamneme.mneme_core.infrastructure.web.AuthenticatedUserArgumentResolver;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

	private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;

	private final CorsProperties corsProperties;

	public WebMvcConfig(AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver,
			CorsProperties corsProperties) {
		this.authenticatedUserArgumentResolver = authenticatedUserArgumentResolver;
		this.corsProperties = corsProperties;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(authenticatedUserArgumentResolver);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/v1/**")
				.allowedOrigins(corsProperties.getAllowedOrigins().toArray(new String[0]))
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("Authorization", "Content-Type");
	}

}
