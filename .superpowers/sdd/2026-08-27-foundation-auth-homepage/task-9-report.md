# Task 9 Report

## RED
- Prior verified RED inherited from the interrupted implementation session: `npm run test:unit -- HomeView.spec.ts --run`
- Expected failure: `HomeView.vue` did not exist yet, so the homepage spec failed on import resolution before any assertions.
- During takeover, the first focused rerun exposed a test harness issue instead of a homepage behavior issue: router auth navigation needed an active Pinia instance before `router.push('/')`. The spec setup was corrected with `setActivePinia(createPinia())`, then verification continued against the existing implementation rather than redoing RED.

## GREEN / Verification
- Focused homepage spec: `npm run test:unit -- HomeView.spec.ts --run`
- Result: passed.
- Full frontend unit: `npm run test:unit -- --run`
- Result: 7 test files, 32 tests passed.
- Build: `npm run build`
- Result: `vue-tsc` and `vite build` passed.
- Lint: `npm run lint:oxlint`
- Result: passed.
- Lint: `npm run lint:eslint`
- Result: passed.
- Diff hygiene: `git diff --check`
- Result: passed before commit cleanup and again after final edits.
- Browser QA: Codex Desktop in-app Browser on 910px desktop and 390px narrow viewports through a temporary Vite QA harness.
- Browser QA result: homepage rendered in a real Vue app with a real Pinia store and memory router; keyboard focus reached the skip link; `朝小职` expanded/collapsed correctly; browser console returned `[]`.
- Design QA: `frontend/design-qa.md`
- Result: `final result: passed`.

## Assets
- Consumed homepage assets:
  - `frontend/src/assets/home/hero-cosmos.png`
  - `frontend/src/assets/home/optimized/hero-cosmos.webp`
  - `frontend/src/assets/home/optimized/xiaozhi-robot-transparent.webp`
  - `frontend/src/assets/home/optimized/holographic-resume-transparent.webp`
  - `frontend/src/assets/home/optimized/career-target-transparent.webp`
  - `frontend/src/assets/home/resume-orbit.png`
  - `frontend/src/assets/home/optimized/resume-orbit.webp`
  - `frontend/src/assets/home/assessment-compass.png`
  - `frontend/src/assets/home/optimized/assessment-compass.webp`
  - `frontend/src/assets/home/optimized/course-cube-transparent.webp`
- The transparent repair PNGs were reviewed first and then converted into optimized WebP derivatives for the final render; they are the assets used in the accepted browser screenshots.

## Implementation
- `frontend/src/app/router.ts`: replaced the student placeholder root with `HomeView`; added `STUDENT`-protected routes for `/resume`, `/job-preferences`, `/jobs`, and `/profile`; preserved login, forbidden, teacher, admin, and auth-guard behavior.
- `frontend/src/features/home/HomeView.vue`: assembled the student homepage with `SkipLink`, section ordering, footer, and `main id="main-content"`.
- `frontend/src/features/home/components/HomeHeader.vue`: added the exact required navigation labels and kept `职业测评` and `课程指导` as disabled `ComingSoonLink` controls.
- `frontend/src/features/home/components/HeroSection.vue`: built the hero banner with the shared `AppButton`, cosmic background art, companion bubble, responsive layout, and reduced-motion-safe floating treatment.
- `frontend/src/features/home/components/JobEntrySection.vue`: implemented the two-path student entry section with real routes for resume upload, job preferences, and direct job browsing, while omitting recommendations and campus activities.
- `frontend/src/features/home/components/ResumeSection.vue`: implemented the single-upload/multi-use resume value section and linked it to `/resume`.
- `frontend/src/features/home/components/IndependentServicesSection.vue`: implemented the independent exploration section and kept evaluation/course guidance non-navigable for this phase.
- `frontend/src/features/home/components/XiaoZhiShell.vue`: implemented the collapsible `朝小职` shell, wired `aria-controls` to `#xiaozhi-panel`, and kept the assistant non-blocking.
- `frontend/src/features/home/__tests__/HomeView.spec.ts`: preserved the TDD entry test and fixed the store setup so the router guard executes in the intended authenticated test state.

## Temporary QA Harness
- Created only for visual QA:
  - `frontend/qa.html`
  - `frontend/src/qa-main.ts`
- Purpose: mount the real homepage through the real router shell with a minimal in-memory `STUDENT` session, without changing production routing or auth guards and without starting the backend.
- Cleanup: both harness files were deleted after browser QA. Temporary screenshots under `.qa-temp/` were also deleted before the final git status check.

## Browser QA
- Desktop comparison used the source `career-platform-homepage-final-v3.png` against a 910px in-app Browser viewport capture of the QA harness.
- Narrow-screen comparison used a 390px in-app Browser viewport capture of the same harness.
- Verified states:
  - default homepage render
  - `朝小职` expanded and collapsed
  - skip-link target verified; keyboard focus was additionally covered by regression tests
  - console log inspection
- Outcome: no blocking fidelity or interaction defects remained; the remaining drift is documented as P3-only in `frontend/design-qa.md`.

## Mutation Rationale
- `HomeView.spec.ts` fails if the homepage loses the core hero copy, removes any of the three allowed student entry points, or reintroduces premature recommendation/campus content.
- Router behavior would fail manual QA if `/resume`, `/job-preferences`, `/jobs`, or `/profile` stopped resolving through the protected construction view.
- Browser QA would fail if `朝小职` could not open/close, if `#main-content` disappeared, or if disabled phase-two links turned into navigable routes.

## Self-Review
- Accessibility: the page keeps a functional skip link, focusable navigation, 44px+ controls, a named assistant toggle, and reduced-motion handling.
- Fidelity: reused the supplied homepage assets instead of CSS/SVG stand-ins; where asset mattes were imperfect, softened them with masking instead of replacing them with fake art.
- Scope control: did not introduce recommendation blocks, campus activity modules, or extra phase-two routes/pages.
- Auth safety: production login, forbidden, teacher/admin access, and the shared auth guard were preserved.
- QA discipline: browser verification ran against a real Vue runtime rather than a static mock, and the temporary harness was removed afterward.

## Concerns
- Browser keyboard-focus state did not surface as a distinct active element in the in-app browser automation surface, so the skip-link contract is additionally covered by the unit regression.

## Fix Round 1
- Scope: addressed the 4 Important review findings and 1 Minor review finding from `task-9-review.md` without adding new visual design or experimental screenshots.

### Review item evidence
- Important: header disabled nav entries hid the visible `即将上线` status.
  - Fix: `HomeHeader.vue` keeps `职业测评` and `课程指导` as disabled `ComingSoonLink` buttons and styles the inline `.coming-soon-link__badge` visibly in the primary nav.
  - Evidence: `HomeView.spec.ts` asserts two disabled nav buttons, no `href`, visible badge elements, and exact `即将上线` text.
- Important: automated DOM contract did not cover routes, disabled badge state, skip link, or `朝小职`.
  - Fix: `HomeView.spec.ts` now mounts the real `HomeView` with the real router and Pinia, then asserts `/resume`, `/job-preferences`, `/jobs`, `#main-content`, disabled coming-soon controls, and `朝小职` open / close via `aria-expanded` and `#xiaozhi-panel`.
  - Evidence: focused unit output: `Test Files 1 passed (1); Tests 3 passed (3)`.
- Important: homepage image payload and loading behavior needed tightening.
  - Fix: transparent runtime art imports use optimized WebP assets; hero images keep `fetchpriority="high"`; below-fold and assistant-toggle artwork uses `loading="lazy"`, `decoding="async"`, and explicit `width` / `height`.
  - Evidence: `HomeView.spec.ts` asserts optimized WebP sources and loading attributes. Build output includes WebP assets only for the repaired transparent art: 74.64 kB, 110.71 kB, 138.01 kB, and 140.59 kB.
- Important: durable QA comparison evidence was incomplete.
  - Fix: retained final artifacts only:
    - `frontend/.qa-artifacts/task-9-desktop-910.png`
    - `frontend/.qa-artifacts/task-9-narrow-390.png`
    - `frontend/.qa-artifacts/task-9-comparison-full.png`
    - `frontend/.qa-artifacts/task-9-comparison-focused.png`
    - `frontend/.qa-artifacts/task-9-comparison-round1.png`
  - Evidence: `frontend/design-qa.md` now records source image, implementation screenshots, CSS viewport, captured pixel dimensions, deviceScaleFactor, file sizes, state, full/focused comparison artifacts, review-round finding/fix/evidence, interaction checks, and console status.
- Minor: unused transparent PNG sources bloated the repo.
  - Fix: deleted:
    - `frontend/src/assets/home/xiaozhi-robot-transparent.png`
    - `frontend/src/assets/home/holographic-resume-transparent.png`
    - `frontend/src/assets/home/career-target-transparent.png`
    - `frontend/src/assets/home/course-cube-transparent.png`
  - Evidence: `rg -n "transparent\\.png|qa-main|qa\\.html" frontend\\src` produced no output after temporary QA cleanup.

### Cleanup
- Deleted intermediate QA screenshots: all dot-prefixed task-9 screenshots plus segmented, calibrated, repair-check, full-round1/full, and non-final round1 desktop captures.
- Deleted temporary QA harness files:
  - `frontend/.qa-fix-transparent-assets.py`
  - `frontend/qa.html`
  - `frontend/src/qa-main.ts`
- Kept the five final QA artifacts listed above.

### Commands and output
- `npm run test:unit -- HomeView.spec.ts --run`
  - Output summary: `Test Files 1 passed (1); Tests 3 passed (3); Duration 2.68s`.
- `npm run test:unit -- --run`
  - Output summary: `Test Files 7 passed (7); Tests 34 passed (34); Duration 2.76s`.
- `npm run build`
  - Output summary: `vue-tsc --build` passed; `vite build` passed; `134 modules transformed`; final JS gzip `62.54 kB`; repaired transparent WebP assets emitted at `74.64 kB`, `110.71 kB`, `138.01 kB`, and `140.59 kB`.
- `npx oxlint .`
  - Output summary: passed with no output.
- `npx eslint . --cache`
  - Output summary: passed with no output.
- `git diff --check`
  - Output summary: passed with no output.

### Final QA status
- `frontend/design-qa.md` records `final result: passed`, backed by the retained screenshots, comparison artifacts, regression tests, browser interaction checks, and empty final browser console.
