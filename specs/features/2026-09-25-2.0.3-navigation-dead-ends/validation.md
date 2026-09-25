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

---

## Reviewer verification — commit 72732f2 (2026-09-25)

> Code-reviewer session (read-only on production code; record files only). Re-verified from the
> tree and by fresh runs; nothing taken on trust.

### Diff review — verdicts

| # | Criterion | Check | Verdict |
| --- | --- | --- | --- |
| R1 | AC-1 no empty-lambda wiring | `grep -nE '= \{ \}|= \{ _, _ -> \}'` on `MainScreen.kt` → **0**; the six callback **parameters are fully deleted** from `HomeScreen`/`TokenDetailScreen` and their call sites — no dead affordance survives anywhere in `app`/`feature` | ✅ |
| R2 | AC-2 Home controls | `QuickActionButton` is unconditionally `onClick = { }` + `enabled = false` (label dimmed `alpha 0.5f`); caption `home_actions_coming_soon` renders under the row ("Send and receive arrive in Phase 2; swap in Phase 3.") | ✅ |
| R3 | AC-3 TokenDetail controls | Send/Receive `Button`/`OutlinedButton` disabled + caption `token_detail_actions_coming_soon`; **See All** `TextButton` `onClick = { }` + `enabled = false`; KDoc updated; the three callback params deleted | ✅ |
| R4 | V1 deviation | The plan's `enabled` parameter was dropped in favour of unconditionally disabled buttons — correct call: dead API until 2.6/2.7/3.3 land; documented in the spec's deviations table | ✅ |
| R5 | D2 strings | The two captions are new English strings in each feature module's `values/strings.xml` (ZH i18n is 3.7) | ✅ |
| R6 | Placeholders untouched | The 4 tab placeholders are informative by design (not dead ends) and unchanged — consistent with the requirements' reading of 2.0.3 | ✅ |

### Fresh automatic evidence (reviewer's own runs)

| # | Check | Command | Result |
| --- | --- | --- | --- |
| R7 | Build + tests | `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --continue` | **PASS** — **249 tests, 0 failures, 0 errors, 1 skipped** (unchanged, matches claim) |
| R8 | Gates (reviewer basis) | `detekt ktlintCheck` | detekt 10 tasks / **78 weighted** — unchanged from the reviewer's 2.0.2b baseline (their 65→65 uses raw counts); ktlint 39 tasks unchanged. **No new findings in the touched files.** |
| R9 | Device | `adb devices` | **No device attached during the review** (the Pixel 6a was offline) — the M1–M8 walk evidence remains developer-recorded; the manual half is the owner's confirmation. |

### Verdict

**PASS (automatic + diff).** The change set satisfies the 2.0.3 exit criteria in code: every formerly
silent control is now disabled with a persistent, phase-naming caption, and the empty-lambda wiring
is gone rather than hidden. Remaining for closure: the owner's manual-half confirmation of the
device walk (M1–M8) and the owner merge of `feature/2.0.3-navigation-dead-ends` to `main`.
