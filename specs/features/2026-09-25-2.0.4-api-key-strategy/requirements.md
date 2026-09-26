# 2.0.4 — API-key strategy — Requirements

> Roadmap item: **2.0.4 — Decide the API-key strategy** (Phase 2.0).
> Branch: `feature/2.0.4-api-key-strategy`. Plan: `plan.md`. Evidence: `validation.md`.
> Owner decisions recorded 2026-09-25 (planning round).

## Problem

The roadmap asked for a decision because all five `local.properties` keys were `your_key_here`.
Two things changed since then, and two defects were found while planning:

1. The owner **supplied all five real keys** (2026-09-25) — but
2. `app/build.gradle.kts:38-62` reads them with `project.findProperty`, and **nothing loads
   `local.properties`** (nor `~/.gradle/gradle.properties`) into Gradle project properties, so
   `BuildConfig` currently carries **empty strings**. The keys never reach the app.
3. No code distinguishes "key missing" from "call failed": the RPC/explorer paths attempt the call
   regardless of whether the key exists (`TokenRepositoryImpl.kt:70-76,180-187`,
   `TransactionRepositoryImpl.kt:86-87`), and Home maps every failure to the generic
   `home_error_network` / `home_error_refresh_failed` strings (`HomeViewModel.kt:124-136`).
4. A stale formatting trap was fixed in passing: the Alchemy/CoinGecko lines in `local.properties`
   had **inline `#` notes**, which `java.util.Properties` treats as part of the value; they are now
   on their own comment lines.

## Owner decisions (Q/A, 2026-09-25)

| # | Question | Answer |
| --- | --- | --- |
| Q1 | Supply real keys, or keep placeholders and treat key-absent degradation as the supported path? | **Real keys are the supported path** (the owner supplied them). Key-absent remains a supported fallback for fresh clones / placeholder builds and must behave explicitly. |
| Q2 | What does a *keyed* surface do when its key is absent? | **Explicit "Not configured" state** — a distinct message naming the missing key and the fix, separate from real network failures, which keep today's empty/error/retry states. |
| Q3 | Does CoinGecko get the same treatment? | **No — CoinGecko stays key-optional.** Its free tier works key-less and `CoinGeckoApiKeyInterceptor` already skips the header when blank; no "Not configured" state for CoinGecko surfaces. |

### Per-screen decision table (AC-6)

| Surface | Key | Key present, call fails | Key absent |
| --- | --- | --- | --- |
| Home — balances / portfolio refresh | Infura (RPC) | `home_error_network` / `home_error_refresh_failed` (unchanged) | **"Not configured"** (new string, names `INFURA_API_KEY`) |
| Home — price chart | CoinGecko | existing loading/empty behavior | unchanged — key-optional |
| TokenDetail — price / chart | CoinGecko | "No chart data available" (unchanged) | unchanged — key-optional |
| TokenDetail — recent transactions / history refresh | Etherscan (explorer) | existing error/empty behavior | **"Not configured"** (new string, names `ETHERSCAN_API_KEY`) |
| TokenDetail — send / receive / See All | — | disabled until 2.6/2.7/2.8 (2.0.3) | n/a |
| Placeholder tabs (History/DApp/NFT/Settings) | — | informative "Coming in Phase X" | n/a |
| BSC / Polygon paths | public RPC (no key) | existing behavior | never "Not configured" — no key required |
| WalletConnect surfaces | WalletConnect project id | not wired yet (3.1) | out of scope |

## Acceptance criteria

| AC | Criterion |
| --- | --- |
| AC-1 | `local.properties` values reach `BuildConfig` through the new loader; verified non-empty **without printing any value**; the file stays git-ignored. |
| AC-2 | With keys present the app behaves as before — no new states, no crashes (device walk). |
| AC-3 | With a keyed dependency absent, the affected surface shows the explicit "Not configured" message and the network layer is **never called** (unit-tested). |
| AC-4 | CoinGecko surfaces never show "Not configured". |
| AC-5 | Build green; full suite green; detekt/ktlint totals identical to `main` (65 / 1454) with no new findings in touched files. |
| AC-6 | The per-screen table above is recorded here and referenced by `validation.md`. |

## Non-goals

- Alchemy failover or RPC switching (2.6/2.7 own signing/RPC choice); WalletConnect key usage (3.1).
- Startup key validation calls; key rotation/secret management beyond git-ignore hygiene.
- 2.0.5 gate debt, 2.0.6 TC traceability, placeholder-tab content.
- Any change under `doc/` or the legacy CR records.
