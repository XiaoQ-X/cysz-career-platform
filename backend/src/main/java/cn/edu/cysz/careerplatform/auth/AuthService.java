package cn.edu.cysz.careerplatform.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.edu.cysz.careerplatform.auth.IdentityProvider.AuthenticatedIdentity;
import cn.edu.cysz.careerplatform.user.UserAccount;
import cn.edu.cysz.careerplatform.user.UserAccountRepository;

@Service
public class AuthService {

	public static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(7);

	private final IdentityProvider identityProvider;
	private final UserAccountRepository users;
	private final RefreshSessionRepository refreshSessions;
	private final JwtTokenService jwtTokenService;
	private final Clock clock;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(IdentityProvider identityProvider, UserAccountRepository users,
			RefreshSessionRepository refreshSessions, JwtTokenService jwtTokenService, Clock clock) {
		this.identityProvider = identityProvider;
		this.users = users;
		this.refreshSessions = refreshSessions;
		this.jwtTokenService = jwtTokenService;
		this.clock = clock;
	}

	@Transactional
	public AuthTokens login(String username, char[] password) {
		try {
			AuthenticatedIdentity identity = identityProvider.authenticate(username, password);
			UserAccount user = users.findById(identity.userId())
					.filter(UserAccount::isActive)
					.orElseThrow(IdentityProvider.InvalidCredentialsException::new);
			return issueTokens(user, identity, clock.instant());
		} finally {
			if (password != null) {
				Arrays.fill(password, '\0');
			}
		}
	}

	@Transactional
	public AuthTokens refresh(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}

		String tokenHash = sha256(rawRefreshToken);
		RefreshSession current = refreshSessions.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(InvalidRefreshTokenException::new);
		Instant now = clock.instant();
		if (!current.isUsableAt(now)) {
			throw new InvalidRefreshTokenException();
		}

		UserAccount user = users.findById(current.getUserId())
				.filter(UserAccount::isActive)
				.orElseThrow(InvalidRefreshTokenException::new);
		AuthenticatedIdentity identity = new AuthenticatedIdentity(
				user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
		AuthTokens replacement = issueTokens(user, identity, now);

		current.revoke(now);
		refreshSessions.save(current);
		return replacement;
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			return;
		}
		refreshSessions.findByTokenHashForUpdate(sha256(rawRefreshToken))
				.ifPresent(session -> session.revoke(clock.instant()));
	}

	private AuthTokens issueTokens(UserAccount user, AuthenticatedIdentity identity, Instant now) {
		String rawRefreshToken = newRefreshToken();
		RefreshSession session = RefreshSession.create(user.getId(), sha256(rawRefreshToken),
				now.plus(REFRESH_TOKEN_LIFETIME), now);
		refreshSessions.save(session);
		String accessToken = jwtTokenService.issueAccessToken(identity, user.getTokenVersion(), now);
		return new AuthTokens(accessToken, identity, rawRefreshToken);
	}

	private String newRefreshToken() {
		byte[] token = new byte[32];
		secureRandom.nextBytes(token);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
	}

	private String sha256(String rawToken) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to hash refresh token", exception);
		}
	}

	public record AuthTokens(String accessToken, AuthenticatedIdentity identity, String refreshToken) {
	}

	public static final class InvalidRefreshTokenException extends RuntimeException {

		public InvalidRefreshTokenException() {
			super("Invalid refresh token");
		}
	}
}
