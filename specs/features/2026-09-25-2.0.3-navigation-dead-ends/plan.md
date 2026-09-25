# 2.0.3 — Close the navigation dead ends — Technical plan

> Approved 2026-09-25 (plan mode). Requirements: `requirements.md` (Q1, D1–D4).
> Evidence: `validation.md`. Branch: `feature/2.0.3-navigation-dead-ends` — implemented and
> committed there (owner-directed, for review); merging to `main` is an owner action.

## Scope (verified in code, 2026-09-25)

Dead affordances, all wired to empty lambdas in `MainScreen.kt`:

| Surface | Control | Owning item | Treatment |
| --- | --- | --- | --- |
| `HomeScreen` quick actions | Send · Receive · Swap | 2.6 · 2.7 · 3.3 | disabled + row caption |
| `TokenDetailScreen` actions | Send · Receive | 2.6 · 2.7 | disabled + row caption |
| `TokenDetailScreen` recent transactions | "See All" → history | 2.8 | disabled (explained by the same caption) |

## Changes

### 1. `feature:feature-home` — `HomeScreen.kt` + `res/values/strings.xml`
- Remove `onNavigateToSend` / `onNavigateToReceive` / `onNavigateToSwap` parameters.
- `QuickActionButton` gains `enabled: Boolean` (default true); the three buttons pass `false`.
- Caption directly under `QuickActionsRow`:
  new string `home_actions_coming_soon` = "Send and receive arrive in Phase 2; swap in Phase 3."

### 2. `feature:feature-tokens` — `TokenDetailScreen.kt` + `res/values/strings.xml`
- Remove `onNavigateToSend` / `onNavigateToReceive` / `onNavigateToHistory` parameters; update the
  KDoc ("wired in a later task" → "disabled until 2.6/2.7/2.8 land").
- `TokenActionButtons`: `Button` / `OutlinedButton` with `enabled = false`; caption under the row:
  new string `token_detail_actions_coming_soon` = "Send, receive and full transaction history arrive in Phase 2."
- "See All" `TextButton`: `enabled = false`.

### 3. `app` — `MainScreen.kt`
- Delete the six empty-lambda arguments; `MainScreen`'s own signature is unchanged.

## Tests

None to add or change — the two modules have no test files (TC-UI is 2.0.6). Re-run the whole
suite and the build to prove no regression.

## Validation plan

- **Automatic:** `./gradlew :app:assembleDebug testDebugUnitTest :domain:test`; gate delta vs
  `main` head (`900aaea`: detekt 65, ktlint 1454) — identical or lower, zero new findings in the
  touched files.
- **Manual (Pixel 6a):** Home shows greyed Send/Receive/Swap + caption; taps do nothing and don't
  crash; token detail shows greyed Send/Receive + See All + caption; Home → TokenDetail and all 5
  tabs still work; screenshots recorded (no FLAG_SECURE on these screens).

## Edge cases & assumptions

- `recentTransactions` empty → the See All row isn't rendered (existing behavior, still correct).
- Material 3 handles the disabled visuals; no custom colors.
- No new dependencies; no changes to navigation graphs.
