package io.github.ifamneme.mneme_core.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

	private Resource publicKey;

	public Resource getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(Resource publicKey) {
		this.publicKey = publicKey;
	}

}
