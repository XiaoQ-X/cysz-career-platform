package cn.edu.cysz.careerplatform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import jakarta.servlet.http.Cookie;

import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import cn.edu.cysz.careerplatform.user.UserAccount;
import cn.edu.cysz.careerplatform.user.UserAccountRepository;
import cn.edu.cysz.careerplatform.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.auth.jwt-secret=task4-test-signing-secret-32-bytes-minimum")
@Import(AuthControllerTest.TestAccountsConfiguration.class)
class AuthControllerTest {

	private static final String REFRESH_COOKIE = "career_refresh";
	private static final String JWT_SECRET = "task4-test-signing-secret-32-bytes-minimum";

	@Autowired
	MockMvc mvc;

	@Autowired
	RefreshSessionRepository refreshSessions;

	@Autowired
	UserAccountRepository users;

	@Autowired
	Clock clock;

	@Test
	void loginReturnsAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
		MvcResult result = login("student", "Student123!");

		assertThat(result.getResponse().getContentAsString()).doesNotContain(cookieValue(result));
		mvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("{\"username\":\"student\",\"password\":\"Student123!\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.user.role").value("STUDENT"))
			.andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
			.andExpect(cookie().sameSite(REFRESH_COOKIE, "Strict"))
			.andExpect(cookie().path(REFRESH_COOKIE, "/api/v1/auth"))
			.andExpect(cookie().maxAge(REFRESH_COOKIE, 604800));
	}

	@Test
	void missingAndWrongPasswordsReturnTheSameSafeCredentialsError() throws Exception {
		MvcResult wrongPassword = mvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("{\"username\":\"student\",\"password\":\"wrong\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.message").value("Invalid credentials"))
			.andReturn();

		MvcResult missingUser = mvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("{\"username\":\"missing\",\"password\":\"wrong\"}"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
			.andExpect(jsonPath("$.message").value("Invalid credentials"))
			.andReturn();

		assertThat(wrongPassword.getResponse().getContentAsString())
				.doesNotContain("student", "wrong", "password");
		assertThat(missingUser.getResponse().getContentAsString())
				.doesNotContain("missing", "wrong", "password");
	}

	@Test
	void accessTokenHasTheExactClaimsAndFifteenMinuteLifetime() throws Exception {
		MvcResult result = login("student", "Student123!");
		String accessToken = jsonValue(result, "$.data.accessToken");
		SignedJWT jwt = SignedJWT.parse(accessToken);

		assertThat(jwt.verify(new MACVerifier(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))).isTrue();
		assertThat(jwt.getJWTClaimsSet().getClaims().keySet())
				.containsExactlyInAnyOrder("sub", "username", "role", "tokenVersion", "iat", "exp");
		assertThat(jwt.getJWTClaimsSet().getSubject())
				.isEqualTo(users.findByUsernameIgnoreCase("student").orElseThrow().getId().toString());
		assertThat(jwt.getJWTClaimsSet().getStringClaim("username")).isEqualTo("student");
		assertThat(jwt.getJWTClaimsSet().getStringClaim("role")).isEqualTo("STUDENT");
		assertThat(jwt.getJWTClaimsSet().getIntegerClaim("tokenVersion")).isZero();
		assertThat(jwt.getJWTClaimsSet().getExpirationTime().getTime()
				- jwt.getJWTClaimsSet().getIssueTime().getTime()).isEqualTo(900_000L);
	}

	@Test
	void refreshRotatesTheCookieAndPersistsOnlyItsSha256Hash() throws Exception {
		MvcResult login = login("student", "Student123!");
		String oldRaw = cookieValue(login);

		MvcResult refresh = mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(oldRaw)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
				.andExpect(cookie().sameSite(REFRESH_COOKIE, "Strict"))
				.andReturn();
		String newRaw = cookieValue(refresh);

		assertThat(newRaw).isNotEqualTo(oldRaw);
		assertThat(refresh.getResponse().getContentAsString()).doesNotContain(oldRaw, newRaw);
		assertThat(refreshSessions.findByTokenHash(sha256(oldRaw))).get()
				.extracting(RefreshSession::getRevokedAt).isNotNull();
		assertThat(refreshSessions.findByTokenHash(sha256(newRaw))).get()
				.extracting(RefreshSession::getTokenHash)
				.isEqualTo(sha256(newRaw));
	}

	@Test
	void replayedRefreshFailsWhileTheRotatedTokenStillWorks() throws Exception {
		String oldRaw = cookieValue(login("student", "Student123!"));
		MvcResult firstRefresh = mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(oldRaw)))
				.andExpect(status().isOk())
				.andReturn();
		String newRaw = cookieValue(firstRefresh);

		mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(oldRaw)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
		mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(newRaw)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty());
	}

	@Test
	void expiredRefreshFailsWithoutExposingTheRawToken() throws Exception {
		UserAccount user = users.findByUsernameIgnoreCase("student").orElseThrow();
		String raw = "expired-refresh-fixture-" + UUID.randomUUID();
		refreshSessions.save(RefreshSession.create(user.getId(), sha256(raw),
				Instant.now(clock).minusSeconds(1), Instant.now(clock).minusSeconds(2)));

		MvcResult result = mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(raw)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).doesNotContain(raw);
	}

	@Test
	void logoutRevokesTheSessionAndClearsTheCookie() throws Exception {
		String raw = cookieValue(login("student", "Student123!"));

		mvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie(raw)))
				.andExpect(status().isOk())
				.andExpect(cookie().maxAge(REFRESH_COOKIE, 0))
				.andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
				.andExpect(cookie().sameSite(REFRESH_COOKIE, "Strict"));

		assertThat(refreshSessions.findByTokenHash(sha256(raw))).get()
				.extracting(RefreshSession::getRevokedAt).isNotNull();
		mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(raw)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	private MvcResult login(String username, String password) throws Exception {
		return mvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
			.andExpect(status().isOk())
			.andReturn();
	}

	private String cookieValue(MvcResult result) {
		Cookie cookie = result.getResponse().getCookie(REFRESH_COOKIE);
		assertThat(cookie).isNotNull();
		return cookie.getValue();
	}

	private Cookie refreshCookie(String value) {
		return new Cookie(REFRESH_COOKIE, value);
	}

	private String jsonValue(MvcResult result, String path) throws Exception {
		return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), path);
	}

	private String sha256(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new AssertionError(exception);
		}
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestAccountsConfiguration {

		@Bean
		CommandLineRunner testAccounts(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
			return args -> {
				seed(repository, passwordEncoder, "student", "Student123!", "张同学", UserRole.STUDENT);
				seed(repository, passwordEncoder, "teacher", "Teacher123!", "王老师", UserRole.TEACHER);
				seed(repository, passwordEncoder, "admin", "Admin123!", "管理员", UserRole.ADMIN);
			};
		}

		private void seed(UserAccountRepository repository, PasswordEncoder passwordEncoder,
				String username, String password, String displayName, UserRole role) {
			if (repository.findByUsernameIgnoreCase(username).isPresent()) {
				return;
			}
			repository.save(UserAccount.create(username, passwordEncoder.encode(password), displayName, role));
		}
	}
}
