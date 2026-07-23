package io.github.ifamneme.mneme_core.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Mints signed test JWTs matching the mneme-auth token contract (docs/specs/jwt-auth.md)
 * so integration tests never need a static seeded token. Two independent key pairs exist:
 * {@link #SIGNING_KEY_PAIR} is the one the app is configured to trust ({@link #publicKeyLocation()}
 * goes into {@code app.jwt.public-key}); {@link #OTHER_KEY_PAIR} signs tokens the app must
 * reject as coming from an untrusted key.
 */
public final class JwtTestTokens {

	public static final String ISSUER = "mneme-auth";

	public static final String AUDIENCE = "mneme-core";

	public static final KeyPair SIGNING_KEY_PAIR = generateKeyPair();

	public static final KeyPair OTHER_KEY_PAIR = generateKeyPair();

	// app.jwt.public-key now binds to a Resource, not raw PEM text, so tests need a real
	// location to point at - the signing key pair is freshly generated per JVM run, so
	// this writes it out once to a temp file rather than a fixed classpath resource.
	private static final Path PUBLIC_KEY_FILE = writePublicKeyToTempFile();

	private JwtTestTokens() {
	}

	public static String publicKeyLocation() {
		return "file:" + PUBLIC_KEY_FILE.toAbsolutePath();
	}

	public static String validToken(UUID subject) {
		Instant now = Instant.now();
		return token(SIGNING_KEY_PAIR, ISSUER, AUDIENCE, subject.toString(), now, now.plus(15, ChronoUnit.MINUTES));
	}

	public static String expiredToken(UUID subject) {
		Instant now = Instant.now();
		return token(SIGNING_KEY_PAIR, ISSUER, AUDIENCE, subject.toString(), now.minus(20, ChronoUnit.MINUTES),
				now.minus(5, ChronoUnit.MINUTES));
	}

	public static String wrongIssuerToken(UUID subject) {
		Instant now = Instant.now();
		return token(SIGNING_KEY_PAIR, "someone-else", AUDIENCE, subject.toString(), now, now.plus(15, ChronoUnit.MINUTES));
	}

	public static String wrongAudienceToken(UUID subject) {
		Instant now = Instant.now();
		return token(SIGNING_KEY_PAIR, ISSUER, "someone-else", subject.toString(), now, now.plus(15, ChronoUnit.MINUTES));
	}

	public static String signedByOtherKeyToken(UUID subject) {
		Instant now = Instant.now();
		return token(OTHER_KEY_PAIR, ISSUER, AUDIENCE, subject.toString(), now, now.plus(15, ChronoUnit.MINUTES));
	}

	private static String token(KeyPair keyPair, String issuer, String audience, String subject, Instant issuedAt,
			Instant expiresAt) {
		try {
			JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key-1").build();
			JWTClaimsSet claims = new JWTClaimsSet.Builder().issuer(issuer)
					.subject(subject)
					.audience(audience)
					.issueTime(java.util.Date.from(issuedAt))
					.expirationTime(java.util.Date.from(expiresAt))
					.build();

			SignedJWT signedJwt = new SignedJWT(header, claims);
			signedJwt.sign(new RSASSASigner(keyPair.getPrivate()));
			return signedJwt.serialize();
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Failed to sign test JWT", ex);
		}
	}

	private static KeyPair generateKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("RSA not available", ex);
		}
	}

	private static Path writePublicKeyToTempFile() {
		try {
			Path path = Files.createTempFile("mneme-auth-test-public-key", ".pem");
			path.toFile().deleteOnExit();
			Files.writeString(path, toPem((RSAPublicKey) SIGNING_KEY_PAIR.getPublic()));
			return path;
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to write test public key to a temp file", ex);
		}
	}

	private static String toPem(RSAPublicKey publicKey) {
		String base64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
		StringBuilder pem = new StringBuilder("-----BEGIN PUBLIC KEY-----\n");
		for (int i = 0; i < base64.length(); i += 64) {
			pem.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
		}
		pem.append("-----END PUBLIC KEY-----\n");
		return pem.toString();
	}

}
