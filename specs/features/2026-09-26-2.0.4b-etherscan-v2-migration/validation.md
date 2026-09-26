# 2.0.4b — Migrate the explorer to Etherscan API V2 — Validation record

> **Status: NOT STARTED (record opened 2026-09-26).** No code has been written for this item and no
> acceptance criterion has been exercised. The only evidence below is the **planning probe P3**, which
> ran before any code and changed one owner decision — it is not verification of the item.
> Requirements: `requirements.md` (owner Q1–Q6, AC-1–AC-8). Plan: `plan.md`.

## Preconditions before any work

| # | Precondition | State |
| --- | --- | --- |
| P1 | `feature/2.0.4-api-key-strategy` merged to `main` and its branch deleted | ☐ pending owner merge |
| P2 | This item's branch cut from the updated `main`: `feature/2.0.4b-etherscan-v2-migration` | ☐ |
| P3 | Masked `curl` proves which chains the owner's V2 key can actually reach | ⚠️ **run 2026-09-26, result below** |
| P4 | Known-active addresses (mainnet + Polygon) for the device walk supplied by the owner | ☐ |

### P3 — masked V2 coverage probe (2026-09-26, planning; key never printed)

`curl -s -G https://api.etherscan.io/v2/api` with `module=account`, `action=txlist`, `offset=3`; only
`status`/`message`/row count recorded; key read from `local.properties` into a shell variable.

| `chainid` | Chain | Observed | Reading |
| --- | --- | --- | --- |
| 1 | Ethereum | `status:"1"`, `message:"OK"`, 3 rows | reachable |
| 11155111 | Sepolia | `status:"1"`, `message:"OK"`, 3 rows | reachable |
| 137 | Polygon | `status:"1"`, `message:"OK"`, 3 rows | reachable |
| 56 | BNB Smart Chain | `status:"0"`, `message:"NOTOK"`, `result` (String): *"Free API access is not supported for this chain. Please upgrade your api plan for full chain coverage. https://etherscan.io/apis"* | **plan-gated, not broken** |

Consequence, taken back to the owner the same day: Q2 was amended (migrate all four chains, surface
the 56 plan gate explicitly) and Q6/AC-8 added (message-driven plan-gate detection + persistent
notice). The V1 → V2 direction itself is confirmed working on three chains with the existing key and
no billing change.

## Automatic half — to be filled by the implementing session

| # | Check | Command / method | Result |
| --- | --- | --- | --- |
| A1 | Request shape (AC-1) | `:core:core-network:testDebugUnitTest` — capturing-interceptor test asserts `/v2/api` + `chainid`/`apikey` for chains 1 / 11155111 / 56 / 137 | ☐ not run |
| A2 | Key model (AC-2, AC-3) | `ChainConfigProviderTest` — non-blank explorer key for every supported chain; `isExplorerConfigured` false for all four when blank; `isRpcConfigured` unchanged for 56/137 | ☐ not run |
| A3 | Envelope handling (AC-4, AC-8) | Moshi round-trip over the four wire shapes + extended `TransactionRepositoryImplTest` (error envelope → `DataResult.Error`, plan-gate envelope → `ExplorerPlanUnsupportedException`, `"No transactions found"` → `Success`) | ☐ not run |
| A4 | Build + suite (AC-7) | `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --continue`; artifact path recorded absolutely | ☐ not run |
| A5 | Gates (AC-7) | `./gradlew detekt ktlintCheck --continue`, compared per file against the post-2.0.4 `main` | ☐ not run |
| A6 | Live V2 check (AC-5) | masked `curl` to `https://api.etherscan.io/v2/api` for `chainid` 1 / 11155111 / 137 (expect `"status":"1"`) and 56 (expect the plan-gate envelope); record `status`/`message`/row count only — **never the key** | ⚠️ run once as probe P3; to be re-run at implementation |

## Manual half — device (Pixel 6a)

| # | Step | Observed | Verdict |
| --- | --- | --- | --- |
| M1 | Real keys, known-active address → TokenDetail history (mainnet) | ☐ not run | ☐ |
| M2 | Same walk on Polygon (`chainid` 137, one key) | ☐ not run | ☐ |
| M3 | Same walk on BSC (`chainid` 56) → **explicit plan notice**, not an empty list (AC-8) | ☐ not run | ☐ |
| M4 | Blank `ETHERSCAN_API_KEY` (backup → blank → rebuild → restore) → explicit "Not configured" on **BSC/Polygon too** (new behaviour) | ☐ not run | ☐ |
| M5 | Crash watch (`FATAL EXCEPTION`) | ☐ not run | ☐ |

Screenshots: *(none yet)*.

## Superseded records

- 2.0.4's per-screen decision table (`../2026-09-25-2.0.4-api-key-strategy/requirements.md`) says
  BSC/Polygon are "never Not configured — no key required". Under V2 they **are** keyed; the
  superseding row is in this item's `requirements.md` and in that item's D2 resolution note.
- 2.0.4's D2 finding ("Etherscan V1 deprecated") is closed by the owner's 2026-09-26 decision to
  create this item; the migration itself remains open until this record's two halves pass.

## Deferrals (pre-registered; finalize as a Handoff list at close)

| Deferred | Why | Owner |
| --- | --- | --- |
| True pagination (`page`/`pageSize` currently ignored by `getTransactionHistory`) | UI + history work | 2.8 |
| `updateTransactionStatus` not consulting the chain | history work | 2.8 |
| The three unimplemented `TransactionRepositoryImpl` write paths | send flow | 2.6 |
| `BlockExplorerApi.getBalance` (no caller) — keep or delete | decided inside TG1 | this item |
