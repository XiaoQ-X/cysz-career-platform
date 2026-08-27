package cn.edu.cysz.careerplatform.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import cn.edu.cysz.careerplatform.user.UserAccount;
import cn.edu.cysz.careerplatform.user.UserAccountRepository;
import cn.edu.cysz.careerplatform.user.UserRole;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final byte[] signingSecret;
	private final UserAccountRepository users;

	public JwtAuthenticationFilter(@Value("${app.auth.jwt-secret}") String signingSecret,
			UserAccountRepository users) {
		this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
		this.users = users;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authorization = singleAuthorizationHeader(request);
		if (authorization == null) {
			filterChain.doFilter(request, response);
			return;
		}
		AuthenticatedUser principal = authenticate(authorization);
		if (principal == null) {
			SecurityErrorResponses.unauthenticated(request, response);
			return;
		}
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))));
		try {
			filterChain.doFilter(request, response);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	private String singleAuthorizationHeader(HttpServletRequest request) {
		var headers = request.getHeaders(HttpHeaders.AUTHORIZATION);
		if (!headers.hasMoreElements()) {
			return null;
		}
		String header = headers.nextElement();
		if (headers.hasMoreElements() || header == null || !header.startsWith("Bearer ")
				|| header.length() == "Bearer ".length() || header.indexOf(',') >= 0) {
			return "";
		}
		return header;
	}

	private AuthenticatedUser authenticate(String authorization) {
		try {
			String rawToken = authorization.substring("Bearer ".length());
			if (rawToken.chars().anyMatch(Character::isWhitespace)) {
				return null;
			}
			SignedJWT token = SignedJWT.parse(rawToken);
			if (!JWSAlgorithm.HS256.equals(token.getHeader().getAlgorithm()) || !token.verify(new MACVerifier(signingSecret))) {
				return null;
			}
			var claims = token.getJWTClaimsSet();
			if (claims.getExpirationTime() == null || !claims.getExpirationTime().toInstant().isAfter(Instant.now())) {
				return null;
			}
			UUID id = canonicalUuid(claims.getSubject());
			String username = claims.getStringClaim("username");
			UserRole role = UserRole.valueOf(claims.getStringClaim("role"));
			Integer tokenVersion = claims.getIntegerClaim("tokenVersion");
			if (id == null || username == null || username.isBlank() || tokenVersion == null || tokenVersion < 0) {
				return null;
			}
			UserAccount user = users.findById(id).orElse(null);
			if (user == null || !user.isActive() || !user.getUsername().equals(username) || user.getRole() != role
					|| user.getTokenVersion() != tokenVersion) {
				return null;
			}
			return new AuthenticatedUser(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole(),
					user.getTokenVersion());
		} catch (Exception ignored) {
			return null;
		}
	}

	private UUID canonicalUuid(String value) {
		try {
			UUID id = UUID.fromString(value);
			return id.toString().equals(value) ? id : null;
		} catch (Exception ignored) {
			return null;
		}
	}
}
