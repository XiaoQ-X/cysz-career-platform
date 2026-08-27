package cn.edu.cysz.careerplatform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

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
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import cn.edu.cysz.careerplatform.user.UserAccount;
import cn.edu.cysz.careerplatform.user.UserAccountRepository;
import cn.edu.cysz.careerplatform.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = "app.auth.jwt-secret=task4-test-signing-secret-32-bytes-minimum")
@Import(AuthControllerTest.TestAccountsConfiguration.class)
class AuthControllerTest {

	private static final String REFRESH_COOKIE = "career_refresh";
	private static final String JWT_SECRET = "task4-test-signing-secret-32-bytes-minimum";

	@Container
	static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
	}

	@Autowired
	MockMvc mvc;

	@Autowired
	RefreshSessionRepository refreshSessions;

	@Autowired
	UserAccountRepository users;

	@Autowired
	Clock clock;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Test
	void loginReturnsAccessTokenAndHttpOnlyRefreshCookie() throws Exception {
		MvcResult result = login("student", "Student123!");

		assertThat(result.getResponse().getContentAsString()).doesNotContain(cookieValue(result));
		mvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("{\"username\":\"student\",\"password\":\"Student123!\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.expiresInSeconds").value(900))
			.andExpect(jsonPath("$.data.user.role").value("STUDENT"))
			.andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
			.andExpect(cookie().sameSite(REFRESH_COOKIE, "Strict"))
			.andExpect(cookie().secure(REFRESH_COOKIE, true))
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
				.andExpect(jsonPath("$.data.expiresInSeconds").value(900))
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

	@Test
	void acceptsAnExplicitTrustedBrowserOrigin() throws Exception {
		mvc.perform(post("/api/v1/auth/login")
				.header("Origin", "http://localhost:3000")
				.header("Sec-Fetch-Site", "same-site")
				.contentType(APPLICATION_JSON)
				.content("{\"username\":\"student\",\"password\":\"Student123!\"}"))
			.andExpect(status().isOk());
	}

	@Test
	void rejectsCrossSiteOriginBeforeCreatingASession() throws Exception {
		long sessionsBefore = refreshSessions.count();

		mvc.perform(post("/api/v1/auth/login")
				.header("Origin", "https://evil.example.test")
				.header("Sec-Fetch-Site", "cross-site")
				.contentType(APPLICATION_JSON)
				.content("{\"username\":\"student\",\"password\":\"Student123!\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("AUTH_REQUEST_REJECTED"))
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

		assertThat(refreshSessions.count()).isEqualTo(sessionsBefore);
	}

	@Test
	void rejectsAnUntrustedSameSiteSiblingOriginBeforeCreatingASession() throws Exception {
		long sessionsBefore = refreshSessions.count();

		mvc.perform(post("/api/v1/auth/login")
				.header("Origin", "https://evil.example.test")
				.header("Sec-Fetch-Site", "same-site")
				.contentType(APPLICATION_JSON)
				.content("{\"username\":\"student\",\"password\":\"Student123!\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("AUTH_REQUEST_REJECTED"))
			.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

		assertThat(refreshSessions.count()).isEqualTo(sessionsBefore);
	}

	@Test
	void securityConfigurationPermitsOnlyTheThreePostAuthRoutes() throws Exception {
		mvc.perform(get("/api/v1/auth/login"))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/auth/other"))
				.andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/auth/login/extra"))
				.andExpect(status().isForbidden());
	}

	@Test
	void malformedRefreshTokensAreRejectedBeforeSessionLookupOrMutation() throws Exception {
		long sessionsBefore = refreshSessions.count();

		mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie("too-short")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
		mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie("A".repeat(44))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

		assertThat(refreshSessions.count()).isEqualTo(sessionsBefore);
	}

	@Test
	void duplicateRefreshCookiesAreRejectedDeterministically() throws Exception {
		long sessionsBefore = refreshSessions.count();
		String valid = validRefreshFixture();

		mvc.perform(post("/api/v1/auth/refresh")
				.cookie(refreshCookie(valid), refreshCookie(valid)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

		assertThat(refreshSessions.count()).isEqualTo(sessionsBefore);
	}

	@Test
	void logoutRemainsIdempotentAndClearsCookieForMalformedInput() throws Exception {
		long sessionsBefore = refreshSessions.count();

		mvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie("not-a-refresh-token")))
				.andExpect(status().isOk())
				.andExpect(cookie().maxAge(REFRESH_COOKIE, 0))
				.andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
				.andExpect(cookie().sameSite(REFRESH_COOKIE, "Strict"));

		assertThat(refreshSessions.count()).isEqualTo(sessionsBefore);

		mvc.perform(post("/api/v1/auth/logout")
				.cookie(refreshCookie(validRefreshFixture()), refreshCookie(validRefreshFixture())))
				.andExpect(status().isOk())
				.andExpect(cookie().maxAge(REFRESH_COOKIE, 0));
		assertThat(refreshSessions.count()).isEqualTo(sessionsBefore);
	}

	@Test
	void rejectsUsernameLongerThanTheDatabaseColumnBeforeCreatingASession() throws Exception {
		long sessionsBefore = refreshSessions.count();

		mvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("{\"username\":\"" + "u".repeat(65) + "\",\"password\":\"Student123!\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

		assertThat(refreshSessions.count()).isEqualTo(sessionsBefore);
	}

	@Test
	void rejectsOnlyUnreasonablyLargePasswordsWithoutAddingANarrowPasswordPolicy() throws Exception {
		long sessionsBefore = refreshSessions.count();

		mvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content("{\"username\":\"student\",\"password\":\"" + "p".repeat(4097) + "\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

		assertThat(refreshSessions.count()).isEqualTo(sessionsBefore);
	}

	@Test
	void insecureCookiesCannotBeConfiguredOutsideAnExplicitLocalProfile() {
		assertThatThrownBy(() -> new AuthController(null, false, "prod"))
				.isInstanceOf(IllegalStateException.class);
		new AuthController(null, false, "local");
	}

	@Test
	void insecureCookiesCannotBeConfiguredWithAProductionAndLocalProfile() {
		assertThatThrownBy(() -> new AuthController(null, false, "prod,local"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void insecureCookiesCannotBeConfiguredWithoutAnActiveProfile() {
		assertThatThrownBy(() -> new AuthController(null, false, ""))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void insecureCookiesRequireEveryActiveProfileToBeAnExactApprovedName() {
		new AuthController(null, false, "test");
		new AuthController(null, false, "e2e");
		new AuthController(null, false, "local,test,e2e");

		assertThatThrownBy(() -> new AuthController(null, false, "production"))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new AuthController(null, false, "production,local"))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new AuthController(null, false, "LOCAL"))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new AuthController(null, false, "local-dev"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void concurrentRefreshAttemptsAllowExactlyOneSingleUseRotation() throws Exception {
		String username = "race-" + UUID.randomUUID();
		UserAccount user = users.save(UserAccount.create(username, passwordEncoder.encode("Race123!"), "Race User", UserRole.STUDENT));
		String oldRaw = cookieValue(login(username, "Race123!"));
		CountDownLatch holderLocked = new CountDownLatch(1);
		CountDownLatch releaseHolder = new CountDownLatch(1);
		CountDownLatch workersStarted = new CountDownLatch(2);
		ExecutorService executor = Executors.newFixedThreadPool(3);
		Future<?> holder = executor.submit(() -> holdRefreshSessionLock(sha256(oldRaw), holderLocked, releaseHolder));

		try {
			assertThat(holderLocked.await(5, TimeUnit.SECONDS)).isTrue();
			Future<MvcResult> first = executor.submit(() -> concurrentRefresh(workersStarted, oldRaw));
			Future<MvcResult> second = executor.submit(() -> concurrentRefresh(workersStarted, oldRaw));
			assertThat(workersStarted.await(5, TimeUnit.SECONDS)).isTrue();
			awaitCondition(() -> refreshSessionLockWaitCount() >= 2,
					"both refresh workers to wait for the held refresh-session row lock");

			releaseHolder.countDown();
			holder.get(5, TimeUnit.SECONDS);
			MvcResult firstResult = first.get(10, TimeUnit.SECONDS);
			MvcResult secondResult = second.get(10, TimeUnit.SECONDS);
			int successful = (firstResult.getResponse().getStatus() == 200 ? 1 : 0)
					+ (secondResult.getResponse().getStatus() == 200 ? 1 : 0);

			assertThat(successful).isEqualTo(1);
			MvcResult winner = firstResult.getResponse().getStatus() == 200 ? firstResult : secondResult;
			MvcResult loser = firstResult.getResponse().getStatus() == 401 ? firstResult : secondResult;
			assertThat(loser.getResponse().getStatus()).isEqualTo(401);
			assertThat(loser.getResponse().getContentAsString()).contains("INVALID_REFRESH_TOKEN");
			assertThat(refreshSessions.findByTokenHash(sha256(oldRaw))).get()
					.extracting(RefreshSession::getRevokedAt).isNotNull();
			assertThat(refreshSessions.countByUserIdAndRevokedAtIsNullAndExpiresAtAfter(user.getId(), Instant.now(clock)))
					.isEqualTo(1);

			mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(cookieValue(winner))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.accessToken").isNotEmpty());
		} finally {
			releaseHolder.countDown();
			executor.shutdownNow();
		}
	}

	private void holdRefreshSessionLock(String tokenHash, CountDownLatch holderLocked,
			CountDownLatch releaseHolder) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			refreshSessions.findByTokenHashForUpdate(tokenHash).orElseThrow();
			holderLocked.countDown();
			try {
				assertThat(releaseHolder.await(10, TimeUnit.SECONDS)).isTrue();
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new AssertionError("Interrupted while holding the refresh-session row lock", exception);
			}
		});
	}

	private MvcResult concurrentRefresh(CountDownLatch workersStarted, String raw) throws Exception {
		workersStarted.countDown();
		return mvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie(raw))).andReturn();
	}

	private void awaitCondition(BooleanSupplier condition, String description) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!condition.getAsBoolean()) {
			if (System.nanoTime() >= deadline) {
				throw new AssertionError("Timed out waiting for " + description);
			}
			Thread.onSpinWait();
		}
	}

	private int refreshSessionLockWaitCount() {
		String query = """
				select count(*)
				from performance_schema.data_lock_waits waits
				join performance_schema.data_locks requested
				  on requested.engine_lock_id = waits.requesting_engine_lock_id
				where requested.object_schema = database()
				  and requested.object_name = 'refresh_session'
				""";
		try (var connection = DriverManager.getConnection(mysql.getJdbcUrl(), "root", mysql.getPassword());
				var statement = connection.createStatement();
				var result = statement.executeQuery(query)) {
			result.next();
			return result.getInt(1);
		} catch (Exception exception) {
			throw new AssertionError("Unable to inspect MySQL refresh-session lock waits", exception);
		}
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

	private String validRefreshFixture() {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(
				"task-4-valid-refresh-fixture-32!".getBytes(StandardCharsets.UTF_8));
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
