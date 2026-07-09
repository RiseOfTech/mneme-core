package io.github.ifamneme.mneme_core.domain;

import java.util.Objects;

public final class Image {

	private final String url;

	public Image(String url) {
		Objects.requireNonNull(url, "url must not be null");
		if (url.isBlank()) {
			throw new IllegalArgumentException("url must not be blank");
		}
		this.url = url;
	}

	public String url() {
		return url;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Image other)) {
			return false;
		}
		return url.equals(other.url);
	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}

	@Override
	public String toString() {
		return url;
	}

}
