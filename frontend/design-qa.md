# Home Design QA

- Source visual truth: `docs/design/career-platform-homepage-final-v3.png`
- Implementation screenshots:
  - `frontend/.qa-artifacts/task-9-desktop-910.png`
  - `frontend/.qa-artifacts/task-9-narrow-390.png`
- Viewports:
  - Desktop: 910×1728
  - Narrow: 390×1728
- Captured pixel sizes:
  - Source: 910×1728
  - Desktop implementation: 1264×2947
  - Narrow implementation: 375×4018
- Density normalization: browser-native capture scale retained; no post-resize normalization was needed for the accepted comparison set.
- State: default homepage, `朝小职` closed, disabled phase-two links visible, route guard preserved, temporary QA harness mounted through real `createMemoryHistory()` + real Pinia STUDENT session.

## Comparison Summary

- Full-view comparison was sufficient because the actionable deltas were section-level composition, copy, image fidelity, and interactive state.
- Desktop hierarchy, navigation, hero copy, entry routes, resume section, and independent services section all match the source intent.
- Narrow layout stacks cleanly and keeps the floating assistant non-blocking.
- Console: no warnings or errors in the final browser passes.

## Interaction Checks

- `朝小职` open / close.
- Skip link target `#main-content` present and covered by regression tests.
- Disabled `职业测评` / `课程指导` nav badges remain visible with `即将上线`.

## Comparison History

- Iteration 1: opaque WebP derivatives introduced white-matte artifacts and an oversized hero assistant crop.
  - Fix: regenerated the affected assets from the transparent repair variants and reduced the hero assistant scale/placement.
  - Post-fix evidence: `frontend/.qa-artifacts/task-9-desktop-910.png`, `frontend/.qa-artifacts/task-9-narrow-390.png`.
- Iteration 2: temporary QA harness initially warned on `/qa.html` because the production router was mounted.
  - Fix: switched the harness to real `createMemoryHistory()` while preserving the production auth guard and STUDENT session.
  - Post-fix evidence: final browser logs were empty.

## Findings

- No actionable P0/P1/P2 issues remain.

## Notes

- Browser keyboard-focus state did not surface a distinct active-element change in this automation surface, so the skip-link contract is additionally covered by `frontend/src/features/home/__tests__/HomeView.spec.ts`.

final result: passed
