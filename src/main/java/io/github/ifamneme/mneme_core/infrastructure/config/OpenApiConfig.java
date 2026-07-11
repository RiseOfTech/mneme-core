package io.github.ifamneme.mneme_core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	public static final String BEARER_AUTH = "bearerAuth";

	@Bean
	public OpenAPI mnemeOpenApi() {
		return new OpenAPI()
			.info(new Info().title("Mneme Core API")
				.version("0.0.1")
				.description("REST API for the mneme-core vocabulary-learning service."))
			.components(new Components().addSecuritySchemes(BEARER_AUTH,
					new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")));
	}

}
