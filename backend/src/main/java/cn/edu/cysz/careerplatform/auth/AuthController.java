package cn.edu.cysz.careerplatform.auth;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
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

	private final AuthService authService;
	private final boolean refreshCookieSecure;

	public AuthController(AuthService authService,
			@Value("${app.auth.refresh-cookie-secure:true}") boolean refreshCookieSecure) {
		this.authService = authService;
		this.refreshCookieSecure = refreshCookieSecure;
	}

	@PostMapping("/login")
	public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletResponse response, HttpServletRequest servletRequest) {
		AuthTokens tokens = authService.login(request.username(), request.password().toCharArray());
		setRefreshCookie(response, tokens.refreshToken(), AuthService.REFRESH_TOKEN_LIFETIME);
		return ApiResponse.of(toResponse(tokens), traceId(servletRequest));
	}

	@PostMapping("/refresh")
	public ApiResponse<AuthResponse> refresh(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
			HttpServletResponse response, HttpServletRequest servletRequest) {
		AuthTokens tokens = authService.refresh(refreshToken);
		setRefreshCookie(response, tokens.refreshToken(), AuthService.REFRESH_TOKEN_LIFETIME);
		return ApiResponse.of(toResponse(tokens), traceId(servletRequest));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
			HttpServletResponse response, HttpServletRequest servletRequest) {
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

	public record LoginRequest(@NotBlank String username, @NotBlank String password) {
	}

	public record AuthResponse(String accessToken, UserResponse user) {
	}

	public record UserResponse(java.util.UUID id, String username, String displayName, String role) {
	}
}
