# 2.0.3 — Close the navigation dead ends — Requirements

> Roadmap item: **2.0.3 — Close the navigation dead ends** (Phase 2.0).
> Branch: `feature/2.0.3-navigation-dead-ends`. Plan: `plan.md`. Evidence: `validation.md`.
> Owner decision recorded 2026-09-25 (ask round before implementation).

## Problem

`app/src/main/java/com/nexvault/wallet/ui/main/MainScreen.kt` wires Home → TokenDetail for real
but passes **empty lambdas** for every other action — `onNavigateToSend`, `onNavigateToReceive`,
`onNavigateToSwap` (`MainScreen.kt:134-136`) and TokenDetail's `onNavigateToSend/Receive/History`
(`:148-150`). Tapping any of those six controls silently does nothing, which the roadmap names the
defect — not the absence of the destinations (Send = 2.6, Receive = 2.7, Swap = 3.3,
History = 2.8).

The four placeholder tabs (History/DApp/NFT/Settings) already render an informative
"Coming in Phase X" screen, so they are **not** dead ends and stay unchanged.

## Owner decision (Q/A, 2026-09-25)

| # | Question | Answer |
| --- | --- | --- |
| Q1 | How should the six dead affordances behave, given their destinations don't exist yet? | **Disabled + a visible explanation.** The buttons render disabled (Material 3 disabled colors) with a persistent one-line caption under each action row stating what arrives and in which phase. The empty-lambda wiring is deleted; the navigation callbacks return when the owning items (2.6/2.7/2.8/3.3) land. |

### Derived decisions

| # | Decision | Why |
| --- | --- | --- |
| D1 | User-facing text names phases, not roadmap ids. | "Phase 2" / "Phase 3" is meaningful to a user; "2.6" is not. |
| D2 | The captions are new English strings in each feature module's `values/strings.xml`. | Only `values/` exists today; ZH i18n is roadmap 3.7. |
| D3 | No unit tests are added for the UI change. | `feature-home`/`feature-tokens` have zero tests; TC-UI coverage is owned by **2.0.6** (legacy CR 2.4-1, 2.5-1). |
| D4 | The placeholder-tab text in `app` stays hardcoded for now. | The hardcoded-strings migration is owned by **4.17**; 2.0.3 touches only the dead controls. |

## Acceptance criteria

| AC | Criterion |
| --- | --- |
| AC-1 | No clickable control in the shipped UI is wired to an empty lambda (grep proves it). |
| AC-2 | Home's Send/Receive/Swap render disabled with a caption naming what arrives and when. |
| AC-3 | TokenDetail's Send/Receive render disabled and its See All link disabled, with a caption covering all three. |
| AC-4 | Home → TokenDetail navigation and all five tab switches still work (verified on device). |
| AC-5 | Build green; full unit suite unchanged-green; no new detekt/ktlint findings in touched files. |

## Non-goals

- Building any send/receive/swap/history destination (2.6/2.7/3.3/2.8).
- Rewriting the placeholder tabs, migrating `app` hardcoded strings (4.17), snackbar
  infrastructure, or adding UI tests (2.0.6).
- Any change under `doc/` or the legacy CR records.
