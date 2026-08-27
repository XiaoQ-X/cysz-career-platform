package cn.edu.cysz.careerplatform.auth;

import java.time.Duration;
import java.util.Arrays;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.edu.cysz.careerplatform.auth.AuthService.AuthTokens;
import cn.edu.cysz.careerplatform.auth.IdentityProvider.AuthenticatedIdentity;
import cn.edu.cysz.careerplatform.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	static final String REFRESH_COOKIE = "career_refresh";
	private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";
	private static final Pattern GENERATED_REFRESH_TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");

	private final AuthService authService;
	private final AuthRequestPolicy requestPolicy;
	private final boolean refreshCookieSecure;

	@Autowired
	public AuthController(AuthService authService,
			AuthRequestPolicy requestPolicy,
			@Value("${app.auth.refresh-cookie-secure:true}") boolean refreshCookieSecure,
			@Value("${spring.profiles.active:}") String activeProfiles) {
		this.authService = authService;
		this.requestPolicy = requestPolicy;
		this.refreshCookieSecure = refreshCookieSecure;
		if (!refreshCookieSecure && !hasExplicitLocalProfile(activeProfiles)) {
			throw new IllegalStateException("Insecure refresh cookies require an explicit local profile");
		}
	}

	public AuthController(AuthService authService, boolean refreshCookieSecure, String activeProfiles) {
		this.authService = authService;
		this.requestPolicy = null;
		this.refreshCookieSecure = refreshCookieSecure;
		if (!refreshCookieSecure && !hasExplicitLocalProfile(activeProfiles)) {
			throw new IllegalStateException("Insecure refresh cookies require an explicit local profile");
		}
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletResponse response, HttpServletRequest servletRequest) {
		requestPolicy.enforce(servletRequest);
		AuthTokens tokens = authService.login(request.username(), request.password().toCharArray());
		setRefreshCookie(response, tokens.refreshToken(), AuthService.REFRESH_TOKEN_LIFETIME);
		return ApiResponse.of(toResponse(tokens), traceId(servletRequest));
	}

	@PostMapping("/refresh")
	public ApiResponse<AuthResponse> refresh(
			HttpServletResponse response, HttpServletRequest servletRequest) {
		requestPolicy.enforce(servletRequest);
		String refreshToken = readRefreshCookie(servletRequest);
		AuthTokens tokens = authService.refresh(refreshToken);
		setRefreshCookie(response, tokens.refreshToken(), AuthService.REFRESH_TOKEN_LIFETIME);
		return ApiResponse.of(toResponse(tokens), traceId(servletRequest));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(
			HttpServletResponse response, HttpServletRequest servletRequest) {
		requestPolicy.enforce(servletRequest);
		String refreshToken = null;
		try {
			refreshToken = readRefreshCookie(servletRequest);
		} catch (AuthService.InvalidRefreshTokenException ignored) {
			// Logout remains idempotent and still clears the cookie.
		}
		authService.logout(refreshToken);
		setRefreshCookie(response, "", Duration.ZERO);
		return ApiResponse.of(null, traceId(servletRequest));
	}

	private AuthResponse toResponse(AuthTokens tokens) {
		AuthenticatedIdentity identity = tokens.identity();
		return new AuthResponse(tokens.accessToken(),
				new UserResponse(identity.userId(), identity.username(), identity.displayName(), identity.role().name()));
	}

	private void setRefreshCookie(HttpServletResponse response, String value, Duration maxAge) {
		ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, value)
				.httpOnly(true)
				.secure(refreshCookieSecure)
				.sameSite("Strict")
				.path(REFRESH_COOKIE_PATH)
				.maxAge(maxAge)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private String traceId(HttpServletRequest request) {
		return (String) request.getAttribute("traceId");
	}

	private String readRefreshCookie(HttpServletRequest request) {
		jakarta.servlet.http.Cookie[] cookies = request.getCookies();
		String value = null;
		int matches = 0;
		if (cookies != null) {
			for (jakarta.servlet.http.Cookie cookie : cookies) {
				if (REFRESH_COOKIE.equals(cookie.getName())) {
					matches++;
					value = cookie.getValue();
				}
			}
		}
		if (matches != 1 || value == null || !GENERATED_REFRESH_TOKEN.matcher(value).matches()) {
			if (matches == 0) {
				return null;
			}
			throw new AuthService.InvalidRefreshTokenException();
		}
		return value;
	}

	private boolean hasExplicitLocalProfile(String activeProfiles) {
		return Arrays.stream(activeProfiles.split(","))
				.map(String::trim)
				.anyMatch(profile -> profile.equals("local") || profile.equals("test") || profile.equals("e2e"));
	}

	public record LoginRequest(@NotBlank @Size(max = 64) String username,
			@NotBlank @Size(max = 4096) String password) {
	}

	public record AuthResponse(String accessToken, UserResponse user) {
	}

	public record UserResponse(java.util.UUID id, String username, String displayName, String role) {
	}
}
