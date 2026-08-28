# Task 10 Report

## RED
- Command: `cd frontend; npm run test:e2e`
- Expected failure: `Error: No tests found`
- Cause: the brief required the first spec at `frontend/tests/e2e/auth-home.spec.ts`, while the existing Playwright config still pointed at `frontend/e2e`.

## Implementation
- `frontend/tests/e2e/auth-home.spec.ts`: added 3 Playwright tests covering student login, refresh-based session restore after reload, student access to `/admin` redirecting to `/forbidden`, homepage key copy, absence of `校园活动`, 390px no page-level horizontal overflow, and `朝小职` remaining interactive without covering the main job-browse action.
- `frontend/playwright.config.ts`: switched `testDir` to `./tests/e2e`, reduced runtime to Chromium, added explicit `baseURL`, and configured dual `webServer` startup for the backend and Vite dev server with Windows/Linux-compatible Maven commands.
- `frontend/vite.config.ts`: added `/api` proxying to `http://127.0.0.1:8080` so browser auth requests stay same-origin through the Vite server.
- `frontend/eslint.config.ts`: taught Playwright lint rules about `tests/e2e/**`.
- `frontend/vitest.config.ts`: excluded `tests/e2e/**` from unit-test discovery.
- `backend/src/main/resources/application-e2e.yaml`: added an e2e-only profile using dedicated schema `career_platform_e2e`, e2e auth origins, an e2e JWT secret, and non-secure refresh cookies for local HTTP.
- `backend/src/main/java/cn/edu/cysz/careerplatform/auth/E2eAccountsConfiguration.java`: seeds `student`, `teacher`, and `admin` test accounts only when the `e2e` profile is active.
- `.github/workflows/ci.yml`: added CI for backend tests, frontend `npm ci`, Playwright browser install, lint, unit, build, Docker Compose MySQL startup, e2e execution, and failure-only Playwright report upload.
- `README.md`: documented the local auth/e2e workflow, Vite proxy behavior, and the e2e-only test accounts.
- Scope guard: no Task 9 homepage Vue component code was changed.

## Verification
- Backend test: `cd backend; ./mvnw.cmd test`
  - Result: blocked by host environment. Testcontainers could not find a valid Docker environment; Maven reported 15 tests run with 4 errors.
- Backend fallback compile: `cd backend; ./mvnw.cmd -DskipTests test-compile`
  - Result: passed.
- Frontend install: `cd frontend; npm ci`
  - Result: passed.
- Frontend lint: `cd frontend; npm run lint`
  - Result: passed before verification reruns and again after the final Windows Playwright-command fix.
- Frontend unit: `cd frontend; npm run test:unit -- --run`
  - Result: passed with 7 files and 34 tests.
- Frontend build: `cd frontend; npm run build`
  - Result: passed before verification reruns and again after the final Windows Playwright-command fix.
- Playwright discovery: `cd frontend; npx playwright test --list`
  - Result: passed; discovered 3 tests in `auth-home.spec.ts`.
- Docker Compose MySQL: `docker compose -f infra/compose.yaml up -d`
  - Result: blocked by host environment. Docker CLI could not connect to `npipe:////./pipe/dockerDesktopLinuxEngine`.
- Backend e2e boot: `cd backend; ./mvnw.cmd "-Dspring-boot.run.profiles=e2e" spring-boot:run`
  - Result: blocked by infrastructure. Spring Boot reached the `e2e` profile, then failed Flyway startup with `Communications link failure` / `Connection refused` to `localhost:3307`.
- E2E execution: `cd frontend; npm run test:e2e`
  - Result: blocked by infrastructure. Playwright discovered the spec but the backend `webServer` exited with code 1 because MySQL was unavailable.

## Coverage Intent In The E2E Spec
- Student login reaches the student homepage.
- Reload restores the student session through the refresh flow.
- Student access to `/admin` lands on `/forbidden`.
- Homepage keeps the required headline and guidance copy.
- Homepage does not show `校园活动`.
- At 390px viewport width, the page does not horizontally overflow.
- `朝小职` can be expanded/collapsed and does not overlap the main job-browse action.

## Concerns
- Real green E2E evidence is still pending a live Docker/MySQL environment; today only test discovery and infra-failure behavior could be verified.
- Backend `./mvnw test` remains infra-blocked on this host until Docker Desktop (or another compatible daemon) is running.
- Unrelated worktree changes at `.superpowers/sdd/2026-08-27-foundation-auth-homepage/task-9-report.md` and untracked `frontend/scripts/` were left untouched.

## Fix Round 1

### Changed files
- `frontend/playwright.config.ts`
- `frontend/playwright.config.spec.ts`
- `frontend/tests/e2e/auth-home.spec.ts`
- `frontend/src/features/home/components/XiaoZhiShell.vue`
- `.superpowers/sdd/2026-08-27-foundation-auth-homepage/task-10-report.md`

### Review items fixed
- Critical: replaced the backend Playwright readiness probe with `url: 'http://127.0.0.1:8080/api/v1/health'` and removed the backend `port` probe, preserving the cross-platform Maven command.
- Important: expanded the 390px E2E to re-check `#xiaozhi-panel` against the `直接浏览岗位库` primary action after opening the shell, prove the action remains `trial`-clickable, then close the shell and verify the closed state; on narrow screens the shell now opens upward instead of horizontally across the bottom CTA area.

### Covering test files
- `frontend/playwright.config.spec.ts`
- `frontend/tests/e2e/auth-home.spec.ts`

### Commands and results
- `cd frontend; npm run test:unit -- playwright.config.spec.ts --run`
  - RED result before the config fix: failed with `expected undefined to be 'http://127.0.0.1:8080/api/v1/health'`.
  - GREEN result after the config fix: passed with `Test Files 1 passed (1); Tests 1 passed (1)`.
- `cd frontend; npx playwright test --list`
  - Result: passed; discovered 3 Chromium tests in `auth-home.spec.ts`, including the 390px overlay scenario.
- `cd frontend; npm run lint`
  - Result: passed.
- `cd frontend; npm run test:unit -- --run`
  - Result: passed with `Test Files 8 passed (8); Tests 35 passed (35)`.
- `cd frontend; npm run build`
  - Result: passed; Vite built 134 modules successfully.
- `docker compose -f infra/compose.yaml up -d`
  - Result: blocked by host environment. Exact output: `failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine; check if the path is correct and if the daemon is running: open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.`

### Concerns
- On August 28, 2026, this host still did not provide a running Docker daemon, so real MySQL-backed E2E could not be attempted in this fix round.
- The upward-opening narrow-screen shell is the smallest product adjustment I made for the review item; it was not accompanied by a live browser E2E pass because infrastructure remained blocked.
