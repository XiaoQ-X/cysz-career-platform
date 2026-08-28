# Task 4 Report: Foundation Auth Homepage

## Takeover state

Task 4 was taken over in the existing worktree after the prior implementer was interrupted. The preserved RED-stage changes were retained: `backend/pom.xml` already contained Nimbus JOSE JWT and `AuthControllerTest.java` already contained the initial login test. No prior production auth files, Task 4 report, or Task 4 commit existed.

The unrequested duplicate plan artifact was removed after scope correction. No planning document was created or modified afterward. The `.superpowers/sdd/2026-08-27-foundation-auth-homepage/task-4-brief.md` was treated as the sole requirements source.

## Implementation

- Added Flyway V2 `refresh_session` table with UUID id/user id, unique SHA-256 token hash, expiry, revocation, creation time, user foreign key, and user index.
- Added `IdentityProvider.authenticate(String, char[])` and `AuthenticatedIdentity`; `LocalIdentityProvider` uses the real BCrypt `PasswordEncoder` and a BCrypt dummy hash so missing users and wrong passwords take the same public invalid-credentials path.
- Added JWT service using Nimbus HS256, exact `sub`, `username`, `role`, `tokenVersion`, `iat`, and `exp` claims, and a 15-minute lifetime.
- Added `AuthService` login, refresh, and logout operations. Refresh values are 32 random bytes, encoded only for the cookie; only lowercase SHA-256 hex is persisted. Refresh rotation is transactional and locks the current row pessimistically before validating, revoking, and replacing it.
- Added `AuthController` endpoints at `/api/v1/auth/login`, `/refresh`, and `/logout`. `career_refresh` is HttpOnly, SameSite Strict, Secure by default, path-scoped to `/api/v1/auth`, and has a seven-day lifetime; logout emits the same cookie with Max-Age 0.
- Added safe `INVALID_CREDENTIALS` and `INVALID_REFRESH_TOKEN` API errors while preserving trace IDs and the existing safe unexpected-error handler.
- Added an explicit test-profile signing key; production configuration requires `JWT_SIGNING_KEY` and has no hardcoded production signing key.
- Added test-only BCrypt accounts for student, teacher, and admin; no default accounts were added to production migrations.
- Kept the security bridge temporary and replaceable: CSRF is disabled for stateless JSON auth, `/api/v1/auth/**` is permitted, and all other requests are denied until Task 5 supplies final authorization/JWT filtering. No Task 5 role authorization or JWT request filter was implemented.

## RED/GREEN evidence

Initial focused run first exposed an outdated Boot 4 test import; only the package import was corrected. With project MySQL started from `infra/compose.yaml`, the preserved test then reached the application and failed because `/api/v1/auth/login` did not exist. Before auth production code, the expanded test class compiled to the expected missing-auth API/type failures (`RefreshSessionRepository`, `RefreshSession`, and required user accessors).

After implementation, the focused suite passed. A full-suite run found and fixed two test/setup root causes without weakening production invariants: the expired fixture used a persistent fixed token hash, and the application context had no explicit test-profile JWT key. The fixture is now unique per run and the test profile has an explicit safe key.

## Commands and output

- `./mvnw -Dtest=AuthControllerTest test` — `BUILD SUCCESS`; `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`.
- `./mvnw test` — `BUILD SUCCESS`; `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0`.
- Both runs used MySQL 8.4 with Flyway migrations; no H2 replacement was used. Full-suite Testcontainers MySQL contexts also started and migrated V1/V2 successfully.
- `git diff --check` — no whitespace errors. Git displayed only normal LF→CRLF working-copy warnings for existing/editable text files.

## Security invariants and mutation review

- Missing-user and wrong-password mutations are covered by the same `INVALID_CREDENTIALS` status/code/message assertions, with no credential values in the response.
- JWT claim omission, wrong role, wrong token version, unsigned/wrong-key token, or lifetime changes are covered by exact claim-set, signature, and 900-second lifetime assertions.
- Cookie attribute/path/lifetime mutations are covered by HttpOnly, SameSite Strict, path, and seven-day assertions; logout covers Max-Age 0.
- Raw refresh persistence/exposure mutations are covered by response-body absence checks and direct database assertions that only the SHA-256 hash is stored.
- Rotation mutation is covered by old-session revocation, changed cookie value, new-session hash, replay rejection, and continued success of the replacement token.
- Expiry and logout/revocation mutations are covered by invalid-refresh responses and post-logout refresh failure.
- Refresh operations use a transaction and pessimistic row lock; failures do not return or log raw refresh material.

## Full suite

All 20 backend tests passed: 7 authentication tests, 2 application-context tests, 10 health/API safety tests, and 1 MySQL repository test.

## Self-review and concerns

- No known functional blocker remains for Task 4.
- The temporary `BootstrapSecurityConfiguration` intentionally denies non-auth requests and must be replaced by Task 5’s final authorization/JWT filter configuration.
- Production startup intentionally fails unless `JWT_SIGNING_KEY` is supplied with at least 256 bits; this is safer than a checked-in fallback. The test profile supplies an explicit test-only value.
- Maven emitted the existing Mockito dynamic-agent warning under the installed JDK; it did not affect compilation or test results and was not changed as part of Task 4.
- No concurrency stress test was added; the production path uses database row locking and transaction boundaries, while the focused tests cover sequential replay/rotation behavior requested for this task.

## Commit

Commit subject: `feat: add secure session authentication`

## Fix round 1/5

### Takeover and RED evidence

The preserved Task 4 implementation and RED tests were kept in place. This round added tests before the corresponding production changes. The boundary-input tests were deliberately run with the new size constraints absent: both failed with HTTP 401 instead of the expected HTTP 400, proving the missing validation behavior rather than a setup failure. The initial review-focused compile also failed only on the expected missing production API surface (the repository active-session count query and the controller constructor needed by the security-guard test). No plan document was created or modified.

### Findings addressed

1. Cookie-authenticated POST requests now pass an explicit request policy. Exact configured origins are required when an Origin header is present, `Sec-Fetch-Site: cross-site` is rejected, and requests without browser metadata remain deliberately supported for non-browser clients. The test profile explicitly lists trusted local/application origins; the default configuration is blank, so startup fails closed rather than accepting a production wildcard. Accepted local origin, cross-site origin, and untrusted same-site sibling origin are covered. Rejected requests return `AUTH_REQUEST_REJECTED` before controller parsing or service mutation, with no replacement cookie.

2. The temporary security bridge permits only POST `/api/v1/auth/login`, `/refresh`, and `/logout`; all other methods and every other `/api/v1/auth/**` path are denied. MockMvc covers GET login, an unlisted POST endpoint, and a nested login path. CSRF remains exempted/disabled for these stateless JSON auth endpoints as required by the brief; final authorization remains Task 5 scope.

3. Unexpected exceptions now create a server-side sanitized log record containing the trace ID, exception type, and first stack location, without the throwable message or request data. The log-capture test injects a fake secret into an exception message and verifies that trace/type/location are present while the secret is absent. Existing safe API errors and trace IDs remain unchanged.

4. Login usernames are bounded at 64 characters, and passwords are nonblank with a generous 4096-character transport bound. Refresh cookies must be exactly 43 unpadded Base64URL characters, matching 32 random bytes. Cookie parsing is deterministic: missing is treated as absent, duplicate names and malformed/oversized values are rejected before service/database lookup, and logout remains idempotent while clearing the canonical cookie. Tests verify malformed, oversized, duplicate, and invalid logout inputs do not mutate sessions.

5. Refresh cookies remain Secure by default. `Secure=false` is accepted only for explicitly named `local`, `test`, or `e2e` profiles; any other profile, including production, fails construction. The normal login response asserts the Secure attribute, and a unit test covers the production guard.

6. Refresh rotation retains the transactional pessimistic row lock and constant-time hash comparison. A bounded, latch-coordinated concurrent MySQL test races two refresh requests over one cookie and verifies exactly one success, one rejection, and one usable unrevoked replacement. It uses `Future.get` timeouts and no sleeps; the run completed without deadlock or timeout.

### Commands and output

- `./mvnw '-Dtest=AuthControllerTest,GlobalExceptionHandlerLoggingTest' test` — `BUILD SUCCESS`; 19 tests, 0 failures/errors (AuthControllerTest 18/18; logging 1/1).
- `./mvnw -Dtest=AuthControllerTest test` — `BUILD SUCCESS`; 18 tests, 0 failures/errors.
- `./mvnw test` — `BUILD SUCCESS`; 32 tests, 0 failures/errors. This included the MySQL-backed repository tests and a Testcontainers MySQL 8.4 context with Flyway V1/V2 migration success.
- `git diff --check` — no whitespace errors before staging. Final staged diff check will be run before commit.

### Security invariants and mutations reviewed

- Origin rejection occurs before refresh-cookie parsing and before login/refresh/logout service calls; rejected browser requests cannot rotate, revoke, or create a session.
- The bridge cannot accidentally expose another auth verb or path while Task 5 is pending.
- Raw passwords, refresh values, hashes, signing keys, cookie headers, request bodies, and exception messages are not placed in the new operational log record or API errors.
- Refresh material is format-checked before repository access; only its SHA-256 digest is persisted. Row locking and a transaction make the old token single-use under concurrent rotation.
- Cookie attributes remain HttpOnly, SameSite Strict, path-scoped, seven-day lifetime, and Secure unless an explicit non-production profile opts out. Production defaults fail closed for both missing trusted origins and insecure cookies.

### Self-review and concerns

- No functional blocker remains for this review round.
- The temporary bridge intentionally denies all non-auth traffic and must be replaced by Task 5's final JWT request filter and role authorization; no Task 5 authorization was added here.
- Browser clients must use one of the explicitly configured trusted origins. Deployments must provide `AUTH_TRUSTED_ORIGINS` and a sufficiently strong `JWT_SIGNING_KEY`; missing values fail closed.
- Maven/JDK emitted the existing Mockito dynamic-agent warning. It did not affect test results and was not broadened into an unrelated build change.

### Fix round 1 commit

Commit subject: `fix: harden session authentication`

## Fix round 2/5

### RED/GREEN evidence

The Secure-cookie regression tests were added before the policy change. With the old `anyMatch` profile rule, `insecureCookiesCannotBeConfiguredWithAProductionAndLocalProfile` failed because the constructor returned normally. The no-profile regression (`""`) was already green under the old implementation because it already failed closed; it was retained to protect that boundary. The minimal implementation now parses a nonempty list and requires every trimmed profile to be exactly `local`, `test`, or `e2e`.

The resulting profile coverage accepts `local`, `test`, `e2e`, and combinations composed only of those names. It rejects no profile, `prod`, `production`, `prod,local`, `production,local`, uppercase `LOCAL`, and nonexact `local-dev`.

The concurrency proof was replaced test-first with a holder transaction plus real MySQL lock-wait observation. The green run held the original `refresh_session` row through a dedicated `TransactionTemplate`, started both MVC refresh workers, observed two MySQL `performance_schema.data_lock_waits` rows for `refresh_session`, then released the holder. It proved exactly one 200 response, exactly one `401 INVALID_REFRESH_TOKEN`, old-token revocation, exactly one active replacement, and successful use of the winning replacement.

Mutation verification temporarily changed `AuthService.refresh` from `findByTokenHashForUpdate` to the nonlocking `findByTokenHash`, then ran the same test. It failed with `expected: 1 but was: 2` active refresh sessions. This is the expected lost-update failure: both workers had read a usable pre-revocation state before they later contended on the holder's row lock for their writes. The production call was restored immediately after that negative-control run.

### Concurrency root-cause investigation

The mutation result was investigated as a possible production defect without weakening the test or adding timing sleeps. The restored production test passed repeatedly. A focused run with Spring transaction-interceptor trace logging and Hibernate SQL logging established:

- The holder (`pool-3-thread-1`) and both MVC refresh workers (`pool-3-thread-2` and `pool-3-thread-3`) each create their own transaction.
- All three issue `select ... from refresh_session ... for update`, and the test observes both worker waits before releasing the holder.
- `AuthService.refresh` performs its first `RefreshSession` read with that locking query. There is no prior session read in either worker transaction, so no pre-lock entity or first-level-cache snapshot exists.
- After release, the winning worker persists its replacement and revokes the old session. The other worker resumes its locking read, reaches the post-lock `current.isUsableAt(now)` check, and rolls back with `InvalidRefreshTokenException` from `AuthService.java:71`.

Therefore the two-success result was a deliberate lock-removal mutation, not a defect in the restored production path. No production change was warranted for this finding: the transactional pessimistic query and the revalidation already occur in the correct order. The strengthened test now prevents their removal or movement before the lock.

### Commands and output

- `./mvnw '-Dtest=AuthControllerTest#insecureCookiesCannotBeConfiguredWithAProductionAndLocalProfile+insecureCookiesCannotBeConfiguredWithoutAnActiveProfile' test` — RED: 2 tests run, 1 expected failure; `prod,local` incorrectly returned normally while the no-profile guard was already green.
- `./mvnw '-Dtest=AuthControllerTest#insecureCookiesCannotBeConfiguredOutsideAnExplicitLocalProfile+insecureCookiesCannotBeConfiguredWithAProductionAndLocalProfile+insecureCookiesCannotBeConfiguredWithoutAnActiveProfile' test` — GREEN: 3 tests, 0 failures/errors.
- `./mvnw '-Dtest=AuthControllerTest#concurrentRefreshAttemptsAllowExactlyOneSingleUseRotation' test` — GREEN: 1 test, 0 failures/errors with the real `FOR UPDATE` path.
- `./mvnw '-Dtest=AuthControllerTest#concurrentRefreshAttemptsAllowExactlyOneSingleUseRotation' test` with the temporary nonlocking mutation — RED: `expected: 1 but was: 2` active sessions. The mutation was restored before all subsequent runs.
- `./mvnw '-Dtest=AuthControllerTest#concurrentRefreshAttemptsAllowExactlyOneSingleUseRotation' '-Dlogging.level.org.springframework.transaction.interceptor=TRACE' '-Dlogging.level.org.hibernate.SQL=DEBUG' test` — GREEN: 1 test, 0 failures/errors; captured the independent transactions, three `FOR UPDATE` statements, winner commit, and second post-lock invalid-token rollback described above.
- `./mvnw '-Dtest=AuthControllerTest' test` — GREEN: 21 tests, 0 failures/errors.
- `./mvnw test` — GREEN: 35 tests, 0 failures/errors across authentication, application context, health/API safety, logging, and MySQL repository suites. Testcontainers MySQL 8.4 started and migrated successfully.

### Files and mutation rationale

- `backend/src/main/java/cn/edu/cysz/careerplatform/auth/AuthController.java` — production policy-only change: Secure=false is now fail-closed unless every configured active profile is an exact approved local/test/e2e profile.
- `backend/src/test/java/cn/edu/cysz/careerplatform/auth/AuthControllerTest.java` — expands profile boundary coverage and replaces the schedule-dependent refresh race with a deterministic holder transaction, latches, bounded futures/condition, and lock-wait observation. No production test hook or mock-call assertion was added.
- The `findByTokenHashForUpdate` → `findByTokenHash` mutation proves the test catches removal of the locking/revalidation boundary; it is not retained in production.

### Concerns

- The lock-wait assertion intentionally connects as the MySQL compose root test account to read `performance_schema.data_lock_waits`; the application account has no access to that diagnostic schema. It is test-only and aligns with the local MySQL 8.4 compose environment used by this task.
- Existing Mockito dynamic-agent warnings remain unchanged and did not affect any result.

## Fix round 3/5

### Root cause and portability correction

The preserved partial Testcontainers migration correctly bound the Spring datasource to the class-local `MySQLContainer<>("mysql:8.4")`, but its diagnostic connection used the container's ordinary application user. A focused run proved the resulting CI-portability failure: the application migrated and ran against the container's random mapped port and `test` schema, then `performance_schema.data_lock_waits` failed with `SELECT command denied to user 'test'`.

The diagnostic query now uses the same class-local container's JDBC URL and container-provided password with MySQL's root diagnostic account. This is not a Compose credential, fixed port, fixed database, or host assumption: the application datasource remains `mysql.getJdbcUrl()`, `mysql.getUsername()`, and `mysql.getPassword()`, and the diagnostic connection uses that same `mysql.getJdbcUrl()` plus the container-generated root password. Root access is limited to the test-only `performance_schema` lock-wait observation; application work continues to use the non-privileged Testcontainers user.

### Verification and mutation evidence

- Initial focused run: expected RED for the portability defect. The class-local MySQL 8.4 container started on a random port and Flyway migrated it; the concurrency proof then failed only because `test` lacked `SELECT` on `performance_schema.data_lock_waits`.
- `./mvnw -Dtest=AuthControllerTest#concurrentRefreshAttemptsAllowExactlyOneSingleUseRotation test` after the diagnostic credential correction — GREEN: 1 test, 0 failures/errors. The holder transaction held the original row, both workers were observed waiting through `performance_schema`, release was latch-controlled, and the result retained one 200, one `401 INVALID_REFRESH_TOKEN`, old-token revocation, one active replacement, and a usable winner replacement.
- Negative control: temporarily changed `AuthService.refresh` from `findByTokenHashForUpdate` to `findByTokenHash` and reran the same focused test. Expected RED: `expected: 1 but was: 2` active replacement sessions. The production lock query was immediately restored.
- Restored focused run: `./mvnw -Dtest=AuthControllerTest#concurrentRefreshAttemptsAllowExactlyOneSingleUseRotation test` — GREEN: 1 test, 0 failures/errors.
- Full suite: `./mvnw test` — GREEN: 35 tests, 0 failures/errors. AuthControllerTest ran 21 tests; the suite also completed its other Testcontainers MySQL 8.4 contexts and Flyway migrations.

### Files and self-review

- `backend/src/test/java/cn/edu/cysz/careerplatform/auth/AuthControllerTest.java` — took over the pre-existing dirty Testcontainers datasource migration and changed only the diagnostic connection to use the root account of that same class-local container. No production source change is retained.
- The concurrency test remains bounded by latches, future timeouts, and condition polling; it uses no arbitrary timing sleep. It proves both worker lock waits before holder release and fails under the nonlocking repository mutation.
- Existing Mockito dynamic-agent/JDK warnings and Testcontainers' JUnit closeable-resource warning remain external test-runtime noise; no new test failure or functional concern remains.
