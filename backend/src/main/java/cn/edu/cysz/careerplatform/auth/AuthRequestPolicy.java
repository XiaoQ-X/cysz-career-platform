package cn.edu.cysz.careerplatform.auth;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthRequestPolicy {

	private final Set<String> trustedOrigins;

	public AuthRequestPolicy(@Value("${app.auth.trusted-origins}") String configuredOrigins) {
		if (configuredOrigins == null || configuredOrigins.isBlank()) {
			throw new IllegalStateException("At least one trusted auth origin is required");
		}
		this.trustedOrigins = Arrays.stream(configuredOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.peek(this::validateOrigin)
				.collect(Collectors.toUnmodifiableSet());
		if (trustedOrigins.isEmpty()) {
			throw new IllegalStateException("At least one trusted auth origin is required");
		}
	}

	public void enforce(HttpServletRequest request) {
		String origin = request.getHeader("Origin");
		String fetchSite = request.getHeader("Sec-Fetch-Site");
		if (origin != null && !trustedOrigins.contains(origin)) {
			throw new RequestRejectedException();
		}
		if ("cross-site".equalsIgnoreCase(fetchSite)) {
			throw new RequestRejectedException();
		}
	}

	private void validateOrigin(String origin) {
		if (origin.contains("*") || origin.equalsIgnoreCase("null")) {
			throw new IllegalStateException("Wildcard or null auth origins are not allowed");
		}
		URI uri;
		try {
			uri = URI.create(origin);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException("Invalid trusted auth origin", exception);
		}
		if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
				|| uri.getHost() == null || uri.getPath() != null && !uri.getPath().isEmpty()
				|| uri.getQuery() != null || uri.getFragment() != null || uri.getUserInfo() != null) {
			throw new IllegalStateException("Invalid trusted auth origin");
		}
	}

	public static final class RequestRejectedException extends RuntimeException {

		public RequestRejectedException() {
			super("Authentication request rejected");
		}
	}
}
