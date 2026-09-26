# 2.0.4b — Migrate the explorer to Etherscan API V2 — Requirements

> Roadmap item: **2.0.4b** (Phase 2.0), inserted **2026-09-26** immediately after 2.0.4, before 2.0.5
> (the same insert pattern as 2.0.2b). Plan: `plan.md`. Evidence: `validation.md`.
> Branch: `feature/2.0.4b-etherscan-v2-migration` — created from `main` **only after**
> `feature/2.0.4-api-key-strategy` is merged and deleted (a feature branch is never stacked on an
> unfinished one).
> Owner decisions recorded 2026-09-26 (planning round).

## Problem

2.0.4's device walk (M4) found that the app talks to Etherscan **V1**:

- `core/core-network/.../config/ChainConfigProvider.kt:75-102` sets `explorerApiBaseUrl` to
  `https://api.etherscan.io/` (mainnet), `https://api-sepolia.etherscan.io/`, `https://api.bscscan.com/`,
  `https://api.polygonscan.com/`;
- `core/core-network/.../api/BlockExplorerApi.kt` is `@GET("api")` with **no `chainid`**;
- Etherscan answered the walk with HTTP 200 and
  `{"status":"0","message":"NOTOK","result":"You are using a deprecated V1 endpoint, switch to Etherscan API V2 …"}`.
  V1 was fully deprecated by 15 August 2025
  ([Etherscan KB](https://kb.etherscan.com/etherscan-api-v1-will-be-fully-deprecated-by-15th-august-2025)).

So **transaction history can never return data**, and because 2.0.4's DTOs declare
`result: List<TransactionDto>` (non-null), an error envelope does not even parse: the failure is
swallowed by the repository's generic `catch` and surfaces as nothing. The D2 finding was left open
for the owner (options A/B/C in 2.0.4's `validation.md` §D2); the owner's answer is this item.

**V2 model** (the reason the item is shaped this way): one endpoint
`https://api.etherscan.io/v2/api` serves every supported chain, the `chainid` query parameter is
mandatory, and **one Etherscan V2 key works across chains** — including Polygon (137)
([V2 announcement](https://info.etherscan.com/switch-to-etherscan-api-v2-by-may-31-2025/)).
**But plan coverage is not uniform** (verified 2026-09-26, masked probe; see `validation.md` §P3):
the owner's free key answers `status:"1"` for chainid 1, 11155111 and 137, and for chainid **56
(BNB Smart Chain)** answers `status:"0"` / NOTOK — *"Free API access is not supported for this chain.
Please upgrade your api plan for full chain coverage."* So BSC history is **configured but
plan-gated**, which is a different state from "key missing" and must be surfaced as such.

Two 2.0.4 decisions have to move with it:

1. 2.0.4's per-screen table states BSC/Polygon **never** show "Not configured" because they are
   keyless by design. Under V2 every explorer call is keyed, so that row is superseded (below).
2. `isExplorerConfigured` keys only chains {1, 11155111}; under V2 the key requirement covers all
   four chains, and `isRpcConfigured`'s keyless-BSC/Polygon rule is unaffected (their RPCs stay
   public).

## Owner decisions (Q/A, 2026-09-26)

| # | Question | Answer |
| --- | --- | --- |
| Q1 | Where does the migration sit in the strict order? | **New item 2.0.4b**, inside Phase 2.0, right after 2.0.4, before 2.0.5 — not folded into 2.8 and not left as an untracked deferral. |
| Q2 | Chain coverage and key model? | **All four chains through one `ETHERSCAN_API_KEY`** (the V2 unified key). BSC/Polygon stop being keyless; their RPCs stay public. No new `local.properties` keys. **Amended 2026-09-26 after the masked probe:** the free key serves 1 / 11155111 / 137 and is **plan-gated on 56**; the owner chose to migrate all four anyway and surface BSC's plan gate explicitly (Q6) rather than buy a paid plan or hide the chain. |
| Q3 | Item scope? | **Endpoint + `chainid` + NOTOK surfacing.** The explorer's error envelopes must stop being silently discarded. True pagination stays with 2.8. |
| Q4 | What does this session deliver? | The roadmap row, this spec directory, and the owner's D2 decision recorded in 2.0.4's `validation.md`. **No production code this pass.** |
| Q5 | What counts as verified? | Unit tests on the **request shape**, a masked live `curl` against V2, and a device walk on a **known-active** address (owner-supplied — the current test wallet holds 0 wei, so an empty list there proves nothing). |
| Q6 | How is the BSC plan gate handled? | **Detect it from the explorer's own message** (not by hard-coding chain 56) and map it to a **distinct exception + a persistent UI notice** ("not covered by the current Etherscan plan"), so any plan-gated chain is handled and a future paid plan starts working without code changes. |

### Per-screen impact (supersedes the BSC/Polygon row of 2.0.4's table)

| Surface | Today (V1) | After 2.0.4b (V2) |
| --- | --- | --- |
| Ethereum mainnet / Sepolia — history | `NOTOK`, silently empty | live: `chainid` 1 / 11155111 + key |
| Polygon — history | dead V1 endpoint, no key | live: `chainid` 137 + the same key |
| BSC — history | dead V1 endpoint, no key | request is V2 + `chainid` 56 + the same key, but the **free plan answers NOTOK** → explicit "not covered by the current Etherscan plan" notice; starts working unchanged if a paid plan is added |
| BSC / Polygon — balances / RPC | public endpoint, no key | unchanged — no key required |
| All four — key blank | n/a (BSC/Polygon keyless) | explicit "Not configured" (2.0.4 state, now on every chain) |
| CoinGecko surfaces | key-optional | unchanged (2.0.4 decision) |

## Acceptance criteria

| AC | Criterion |
| --- | --- |
| AC-1 | Every explorer request goes to `https://api.etherscan.io/v2/api` and carries `chainid` equal to the chain being queried — asserted on the built request, not on a constant. |
| AC-2 | The **same** `ETHERSCAN_API_KEY` is configured for all four chains; no supported chain is left with an empty explorer key. (Plan coverage is a separate axis: 56 is plan-gated on free — that is AC-8, not a key-configuration defect.) |
| AC-3 | `isExplorerConfigured` requires the key for **every** supported chain: with it blank, the affected history surface shows the explicit "Not configured" state and the network layer is never called (extends 2.0.4 AC-3 to BSC/Polygon); `isRpcConfigured` behaviour for 56/137 is unchanged. |
| AC-4 | Explorer **error envelopes are surfaced**: `status:"0"` with a non-list `result` (deprecated endpoint, invalid key, rate limit) yields `DataResult.Error` carrying the explorer's message; a legitimate `"No transactions found"` (`result: []`) stays a successful empty result, not an error. |
| AC-5 | Masked live proof: `curl` against V2 returns `status:"1"` for mainnet/Sepolia/Polygon addresses with history, and `chainid=56` returns the documented plan-gate response — recorded as observed, key never printed. |
| AC-6 | Device walk: TokenDetail history lists real transactions for a known-active address on the Pixel 6a; 0 crashes; screenshots recorded. |
| AC-7 | Build green; full suite green; detekt/ktlint totals not worse than the merge base with no touched file worse; no new dependency outside `specs/tech-stack.md`. |
| AC-8 | A plan-gated chain is reported **as such**: the explorer's plan-gate message maps to a distinct exception and a persistent, explicit notice (not a generic error, not a silent empty); the mapping is message-driven, so no chain id is hard-coded, and the unit test covers the exact observed wire body. |

> **SUSPENDED 2026-09-26 (owner decision): funding the walk wallet is on hold — the relative test
> cases are suspended until the balance is not zero.** The owner is not buying or transferring funds
> for the device walk for now, and the imported wallet's active account holds 0 wei on every readable
> chain, so the balance-dependent part of AC-6 (mainnet/Polygon history on a funded address) cannot be
> exercised. **Suspend M1 and M2** (and precondition P4) until the active account's balance is
> non-zero; M3 (BSC plan notice), M4 (blank-key state) and M5 (crash watch) need no balance and remain
> walkable. The criteria above are unchanged — only their evidence is deferred. Details, the
> zero-balance probe and the exact resume condition: `validation.md` §Suspended.

## Non-goals

- `getTransactionHistory` true pagination (`page`/`pageSize` are ignored today) and the History UI —
  roadmap **2.8**, which now consumes a working V2 source instead of a dead one.
- `updateTransactionStatus` not consulting the chain — 2.8.
- The three unimplemented `TransactionRepositoryImpl` write paths — 2.6.
- RPC failover / Alchemy usage (2.6/2.7), WalletConnect key usage (3.1).
- Reworking 2.0.4's "not configured" UX beyond extending it to BSC/Polygon.
- Any new `local.properties` key, any BscScan/PolygonScan-specific key (V2 removes the need; option C
  of the D2 table is explicitly declined).
- **Buying a paid Etherscan plan** — a billing decision, not code. The item must behave correctly
  whether or not the plan covers 56 (Q6), and a paid plan needs no code change to take effect.
- Removing BNB Smart Chain from the wallet's supported chains: balances/RPC keep working there.
