# Home Design QA — final review fix wave

- Source visual truth: `docs/design/career-platform-homepage-final-v3.png`
  - Pixel size: 910x1728
  - File size: 1,854,864 bytes
- Production-preview captures:
  - `frontend/.qa-artifacts/final-fix-desktop-1440.png`
    - CSS viewport: 1440x1000
    - Captured pixel size: 1440x2308
    - File size: 748,658 bytes
  - `frontend/.qa-artifacts/final-fix-desktop-1440-footer-expanded.png`
    - CSS and captured viewport: 1440x1000
    - File size: 281,172 bytes
  - `frontend/.qa-artifacts/final-fix-narrow-390.png`
    - CSS viewport: 390x844
    - Captured pixel size: 390x4311
    - File size: 646,574 bytes
  - `frontend/.qa-artifacts/final-fix-narrow-390-footer-expanded.png`
    - CSS and captured viewport: 390x844
    - File size: 54,718 bytes
- Capture state: authenticated student homepage, production Vite preview, XiaoZhi collapsed for full-page captures and expanded at the footer for safe-area captures. The capture harness intercepted only the refresh response and was removed after capture. No browser console or page errors were observed.

## Repaired artwork

The four failed transparent WebPs were removed from the runtime tree and replaced with code-native inline SVG components:

- `XiaoZhiPetArt.vue`: friendly pet-like graduate mascot used by the hero and the real expand/collapse assistant toggle.
- `ResumeDocumentArt.vue`: luminous résumé document, profile marks, upload arrow, and orbital base.
- `CareerTargetArt.vue`: concentric career target, arrow, and orbital base.
- `CourseCubeArt.vue`: course cube, graduation cap, tassel, and open-book mark.

The components contain only SVG geometry and CSS colors; none embeds an `<image>` or raster data. `HomeView.spec.ts` renders the real components and asserts all five runtime placements, `aria-hidden`, absence of embedded images, and absence of `*-transparent.webp` image sources.

The old Windows-only matte verifier was removed with its four failed target files. No replacement alpha verifier is applicable because these four runtime artworks no longer use raster alpha. The remaining runtime WebPs (`hero-cosmos`, `assessment-compass`, and `resume-orbit`) are intentional complete compositions, not replacements for the failed transparent cutouts.

## Manual visual and source checks

- Desktop and 390px captures show clean subject edges with no rectangular, striped, checkerboard, or light-matte residue.
- XiaoZhi retains the same mascot identity in the hero and toggle, while the interactive shell still owns expand/collapse and `aria-controls` behavior; no chat, Coze, or drag behavior was added.
- The four subjects preserve the reference intent and cyan/violet/amber visual language.
- Footer information targets are visible and real. Expanded-shell captures show footer links and copyright above the reserved blank region at both viewport widths.
- The production build packages only the three intentional remaining WebPs listed above; the four repaired subjects are emitted as component SVG markup.

## Automated coverage status

- Focused DOM/component tests: passed after RED failures for missing vectors, footer targets, and safe-area structure.
- Production build/type-check: passed, including the active Playwright TypeScript project.
- Playwright discovery: five tests found, including logout/reload and desktop/390px collapsed/expanded hit-testing/focusability coverage.
- Live Docker-backed Playwright execution remains environment-dependent and must not be inferred from these screenshots.
