# 2.0.4b — Migrate the explorer to Etherscan API V2 — Validation record

> **Status: CLOSED 2026-09-26 as `[~]` — MERGED TO `main`, NOT VERIFIED (owner-directed; §Close).**
> The automatic half is complete, and the reviewer session verified it **PASS** (`744251d` on
> `e82eba2`, R1–R9 — its R10 device re-walk was blocked by a locked phone and left owner-pending). The
> manual half is **not** verified: M1/M2 are suspended with the funding decision and M3–M5 have not
> been run. The roadmap row therefore stays **implemented-unverified** (`[~]`) and may only move to
> `[x]` when M1/M2 pass after funding.
> Requirements: `requirements.md` (Q1–Q6, AC-1–AC-8). Plan: `plan.md`.

**Host used for every command below:** Linux (`superguo-SQM2270`), `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
(the Linux recipe of `specs/dev-environment.md`; the macOS JBR path in the same doc does not exist on
this host). Android SDK `/home/superguo/Android/Sdk`. All commands from the repository root.

## Preconditions

| # | Precondition | State |
| --- | --- | --- |
| P1 | `feature/2.0.4-api-key-strategy` merged to `main`, branch deleted | ✅ done 2026-09-26 (`main` = `origin/main` = `e4d1f02`) |
| P2 | Branch cut from the updated `main`: `feature/2.0.4b-etherscan-v2-migration` | ✅ created from `e4d1f02`; no commits on it yet (work left in the tree for owner review) |
| P3 | Masked `curl` proves which chains the owner's V2 key reaches | ✅ run 2026-09-26 and re-run at implementation (A6) |
| P4 | Known-active wallet addresses (mainnet + Polygon) for the device walk | ⏸ **SUSPENDED 2026-09-26 — funding on hold (owner); resumes when the active account's balance is non-zero (§Suspended)** |

### P3 / A6 — masked V2 probe (2026-09-26; key never printed, length only)

`curl -s -G https://api.etherscan.io/v2/api`; `apikey` read from `local.properties` into a shell
variable (`ETHERSCAN_API_KEY`, length 34, value never echoed or logged); only `status` / `message` /
row count recorded. The addresses are public, well-known accounts with history — **not** the owner's
wallet, which is why the device walk still needs P4.

| `chainid` | Action / address | Observed |
| --- | --- | --- |
| 1 | `balance` | `status:"1"`, `message:"OK"`, value returned |
| 1 | `txlist`, `0xd8dA…6045` (`page=1`, `offset=3`) | `status:"1"`, `message:"OK"`, **3 rows** |
| 11155111 | `balance` | `status:"1"`, `message:"OK"`, value returned |
| 11155111 | `txlist`, `0xFF50…7b2b` (`page=1`, `offset=3`) | `status:"1"`, `message:"OK"`, **3 rows** |
| 137 | `balance` | `status:"1"`, `message:"OK"`, value returned |
| 137 | `txlist`, `0xd8dA…6045` (offset only) | `status:"1"`, `message:"OK"`, 1000 rows (no `page` sent; V2 answers its default page size) |
| 56 | `balance` and `txlist` | `status:"0"`, `message:"NOTOK"`, `result` (String): *"Free API access is not supported for this chain. Please upgrade your api plan for full chain coverage. https://etherscan.io/apis"* — the **plan gate** |

Reading: the existing key works on 1 / 11155111 / 137 through the V2 route; 56 is **configured but
plan-gated** (AC-8), exactly as the planning probe found. No billing change was made.

## Automatic half — commands and observed results (2026-09-26)

| # | Check | Command | Observed |
| --- | --- | --- | --- |
| A1 | Request shape (AC-1) | `./gradlew :core:core-network:testDebugUnitTest` → `BlockExplorerRequestShapeTest` | **2/2 pass.** The test builds the real `BlockExplorerApi` through Retrofit with a capturing `okhttp3.Interceptor` and asserts the **built** request for chains 1 / 11155111 / 56 / 137: scheme `https`, host `api.etherscan.io`, path `[v2, api]`, `chainid=<that chain>`, `module=account`, `apikey`, `address`, `startblock=0`, `endblock=99999999`, `offset=20`, `sort=desc`, and `tag=latest` for `action=balance`. Sample built URL: `https://api.etherscan.io/v2/api?chainid=1&module=account&action=balance&address=0x1234…&tag=latest&apikey=…` |
| A2 | Key model (AC-2, AC-3) | `:core:core-network:testDebugUnitTest` → `ChainConfigProviderTest` | **5/5 pass.** All four chains: `explorerApiBaseUrl == "https://api.etherscan.io/v2/"` and `explorerApiKey == etherscanApiKey`; `isExplorerConfigured` true for all four with a key and false for all four when it is blank; `isRpcConfigured` unchanged (1/11155111 need Infura, 56/137 public); unknown chain (10) → no config, both `…Configured` false |
| A3 | Envelope handling (AC-4, AC-8) | `:core:core-network:testDebugUnitTest` → `EtherscanEnvelopeParsingTest` (7) and `:data:testDebugUnitTest` → `TransactionRepositoryImplTest` (7) | **7/7 and 7/7 pass.** Parsing: success rows; `"No transactions found"` + `[]` → no error; deprecated-V1 String → `ExplorerApiException` with the explorer's text; **verbatim** plan-gate body → `ExplorerPlanUnsupportedException`; `result:null` → error carrying `message`; a malformed row still throws `JsonDataException` (not swallowed). Repository: merges native + token transfers (upsert of 2 rows, `chainId` passed to both calls), V1 rejection → `DataResult.Error(ExplorerApiException)`, plan-gate → `DataResult.Error(ExplorerPlanUnsupportedException)`, `"No transactions found"` → `Success` with no upsert, key-absent guard still short-circuits before the network |
| A4 | Build + suite (AC-7) | `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --continue` | **BUILD SUCCESSFUL**; **274 tests, 0 failures, 0 errors, 1 skipped** (255/0/1 at the 2.0.4 baseline; +19 new tests). Artifact: `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk` (47,986,669 bytes) |
| A5 | Gates (AC-7) | `./gradlew detekt ktlintCheck --continue` on the branch, then `git stash push -u` + the same command on `main` (`e4d1f02`), counts taken from the generated reports | **detekt 65 → 65** (the same 10 modules fail as on `main`; all pre-existing) and **ktlint 1589 → 1556** (33 fewer). Per-file check with the same ktlint 1.5.0 the Gradle plugin uses: every touched pre-existing file has the **identical** count to `main` (BlockExplorerApi 1, BlockExplorerApiFactory 6, ChainConfigProvider 7, NetworkModule 15, TransactionRepositoryImpl 23, TokenDetailScreen 19, TokenDetailViewModel 6, Exceptions 0, TokenDetailUiState 0); `TransactionRepositoryImplTest` improved 6 → 0; every new file (adapter factory, envelope DTOs + helper, three `core-network` test files, fixtures) has **0** findings. One `MaxLineLength` finding that the first pass had introduced in the new parsing test was found and fixed; the numbers above are the final run |
| A6 | Live V2 check (AC-5) | masked `curl` per §P3/A6 above | Recorded as observed: `"status":"1"` with rows for 1 / 11155111 / 137, and the plan-gate envelope for 56. Key never printed |

`spotless` was not used as evidence (it has no configuration block — a no-op).

## Manual half — device (Pixel 6a) — NOT RUN (M1/M2 suspended, M3–M5 walkable)

| # | Step | Observed | Verdict |
| --- | --- | --- | --- |
| M1 | Real keys, **owner-supplied** known-active address → TokenDetail history (mainnet) | ⏸ **SUSPENDED — zero balance, funding on hold (§Suspended)** | ⏸ |
| M2 | Same walk on Polygon (`chainid` 137, one key) | ⏸ **SUSPENDED — zero balance, funding on hold (§Suspended)** | ⏸ |
| M3 | Same walk on BSC (`chainid` 56) → **explicit plan notice**, not an empty list (AC-8) | ☐ not run | ☐ |
| M4 | Blank `ETHERSCAN_API_KEY` (backup → blank → rebuild → restore) → explicit "Not configured" on **BSC/Polygon too** (new behaviour) | ☐ not run | ☐ |
| M5 | Crash watch (`FATAL EXCEPTION`) | ☐ not run | ☐ |

Screenshots: *(none yet)*. **P4 blocker, now suspended:** the 2.0.4 test wallet holds 0 wei and so
does the account imported on 2026-09-26, so an empty history there proves nothing — see §Suspended.

## Suspended — funding on hold (owner decision, 2026-09-26)

**Owner decision:** the funding route for the walk wallet is skipped for now (the Ethereum-ATM /
on-ramp investigation of 2026-09-26 was set aside). No funds were bought or transferred, and the
balance-dependent test cases are suspended with it.

**Evidence that triggers the suspension** (masked V2 probe, key never printed). The account imported
into the app on 2026-09-26 is `0x2477009C5AF3aEa0B7Bf73B60392df7dbA7E9254`, derived at
`m/44'/60'/0'/0/0` — read from the device's own wallet metadata via `run-as` (public value only).

| `chainid` | `balance` | `txlist` | `tokentx` |
| --- | --- | --- | --- |
| 1 | 0 wei | 0 rows | 0 rows |
| 137 | 0 wei | 0 rows | 0 rows |
| 11155111 | 0 wei | 0 rows | 0 rows |
| 56 | plan-gated (`NOTOK`) | — | — |

**Consequence — suspended test cases.** A zero balance means there is no real history to list, and an
empty list is a *legitimate* result, so running these rows would prove nothing:

| Row | State |
| --- | --- |
| M1 — mainnet history | ⏸ suspended |
| M2 — Polygon history | ⏸ suspended |
| P4 — known-active addresses | ⏸ suspended with them |

**Still walkable without funding** (no balance involved): M3 (BSC shows the plan notice), M4 (blank
key → "Not configured" on all four chains), M5 (crash watch).

**Resume condition (owner's rule):** the rows stay suspended **until the balance is not zero**. When
the owner funds the active account, the implementing session re-probes it *before* the walk — the
proof needs `txlist`/`tokentx` **rows** on chains 1 and 137, because a non-zero balance alone is not
enough: `0xE8De…56Fe` holds 42 293 POL on Polygon and still answers `No transactions found`, that
money having arrived as an internal transaction, which this item does not query. M1/M2 then run on
the device as written.

## Decisions taken at implementation (2026-09-26)

| Decision | Choice / reason |
| --- | --- |
| `BlockExplorerApi.getBalance` (no caller) | **Migrated, not deleted** — one extra `@Query("chainid")` keeps the whole interface on V2 and risks nothing; 2.8's history work may want it. This is the choice `plan.md` TG1 asked to record |
| Lenient envelope binding (TG3) | **A `JsonAdapter.Factory`** (`EtherscanEnvelopeAdapterFactory` + `LenientListEnvelopeAdapter`), **not** the first attempt with `@FromJson`/`@ToJson` methods taking an extra `JsonAdapter` parameter: Moshi rejected that signature while building the tests (`IllegalArgumentException: Unexpected signature for … transactionListToJson`) and the whole suite failed. The factory resolves the row-list adapter lazily from the built `Moshi`, reads `status`/`message` by hand, accepts `result` as array → rows and String → `resultText`, and never throws on a rejection body. Registered in `NetworkModule.provideMoshi()` |
| `EtherscanBalanceResponse` | Left on Moshi codegen: its `result` is a String in both success and rejection envelopes, so it has no parse defect, and it has no caller |
| UI scope for `ExplorerApiException` | Only `ExplorerPlanUnsupportedException` gets a new persistent notice (AC-8). A generic explorer rejection still reaches the caller as `DataResult.Error` carrying the explorer's message (AC-4) but gets no dedicated history banner in this item; richer surfacing belongs to 2.8's History UI. Recorded because `plan.md` left it open |
| Notice text | `token_detail_history_plan_gated` = "Transaction history for this chain is not covered by the current Etherscan plan." It renders in the same place as 2.0.4's "Not configured" notice |

## Superseded records

- 2.0.4's per-screen table says BSC/Polygon are "never Not configured". Under V2 they **are** keyed;
  the superseding row is in this item's `requirements.md` and in that item's D2 resolution note.
- 2.0.4's D2 finding ("Etherscan V1 deprecated") is closed by this item; the migration itself leaves
  `[~]` until the manual half passes.

## Handoff (deferred work and its owner)

| Deferred | Why | Owner |
| --- | --- | --- |
| True pagination (`page`/`pageSize` are still ignored by `getTransactionHistory`) | UI + history work | 2.8 |
| `updateTransactionStatus` not consulting the chain | history work | 2.8 |
| The three unimplemented `TransactionRepositoryImpl` write paths | send flow | 2.6 |
| A dedicated in-app history error banner for generic `ExplorerApiException` | scope decision above; 2.8 owns the History UI | 2.8 |
| Buying a paid Etherscan plan (removes the BSC plan gate) | billing decision, not code; the item behaves correctly either way | owner |

---

## Reviewer verification — commit e82eba2 (2026-09-26)

> Code-reviewer session (read-only on production code; record files only). Re-verified from the
> tree, by fresh runs, and by the reviewer's own masked live probe; nothing taken on trust.

### Diff review — verdicts

| # | Criterion | Check | Verdict |
| --- | --- | --- | --- |
| R1 | AC-1 request shape | `BlockExplorerApi` carries `@Query("chainid")` on all three methods; the factory resolves the single V2 base URL `https://api.etherscan.io/v2/`; the built-request test asserts scheme/host/path/`chainid` — not constants | ✅ |
| R2 | AC-2/AC-3 key model | `ChainConfigProvider` serves all four chains from `ETHERSCAN_V2_BASE_URL` with the same `etherscanApiKey`; `isExplorerConfigured` is a pure key check for **every** supported chain (no chain-id set); `isRpcConfigured` keeps {1, 11155111} keyed and 56/137 public | ✅ |
| R3 | AC-4 envelope surfacing | `EtherscanEnvelopeAdapterFactory` + `LenientListEnvelopeAdapter` parse the array-vs-String `result` union by hand (Moshi codegen cannot express it — the documented reason); `envelopeErrorOrNull`: success → null, array (even `[]` = "No transactions found") → null, rejection String → exception carrying the explorer's text, no payload → `ExplorerApiException(status message)`. Repository: `native.errorOrNull()`/`tokenTx.errorOrNull()` → `DataResult.Error`; rows merged from both calls with `chainId` | ✅ |
| R4 | AC-8 plan gate | message-driven (`message == "NOTOK" && resultText.contains(PLAN_GATE_MARKER)`) → `ExplorerPlanUnsupportedException`; **no chain id hard-coded**; the marker is the verbatim observed wire text; the unit test covers the exact body. UI: `isHistoryPlanGated` → persistent notice string | ✅ |
| R5 | Exceptions | `ExplorerApiException` and `ExplorerPlanUnsupportedException` are distinct from `ApiKeyNotConfiguredException` — the three states (no key / rejected / plan-gated) cannot be conflated | ✅ |
| R6 | Suspension honesty | the owner-suspended funded rows (M1/M2/P4) and the not-run walkable rows (M3–M5) are recorded with the exact resume condition (non-zero balance **with txlist/tokentx rows**, not balance alone) | ✅ |

### Fresh automatic evidence (reviewer's own runs)

| # | Check | Command | Result |
| --- | --- | --- | --- |
| R7 | Build + suite | `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --continue` | **PASS — 274 tests, 0 failures, 0 errors, 1 skipped** (255 → 274, +19 — matches claim) |
| R8 | Gates (reviewer basis) | `detekt ktlintCheck` | detekt 10 tasks / **78 weighted** — unchanged from the reviewer's baseline (their 65→65 / 1589→1556 bases); ktlint 39 tasks unchanged |
| R9 | AC-5 masked live probe (reviewer's own) | `curl` against `/v2/api`, key read into a shell variable (length 34, never printed) | chainid 1 `txlist` → `status:"1"`, `message:"OK"`, **3 rows**; chainid 56 → `status:"0"`, `message:"NOTOK"`, `result` text starts `"Free API access is not supported for this chain…"` — the plan gate, verbatim | ✅ |
| R10 | Device (reviewer) | install + launch attempts on the Pixel 6a | the phone screen was locked/off at review time, so the walkable manual rows (M3–M5) could not be re-driven here; they remain owner-pending as recorded, and M1/M2 stay suspended per the owner's funding decision | ⏸ owner-pending |

### Verdict

**PASS (automatic + diff + live probe).** The V2 migration is implemented exactly as decided: one
host, mandatory `chainid`, one key across all four chains, rejection envelopes surfaced instead of
silently discarded, and the plan gate detected from the explorer's own message with a persistent
UI notice. The roadmap row correctly stays `[~]` until the owner's manual half passes (M3–M5
walkable now; M1/M2 resume when the walk wallet has real history). Remaining for closure: the
owner's manual-half evidence, then the owner merge of
`feature/2.0.4b-etherscan-v2-migration` to `main`.

## Close / sign-off (owner-directed, 2026-09-26)

| # | Step | State |
| --- | --- | --- |
| C1 | Reviewer verification | ✅ **PASS** — reviewer commit `744251d` verifying `e82eba2` (R1–R9; R10 device re-walk blocked by a locked phone) |
| C2 | Implementation committed on the item branch | ✅ `e82eba2` — 23 files: production code, 19 new tests, spec records |
| C3 | Roadmap row | **`[~]` — implemented, NOT verified.** Owner decision: merge now and keep the row implemented-unverified; it moves to `[x]` only when M1/M2 pass |
| C4 | Merge to `main` and push | ✅ `--no-ff` merge commit, pushed to `origin/main`; local `feature/2.0.4b-etherscan-v2-migration` deleted |
| C5 | Verification debt carried forward | M1 (mainnet history) + M2 (Polygon history) **suspended with the funding decision**; M3–M5 **owner-pending** (no balance needed, the reviewer found the phone locked). Resume rule: fund the active account until the balance is not zero, re-probe chains 1/137 for `txlist`/`tokentx` **rows**, walk M1–M5, fill the tables — only then may the row become `[x]` |

**Honesty notes.** (1) Closing as `[~]` is deliberate, not a claim: the code is merged and
reviewer-verified on the automatic/diff/live-probe half, but **no device walk has happened** — that
evidence is owed, not waived. (2) The reviewer's own runs reproduced the developer's numbers (274
tests / 0 failures / 1 skipped; gates unchanged; the masked probe green on chainid 1 and plan-gated on
56), so the automatic half does not rest on a single session's word. (3) Nothing in the funding
suspension changes the code: it ships correct V2 behaviour; what is missing is evidence from a funded
address. (4) A later paid Etherscan plan turns the BSC notice into a live list with no code change
(AC-8/Q6).
