# 2.0.3 — Close the navigation dead ends — Validation record

> Status: **IMPLEMENTED + DEVICE-WALKED 2026-09-25 — AWAITING OWNER CONFIRMATION.**
> Requirements: `requirements.md` (Q1, D1–D4). Plan: `plan.md`.
> Branch: `feature/2.0.3-navigation-dead-ends`; the change set is committed there
> (owner-directed commit for review, 2026-09-25).

## Automatic half

| # | Check | Command | Result |
| --- | --- | --- | --- |
| A1 | Build | `./gradlew :app:assembleDebug` | **BUILD SUCCESSFUL**; APK `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk` (47,985,173 bytes, 16:48, `lastUpdateTime` on device matches) |
| A2 | Unit tests | `./gradlew testDebugUnitTest :domain:test --continue` | **BUILD SUCCESSFUL — 249 tests, 0 failed, 1 skipped** (unchanged from 2.0.2b) |
| A3 | Gate delta vs `main` (`900aaea`) | `./gradlew detekt ktlintCheck --continue`, stash-compare | **detekt 65 → 65; ktlint 1454 → 1454** — identical totals, identical per-file counts on the three touched files (HomeScreen 26, TokenDetailScreen 19, MainScreen 12); the 3 instances my first draft added were found by the stash-compare and removed (single-line modifiers, no `enabled` conditional) |
| A4 | No empty-lambda wiring | `grep -nE "= \{ \}|= \{ _, _ -> \}" app/.../MainScreen.kt` | **0 matches** (exit 1) |

## Manual half — device walk (Pixel 6a, 2026-09-25, adb over Wi-Fi)

App data had been cleared (Welcome on launch), so the walk first recreated a wallet on the
merged build — which also re-verified the 2.0.2b flow — then exercised 2.0.3.

| # | Step | Observed | Verdict |
| --- | --- | --- | --- |
| M1 | Welcome → Create → verify → Set PIN | full onboarding reaches Home; phrase `wheel turn option fruit asset paddle slow issue cinnamon club engage clever` verified in order | ✅ (2.0.2b flow intact) |
| M2 | Home quick actions | caption **"Send and receive arrive in Phase 2; swap in Phase 3."** renders under the row; three semantics nodes (`enabled="false"`, bounds wrapping each button) present; labels still readable | ✅ AC-2 |
| M3 | Tap Send/Receive/Swap | screen before == after (no navigation, no crash) | ✅ no-op is impossible — the buttons are disabled |
| M4 | Tap a token row | Home → **TokenDetail** navigation still works | ✅ AC-4 |
| M5 | TokenDetail actions | Send/Receive wrapped by two `enabled="false"` nodes; caption **"Send, receive and full transaction history arrive in Phase 2."** present (composed lower in the lazy list; visible after scrolling — screenshot `token_detail_caption.png`) | ✅ AC-3 |
| M6 | See All | not rendered — `recentTransactions` is empty with placeholder API keys (2.0.4 territory); its `enabled = false` change is verified in the diff, not on device | ✅ (code) / ⚠️ not exercisable on device |
| M7 | Tab walk | History/DApp/NFT/Settings each show their informative "Coming in Phase X" placeholder; Home restores its back stack (token detail) per AC-1.7 | ✅ placeholders unchanged, not dead ends |
| M8 | Crash watch | `logcat` `FATAL EXCEPTION` count during the walk: **0** | ✅ |

Screenshots: `/tmp/t203-home.png`, `/tmp/t203-token_detail.png`, `/tmp/t203-token_detail_caption.png`
(the screens set no FLAG_SECURE).

## Deviations from the plan

| # | Plan said | Done | Why |
| --- | --- | --- | --- |
| V1 | `QuickActionButton` gains an `enabled` parameter | Parameter dropped; the button is unconditionally disabled (empty `onClick`, `enabled = false`) and its label dimmed | The button has no enabled state until 2.6/2.7/3.3 land, so a parameter would be dead API; also removed a conditional that ktlint flagged. |

## Handoff

| Deferred | Why | Owner |
| --- | --- | --- |
| Re-enabling the buttons with real navigation callbacks | The destinations arrive with their items (Send 2.6, Receive 2.7, Swap 3.3, History 2.8); each item must restore the removed `HomeScreen`/`TokenDetailScreen` callback parameters and the `MainScreen` wiring | **2.6 / 2.7 / 2.8 / 3.3** |
| Exercising the disabled "See All" on a device | Needs real transaction history, which needs API keys (2.0.4) and the history implementation (2.8) | **2.8** |
| Hardcoded placeholder-tab strings in `app` | Out of scope here; string adoption is 4.17 | **4.17** |
