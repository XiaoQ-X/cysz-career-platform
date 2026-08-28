# Home Design QA

- Source visual truth: `docs/design/career-platform-homepage-final-v3.png`
  - Pixel size: 910x1728
  - File size: 1,854,864 bytes
- Implementation screenshots:
  - `frontend/.qa-artifacts/task-9-desktop-910.png`
    - CSS viewport: 910x1728
    - Captured pixel size: 910x1728
    - deviceScaleFactor: 1
    - File size: 663,044 bytes
  - `frontend/.qa-artifacts/task-9-narrow-390.png`
    - CSS viewport: 390x844
    - Captured pixel size: 390x844
    - deviceScaleFactor: 1
    - File size: 178,745 bytes
- Comparison evidence:
  - Full comparison: `frontend/.qa-artifacts/task-9-comparison-full.png`
    - Pixel size: 1820x1862
    - File size: 2,553,619 bytes
  - Focused comparison: `frontend/.qa-artifacts/task-9-comparison-focused.png`
    - Pixel size: 1820x1136
    - File size: 1,664,015 bytes
  - Fix round 1 comparison: `frontend/.qa-artifacts/task-9-comparison-round1.png`
    - Pixel size: 1790x1083
    - File size: 1,611,425 bytes
- Capture state: default homepage, `朝小职` closed for baseline screenshots, disabled phase-two links visible with `即将上线`, route guard preserved through the temporary real Vue QA harness, and temporary harness removed after capture.

## Asset Evidence

- Runtime transparent artwork now uses optimized WebP derivatives:
  - `xiaozhi-robot-transparent.webp`: 620x620, 138,018 bytes
  - `holographic-resume-transparent.webp`: 560x560, 110,714 bytes
  - `career-target-transparent.webp`: 560x560, 140,598 bytes
  - `course-cube-transparent.webp`: 480x480, 74,642 bytes
- Removed unused transparent PNG sources from the runtime asset tree:
  - `xiaozhi-robot-transparent.png`
  - `holographic-resume-transparent.png`
  - `career-target-transparent.png`
  - `course-cube-transparent.png`
- Hero art keeps `fetchpriority="high"` and no lazy loading; below-fold artwork and the floating assistant toggle use `loading="lazy"`, `decoding="async"`, and explicit `width` / `height` attributes to reserve layout ratio.

## Review Findings And Fix Evidence

- P0: none reported in the review.
- P1: Header hid the `即将上线` badge for disabled `职业测评` and `课程指导`.
  - Fix: `HomeHeader.vue` keeps disabled nav items visible as button controls with inline badge styling.
  - Post-fix evidence: `HomeView.spec.ts` asserts two disabled nav buttons, no `href`, visible `.coming-soon-link__badge`, and exact badge text.
- P1: DOM test contract did not cover routes, disabled badges, skip link, or `朝小职`.
  - Fix: `HomeView.spec.ts` now asserts `/resume`, `/job-preferences`, `/jobs`, `#main-content`, disabled coming-soon controls, and real `朝小职` open / close state via `aria-expanded` and `#xiaozhi-panel`.
  - Post-fix evidence: focused and full unit checks passed in the final verification run.
- P1: Homepage eagerly loaded large transparent PNG artwork and did not document the final image sizing path.
  - Fix: transparent runtime artwork imports now point to optimized WebP assets; below-fold images lazy-load with explicit dimensions; hero images remain high priority.
  - Post-fix evidence: `HomeView.spec.ts` asserts hero WebP priority and optimized WebP lazy artwork; final build passed with the PNG sources deleted.
- P1: Visual QA evidence was not durable enough to audit the `passed` claim.
  - Fix: kept the final desktop, narrow, full comparison, focused comparison, and round 1 comparison artifacts listed above; removed intermediate dot-prefixed, segmented, calibrated, repair-check, and temporary harness files.
  - Post-fix evidence: this file records source, implementation screenshots, viewports, pixel sizes, device scale, comparison artifacts, state, and console / interaction checks.
- P2: Four unused `*-transparent.png` files added repo weight without runtime use.
  - Fix: deleted the unused PNG sources after confirming optimized WebP derivatives are the runtime imports.
  - Post-fix evidence: `rg "transparent\\.png" frontend/src` finds no runtime references after `frontend/qa.html` and `frontend/src/qa-main.ts` cleanup.

## Interaction And Console

- `朝小职` open / close: verified by real component test and browser QA before temporary harness cleanup.
- Skip link: `a[href="#main-content"]` and `<main id="main-content">` present; covered by regression test.
- Disabled badges: visible `即将上线` text for `职业测评` and `课程指导`; covered by regression test and desktop screenshot.
- Console: final browser pass returned no warnings or errors.

final result: passed
