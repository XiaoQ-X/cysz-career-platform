# Home Design QA

- Date: 2026-08-27
- Reference: `docs/design/career-platform-homepage-final-v3.png`
- Comparison captures: in-app Browser, 910px desktop viewport and 390px narrow viewport
- Verification surface: temporary Vite QA harness mounted the real `HomeView` inside the real router shell with a real Pinia store and a minimal in-memory `STUDENT` session so the protected homepage could be reviewed without changing production auth routing

## Comparison Summary

- Desktop 910px: section order, navigation labels, hero copy, CTA hierarchy, and dark cosmic art direction match the reference intent.
- Narrow 390px: the header condenses cleanly, the hero CTA remains visible, the entry cards stack without overlap, and the floating assistant stays non-blocking.
- Interaction/state: `朝小职` expands and collapses correctly, `职业测评` and `课程指导` remain disabled `ComingSoonLink` controls, and focus can reach the skip link targeting `#main-content`.
- Console: in-app Browser console check returned no warnings or errors.

## Findings

- P3: the hero title angle is simplified compared with the source artwork, but the hierarchy, copy, and above-the-fold composition remain aligned.
- P3: the supplied raster asset bundle uses light matte edges instead of perfect cutout transparency, so the implementation softens those edges with CSS masking; the resulting glow treatment is slightly less crisp than the source image.

## Resolution

- No P0, P1, or P2 fidelity, accessibility, responsiveness, or interaction issues remained after the final pass.

final result: passed
