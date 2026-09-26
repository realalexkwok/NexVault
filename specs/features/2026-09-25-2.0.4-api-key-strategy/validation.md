# 2.0.4 — API-key strategy — Validation record

> Status: **IMPLEMENTED + DEVICE-WALKED 2026-09-25 — AWAITING OWNER CONFIRMATION.** The single open
> finding (D2, Etherscan V1) was **decided by the owner on 2026-09-26**: the migration becomes its
> own roadmap item, **2.0.4b** — see §D2 below. What still blocks 2.0.4's own closure is the owner's
> confirmation and the merge of `feature/2.0.4-api-key-strategy` to `main`.
> Requirements: `requirements.md` (Q1–Q3, per-screen table). Plan: `plan.md`.
> Branch: `feature/2.0.4-api-key-strategy`; the change set is committed there (owner-directed
> commit for review, 2026-09-25).

## Automatic half

| # | Check | Command / method | Result |
| --- | --- | --- | --- |
| A1 | Loader proof | parsed the generated `BuildConfig.java`; lengths only, values masked | all five fields **non-empty**: Infura 32, Alchemy 26, CoinGecko 27, Etherscan 34, WalletConnect 32 chars — the inline-comment pollution is gone |
| A2 | Build | `./gradlew :app:assembleDebug` | **BUILD SUCCESSFUL**; APK `app/build/outputs/apk/debug/app-debug.apk` |
| A3 | Unit tests | `./gradlew testDebugUnitTest :domain:test --continue` | **BUILD SUCCESSFUL — 255 tests, 0 failed, 1 skipped** (249 → 255: +6 new data tests) |
| A4 | Gate delta vs `main` (`98da081`) | `./gradlew detekt ktlintCheck --continue` twice (branch, then `git stash push -u` for the main baseline) | **detekt 65 → 65, ktlint 1471 → 1471**, **no file worse than main** |
| A5 | Unconfigured paths never hit the network | new `TokenRepositoryImplTest` (4) + `TransactionRepositoryImplTest` (2) | unconfigured RPC/explorer → `DataResult.Error(ApiKeyNotConfiguredException)` with `coVerify(exactly = 0)` on `Web3jProvider`/`BlockExplorerApiFactory`; configured paths still reach them |

**Baseline-number correction (honesty note).** Earlier records quoted ktlint **1454** for `main`.
A fresh, fully-executed run gives **1471**: the 1454 figure came from a run in which several ktlint
tasks were `UP-TO-DATE` and therefore printed no findings. The 2.0.3 conclusion is unaffected —
its per-file comparisons were branch-vs-stash in one session, and those files still match their
baselines exactly.

## Manual half — device walk (Pixel 6a, 2026-09-25)

| # | Step | Observed | Verdict |
| --- | --- | --- | --- |
| M1 | Install with the real keys, unlock (PIN) → Home | no "not configured" state; token rows show live CoinGecko 24h changes (`+0.6%`); `okhttp` log shows `api.coingecko.com` with **HTTP 200** | ✅ AC-2 |
| M2 | Open Ethereum → TokenDetail | **Price `$2,691.24`, 24h Change `+0.58%`** (host-side curl minutes earlier: `$2,691.96`) and the chart renders — no "No chart data available" | ✅ real data flows |
| M3 | RPC path | `refreshBalances` reaches `ethGetBalance` and returns (instrumented run: `rpc start` → `rpc ok`, balance 0 wei for this wallet) | ✅ |
| M4 | Explorer path | `okhttp` shows `api.etherscan.io` being called — see **D2**: the V1 endpoint answers `NOTOK` | ⚠️ D2 |
| M5 | Key-absent check | blanked `INFURA_API_KEY` (local.properties backed up, values never printed), rebuilt + reinstalled → Home shows **"Ethereum RPC is not configured. Add INFURA_API_KEY to local.properties and rebuild."** and no generic error (screenshot `/tmp/t204-not-configured.png`) | ✅ AC-3 |
| M6 | Restore | key restored, rebuilt + reinstalled → notice gone, live changes back (`+0.6%`), HTTP 200s | ✅ |
| M7 | Crash watch | no `FATAL EXCEPTION` during any of the above | ✅ |

Screenshots: `/tmp/t204-home-live.png`, `/tmp/t204-token-detail-live.png`,
`/tmp/t204-not-configured.png`.

## Findings

### D1 — Moshi codegen was never wired: every Retrofit call failed before any I/O (FIXED)

The walk started with **zero** HTTP requests and no error surfaced. Instrumentation (temporary
`Log.i`, since removed) showed the CoinGecko call dying instantly:

```
prices call failed: IllegalArgumentException: Unable to create converter for
java.util.Map<String, TokenPriceResponse> for method CoinGeckoApi.getTokenPrices
```

Cause: `core:core-network` has `@JsonClass(generateAdapter = true)` DTOs and `moshi-kotlin-codegen`
only inside `libs.bundles.networking` (an `implementation` dependency) — **no `ksp(...)`
declaration**, so no adapters were generated (0 `*JsonAdapter*` files in the module build dir) and
Moshi could not convert any response type.

Fix: `ksp(libs.moshi.kotlin.codegen)` in `core/core-network/build.gradle.kts`. Evidence after the
fix: **42 generated adapters**, CoinGecko `200` responses, live price/chart on device (M1–M2).
This defect predates 2.0.4 — it is why every network-backed screen has looked empty since Phase 2 —
and would have made the owner's keys useless.

### D2 — Etherscan V1 endpoint is deprecated (DECIDED 2026-09-26; migration owned by item 2.0.4b)

The app's explorer base URL is `https://api.etherscan.io/` (V1). Etherscan now answers V1 calls
with `{"status":"0","message":"NOTOK","result":"You are using a deprecated V1 endpoint, switch to
Etherscan API V2 …"}` (verified with curl using the owner's key, HTTP 200, body NOTOK). The app
therefore calls the explorer (M4) and always gets zero transactions.

Options for the owner:

| Option | Shape | Trade-off |
| --- | --- | --- |
| A. Migrate to V2 in this item | base URL → `https://api.etherscan.io/v2/`, add the mandatory `chainid` query param; rebuild the explorer DTO calls | V2 requires an API key for **every** chain, so BSC/Polygon history would need BscScan/PolygonScan keys the owner does not have (they currently use empty keys) |
| B. Record as a known limitation, fix with history work | 2.0.4 keeps the honest "not configured"/empty behaviour; the migration lands with the History item | Deferral: transaction history stays empty until then |
| C. Migrate to V2 for mainnet/Sepolia only | chainid for keyed chains, keep the V1 path for BSC/Polygon | Two code paths; BSC/Polygon stay broken anyway |

Recommendation: **B** — 2.0.4's decision was the key strategy, and the explorer rewrite belongs
with the history work (owner **2.8**), which can also decide the BSC/Polygon key story.

**Owner decision (2026-09-26, planning round):** none of A/B/C verbatim — the migration becomes its
own tracked roadmap item, **2.0.4b — Migrate the explorer to Etherscan API V2**, inserted in Phase 2.0
immediately after 2.0.4 and before 2.0.5, with its spec at
`specs/features/2026-09-26-2.0.4b-etherscan-v2-migration/`. The owner chose the **all-four-chains**
shape on the **single V2 key** (C's "mainnet only" and the separate BscScan/PolygonScan keys are
declined), plus error-envelope surfacing; **true pagination stays with 2.8**. A masked probe the same
day (`2.0.4b/validation.md` §P3) then showed the free key reaches 1 / 11155111 / 137 but is
**plan-gated on 56**, so the owner amended the decision: BSC is migrated too, and its plan gate gets
its own explicit state instead of a paid-plan purchase or a hidden chain. Consequences recorded
in that item's `requirements.md`: BSC/Polygon history becomes keyed, superseding this item's
per-screen "never Not configured" row for those chains (their RPCs stay keyless). Starting 2.0.4b
requires this branch to be merged and deleted first.

## Deviations from the plan

| # | Plan said | Done | Why |
| --- | --- | --- | --- |
| V1 | Map the new exception to a new string | A **persistent inline notice** (`HomeUiState.isRpcNotConfigured` / `TokenDetailUiState.isHistoryConfigured`) instead of the transient snackbar | A missing key is a standing condition; a 4-second snackbar is not an explicit state |
| V2 | not mentioned | The build-script loader needs `import java.util.Properties` at the top of `app/build.gradle.kts` | Inside a Kotlin DSL script `java` resolves to the Gradle Java plugin extension, so `java.util.Properties` does not compile (`Unresolved reference 'util'`) |
| V3 | not mentioned | Added the missing `ksp(libs.moshi.kotlin.codegen)` (finding D1) | Without it no key can produce data; found by the walk, verified by the same walk |

## Handoff

| Deferred | Why | Owner |
| --- | --- | --- |
| Etherscan V1 → V2 migration (finding D2) | Owner decided 2026-09-26: its own item, **2.0.4b**, after 2.0.4 and before 2.0.5 | **2.0.4b** |
| BSC/Polygon explorer keys | V2 needs a key per chain, and V2's unified key is the one `ETHERSCAN_API_KEY`; the app needs no new key | **2.0.4b** |
| Alchemy key still unused | No failover/RPC-switching this round | 2.6/2.7 (signing/RPC choice) |
| WalletConnect project id | Consumed by nothing until DApp pairing | 3.1 |
| Key-absent states for screens built later (send/receive/swap/history) | Their callbacks and error surfaces arrive with their items | 2.6/2.7/3.3/2.8 |

---

## Reviewer verification — commit 3b57c48 (2026-09-26)

> Code-reviewer session (read-only on production code; record files only). Re-verified from the
> tree and by fresh runs; nothing taken on trust.

### Diff review — verdicts

| # | Criterion | Check | Verdict |
| --- | --- | --- | --- |
| R1 | AC-1 loader | `app/build.gradle.kts` loads `local.properties` explicitly (with `-P` override precedence); `apiKey()` helper; **values never printed**. Generated `BuildConfig.java` value lengths measured masked: Infura 32, Alchemy 26, CoinGecko 27, Etherscan 34, WalletConnect 32 — all **non-empty**, matching the developer's claim exactly; `git check-ignore local.properties` → ignored; the commit's file list contains no `local.properties`/`gradle.properties`. | ✅ |
| R2 | AC-3 key-absent guard | `ChainConfigProvider.isRpcConfigured`/`isExplorerConfigured` keyed to chains {1, 11155111} only (BSC/Polygon public, never "not configured"); `TokenRepositoryImpl` (balances + ERC-20 metadata) and `TransactionRepositoryImpl` (history refresh) fail **before** touching `Web3jProvider`/`BlockExplorerApiFactory` with the new, distinct `ApiKeyNotConfiguredException` | ✅ |
| R3 | AC-3 tests | `TokenRepositoryImplTest` + `TransactionRepositoryImplTest` assert the exception type **and** `coVerify(exactly = 0)` on the network providers for the unconfigured paths; configured paths still reach them | ✅ |
| R4 | AC-4 CoinGecko | no `ApiKeyNotConfiguredException` wiring on any CoinGecko path; the key-optional interceptor behavior is unchanged | ✅ |
| R5 | UI states | `HomeUiState.isRpcNotConfigured` / `TokenDetailUiState.isHistoryConfigured` drive **persistent inline notices** (V1 deviation — correct: a standing condition is not a snackbar); generic network/refresh errors keep their existing strings; the two new strings exist in each feature module's `values/strings.xml` | ✅ |
| R6 | D1 fix | `ksp(libs.moshi.kotlin.codegen)` added to `core:core-network` — the root cause of every empty network screen since Phase 2; the fresh build generates the adapters (build passed with the processor wired) | ✅ |
| R7 | D2 (open) | The Etherscan **V1** endpoint is deprecated and answers `NOTOK`; the fix is deliberately deferred pending the owner's choice (options A/B/C in the developer's findings; recommendation **B** — migrate with the history work, owner 2.8) | ⚠️ **needs owner decision** |

### Fresh automatic evidence (reviewer's own runs)

| # | Check | Command | Result |
| --- | --- | --- | --- |
| R8 | Build + tests | `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --continue` | **PASS — 255 tests, 0 failures, 0 errors, 1 skipped** (249 → 255, +6 — matches claim) |
| R9 | Gates (reviewer basis) | `detekt ktlintCheck` | detekt 10 tasks / **78 weighted** — unchanged from the reviewer's baseline (their 65→65 / 1471→1471 bases); ktlint 39 tasks unchanged. **No new findings in touched files.** |
| R10 | Device (reviewer) | launch on the Pixel 6a | app process runs; the screen was locked/off at review time, so the live-data walk (M1–M2) and the key-blanked notice (M5/M6) remain developer-recorded — the manual half is the owner's confirmation. |

### Verdict

**PASS (automatic + diff).** The key strategy is implemented exactly as decided: real keys now reach
`BuildConfig`, keyed surfaces fail fast with an explicit, persistent "not configured" state and
never touch the network, CoinGecko stays key-optional, and the D1 Moshi-codegen defect — the reason
every network screen looked empty — is fixed and covered by the green suite. **Blocking for
closure: the owner's decision on D2 (Etherscan V1 → V2 migration), then the owner merge of
`feature/2.0.4-api-key-strategy` to `main`.**
