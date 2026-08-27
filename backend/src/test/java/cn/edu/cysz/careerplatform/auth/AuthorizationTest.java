package cn.edu.cysz.careerplatform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.edu.cysz.careerplatform.auth.IdentityProvider.AuthenticatedIdentity;
import cn.edu.cysz.careerplatform.user.UserAccount;
import cn.edu.cysz.careerplatform.user.UserAccountRepository;
import cn.edu.cysz.careerplatform.user.UserRole;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import(AuthorizationTest.TestEndpoints.class)
class AuthorizationTest {

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
	JwtTokenService tokens;

	@Autowired
	UserAccountRepository users;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	EntityManager entityManager;

	private UserAccount student;
	private UserAccount teacher;
	private UserAccount admin;

	@BeforeEach
	void setUpUsers() {
		users.deleteAll();
		student = users.save(UserAccount.create("student", "hash", "张同学", UserRole.STUDENT));
		teacher = users.save(UserAccount.create("teacher", "hash", "王老师", UserRole.TEACHER));
		admin = users.save(UserAccount.create("admin", "hash", "管理员", UserRole.ADMIN));
	}

	@Test
	void allowsOnlyTheMatchingRoleForEachProtectedArea() throws Exception {
		mvc.perform(get("/api/v1/student/ping").header(HttpHeaders.AUTHORIZATION, bearer(student)))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/teacher/ping").header(HttpHeaders.AUTHORIZATION, bearer(teacher)))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/admin/ping").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsCrossRoleRequestsWithTheForbiddenEnvelopeAndCurrentTrace() throws Exception {
		String traceId = "e6d9f77d-9520-4f64-b65c-0abef2721e5b";

		for (RoleRequest request : new RoleRequest[] {
				new RoleRequest("/api/v1/teacher/ping", student), new RoleRequest("/api/v1/admin/ping", student),
				new RoleRequest("/api/v1/student/ping", teacher), new RoleRequest("/api/v1/admin/ping", teacher),
				new RoleRequest("/api/v1/student/ping", admin), new RoleRequest("/api/v1/teacher/ping", admin) }) {
			mvc.perform(get(request.path()).header(HttpHeaders.AUTHORIZATION, bearer(request.user()))
					.header("X-Trace-Id", traceId))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("FORBIDDEN"))
					.andExpect(jsonPath("$.traceId").value(traceId));
		}
	}

	@Test
	void returnsTheAuthenticatedCurrentUserWithoutAcceptingAnArbitraryId() throws Exception {
		mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(teacher)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(teacher.getId().toString()))
				.andExpect(jsonPath("$.data.username").value("teacher"))
				.andExpect(jsonPath("$.data.displayName").value("王老师"))
				.andExpect(jsonPath("$.data.role").value("TEACHER"));
	}

	@Test
	void unauthenticatedAndMalformedCredentialsReturnSafe401Envelopes() throws Exception {
		String traceId = "e6d9f77d-9520-4f64-b65c-0abef2721e5b";
		for (String authorization : new String[] { null, "Basic abc", "Bearer", "Bearer not.a.jwt", "Bearer a, Bearer b" }) {
			var request = get("/api/v1/users/me").header("X-Trace-Id", traceId);
			if (authorization != null) {
				request.header(HttpHeaders.AUTHORIZATION, authorization);
			}
			mvc.perform(request)
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
					.andExpect(jsonPath("$.traceId").value(traceId));
		}
		mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(student))
				.header(HttpHeaders.AUTHORIZATION, bearer(teacher)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void rejectsExpiredWrongKeyWrongAlgorithmAndVersionMismatchedTokensWithoutLeakingThem() throws Exception {
		String expired = tokens.issueAccessToken(identity(student), 0, Instant.now().minusSeconds(16 * 60));
		String wrongKey = signedToken(student, "a-different-test-signing-key-that-is-at-least-32-bytes", JWSAlgorithm.HS256, 0);
		String wrongAlgorithm = signedToken(student, "a-64-byte-test-signing-key-that-is-long-enough-for-hs384-signing-000", JWSAlgorithm.HS384, 0);
		String wrongVersion = tokens.issueAccessToken(identity(student), 1, Instant.now());

		for (String token : new String[] { expired, wrongKey, wrongAlgorithm, wrongVersion }) {
			var result = mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
					.andReturn();
			assertThat(result.getResponse().getContentAsString()).doesNotContain(token, "SignedJWT", "Exception");
		}
	}

	@Test
	void rejectsTokensForDeletedUsersAndDoesNotCreateSessions() throws Exception {
		String token = bearer(student);
		users.delete(student);

		mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void rejectsTokensForUsersDisabledAfterTokenIssuance() throws Exception {
		String token = bearer(teacher);
		assertThat(jdbc.update("update user_account set status = 'DISABLED' where username = ?", teacher.getUsername()))
				.isEqualTo(1);
		entityManager.clear();

		mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void doesNotCreateOrReuseAnHttpSessionForBearerAuthentication() throws Exception {
		var authenticated = mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(authenticated.getRequest().getSession(false)).isNull();
		assertThat(authenticated.getResponse().getCookie("JSESSIONID")).isNull();

		mvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void keepsHealthAndExactAuthPostsPublicButDoesNotPermitOtherAuthRoutes() throws Exception {
		mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
		mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isOk());
		mvc.perform(get("/api/v1/auth/login")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/auth/other")).andExpect(status().isForbidden());
	}

	private String bearer(UserAccount user) {
		return "Bearer " + tokens.issueAccessToken(identity(user), user.getTokenVersion(), Instant.now());
	}

	private AuthenticatedIdentity identity(UserAccount user) {
		return new AuthenticatedIdentity(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
	}

	private String signedToken(UserAccount user, String secret, JWSAlgorithm algorithm, int tokenVersion) throws Exception {
		SignedJWT token = new SignedJWT(new JWSHeader(algorithm), new JWTClaimsSet.Builder()
				.subject(user.getId().toString()).claim("username", user.getUsername()).claim("role", user.getRole().name())
				.claim("tokenVersion", tokenVersion).issueTime(java.util.Date.from(Instant.now()))
				.expirationTime(java.util.Date.from(Instant.now().plusSeconds(60))).build());
		token.sign(new MACSigner(secret));
		return token.serialize();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestEndpoints {

		@Bean
		PingController pingController() {
			return new PingController();
		}
	}

	@RestController
	static class PingController {

		@GetMapping("/api/v1/student/ping")
		String student() { return "student"; }

		@GetMapping("/api/v1/teacher/ping")
		String teacher() { return "teacher"; }

		@GetMapping("/api/v1/admin/ping")
		String admin() { return "admin"; }
	}

	private record RoleRequest(String path, UserAccount user) {
	}
}
