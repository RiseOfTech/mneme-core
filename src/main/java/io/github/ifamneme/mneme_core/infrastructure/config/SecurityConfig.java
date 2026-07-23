package io.github.ifamneme.mneme_core.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.DispatcherType;

/**
 * Replaces the old {@code AuthenticationFilter}: token verification, the public-path
 * allowlist, CORS, and the OPTIONS-preflight bypass all move here.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({ JwtProperties.class, CorsProperties.class })
public class SecurityConfig {

	// Normative per the token contract (docs/specs/jwt-auth.md) - not configurable, no
	// other issuer/audience is ever valid.
	private static final String EXPECTED_ISSUER = "mneme-auth";

	private static final String EXPECTED_AUDIENCE = "mneme-core";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
			CorsConfigurationSource corsConfigurationSource, AuthenticationEntryPoint authenticationEntryPoint)
			throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
						.requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder)))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint));

		return http.build();
	}

	@Bean
	public JwtDecoder jwtDecoder(JwtProperties jwtProperties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(parsePublicKey(jwtProperties.getPublicKey())).build();

		OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(EXPECTED_ISSUER);
		OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
				audience -> audience != null && audience.contains(EXPECTED_AUDIENCE));
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));

		return decoder;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/v1/**", configuration);
		return source;
	}

	// The public key arrives as a PEM resource (env-configured location or mounted
	// file); this is key *decoding*, not JWT parsing or signature verification - that
	// stays entirely inside NimbusJwtDecoder.
	private static RSAPublicKey parsePublicKey(Resource publicKeyResource) {
		if (publicKeyResource == null || !publicKeyResource.exists()) {
			throw new IllegalStateException("app.jwt.public-key does not point to an existing resource: "
					+ describe(publicKeyResource));
		}

		String pem;
		try (InputStream in = publicKeyResource.getInputStream()) {
			pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to read app.jwt.public-key from " + describe(publicKeyResource), ex);
		}

		String base64 = pem.lines().filter(line -> !line.startsWith("-----")).reduce("", String::concat);
		byte[] encoded;
		try {
			encoded = Base64.getDecoder().decode(base64);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException(
					"app.jwt.public-key at " + describe(publicKeyResource) + " is not valid base64 PEM content", ex);
		}

		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
			throw new IllegalStateException("app.jwt.public-key at " + describe(publicKeyResource)
					+ " is not a valid X.509/SPKI RSA public key", ex);
		}
	}

	private static String describe(Resource resource) {
		return resource == null ? "<none configured>" : resource.getDescription();
	}

}
