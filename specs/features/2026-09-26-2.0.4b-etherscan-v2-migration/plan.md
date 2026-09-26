# 2.0.4b — Migrate the explorer to Etherscan API V2 — Technical plan

> **Status: IMPLEMENTED 2026-09-26 — automatic half green, manual half pending (`validation.md`).**
> Requirements (Q1–Q6, AC-1–AC-8) in `requirements.md`; evidence in `validation.md`.
> Entry condition: `feature/2.0.4-api-key-strategy` merged to `main` and the branch deleted; this
> item's branch is cut from the updated `main` (never stacked on 2.0.4) — both done 2026-09-26.

## Task groups

### TG1 — V2 endpoint and the mandatory `chainid` (`core:core-network`)

- `config/ChainConfigProvider.kt`: set `explorerApiBaseUrl = "https://api.etherscan.io/v2/"` for
  **all four** chains (1, 11155111, 56, 137). V2 is one host for every chain; the per-chain
  BscScan/PolygonScan/Sepolia hosts disappear.
- `api/BlockExplorerApi.kt`: add `@Query("chainid") chainId: Int` to `getTransactions` and
  `getTokenTransfers` (and `getBalance`, see below). `@GET("api")` on a `…/v2/` base yields exactly
  `https://api.etherscan.io/v2/api`, the canonical V2 route.
  - Rejected alternative: an OkHttp interceptor in `BlockExplorerApiFactory` that appends `chainid`.
    It hides a mandatory protocol parameter from the interface, makes the request shape harder to
    assert, and the factory already caches one `BlockExplorerApi` per chain, so there is no sharing
    benefit to protect.
- `data/TransactionRepositoryImpl.kt:97-98` (`refreshTransactionHistory`): pass the `chainId` the
  method already receives into both calls.
- `BlockExplorerApi.getBalance` is declared but has **no caller** (grep: only the interface).
  **Decided 2026-09-26: migrated, not deleted** — one extra `@Query("chainid")` keeps the whole
  interface on V2 at no risk, and 2.8's history work may want it (`validation.md` §Decisions).

### TG2 — Key model: one V2 key, all chains keyed (`core:core-network`)

- `ChainConfigProvider`: split the constant — `KEYED_RPC_CHAIN_IDS = setOf(1, 11155111)` keeps
  `isRpcConfigured` exactly as 2.0.4 left it (BSC/Polygon RPC stays public and keyless);
  `isExplorerConfigured(chainId)` becomes `configs.containsKey(chainId) && etherscanApiKey.isNotBlank()`.
- `explorerApiKey = etherscanApiKey` for chains 56 and 137 too (today they hard-code `""`).
- **Key configured ≠ data available.** The 2026-09-26 probe showed the free plan refuses `chainid=56`
  (TG3/AC-8 owns that state). `isExplorerConfigured` therefore stays a pure key check — no chain-id
  special case, no attempt to encode plan coverage in the config.
- No `app/build.gradle.kts` change: the five-key set stands (Q2). The BSC/Polygon history surfaces
  now legitimately show 2.0.4's "not configured" notice when `ETHERSCAN_API_KEY` is blank — a
  **user-visible change** that 2.0.4's per-screen table must be read with (superseding row is in
  `requirements.md`).
- Check the two 2.0.4 notice strings: they name `ETHERSCAN_API_KEY`/`INFURA_API_KEY`; nothing there
  claims BSC/Polygon are exempt, so no string change is expected. Confirm by reading them.

### TG3 — Surface explorer error envelopes (the silent-empty defect)

Contract for the three envelope DTOs (`EtherscanTransactionListResponse`,
`EtherscanTokenTransferListResponse`, `EtherscanBalanceResponse`):

| Wire shape | Meaning | Required outcome |
| --- | --- | --- |
| `status:"1"`, `result:[…]` | data | merge into Room, `DataResult.Success(Unit)` |
| `status:"0"`, `message:"No transactions found"`, `result:[]` | legitimate empty | `DataResult.Success(Unit)`, no error, no rows |
| `status:"0"`, `result:"…deprecated V1 endpoint…"` (String, not list) | rejected call | `DataResult.Error(ExplorerApiException(explorerMessage))` |
| `status:"0"`, `result:"Free API access is not supported for this chain. …"` | **plan gate** (observed on `chainid=56`, 2026-09-26) | `DataResult.Error(ExplorerPlanUnsupportedException(explorerMessage))` — see AC-8 |

Implementation notes:

- The current `result: List<TransactionDto>` cannot express row 3: Moshi throws while parsing the
  String, the repository's generic `catch` converts it to a message-less error, and the explorer's
  own explanation is lost. **Decided and done 2026-09-26:** the two list DTOs lose
  `@JsonClass(generateAdapter = true)` and carry `result: List<…>?` + `resultText: String?`, bound by
  a lenient `JsonAdapter.Factory` (`EtherscanEnvelopeAdapterFactory` +
  `LenientListEnvelopeAdapter`) registered in `NetworkModule.provideMoshi()`; `EtherscanBalanceResponse`
  stays on codegen (its `result` is a String in both shapes and it has no caller). The proof is the
  Moshi round-trip test over all four wire shapes (TG4).
  - Tried first and rejected: `@FromJson`/`@ToJson` methods with an extra `JsonAdapter<List<…>>`
    parameter. Moshi 1.x refuses that signature at adapter-registration time
    (`IllegalArgumentException: Unexpected signature for … transactionListToJson`), which failed the
    whole suite until the factory replaced it.
- New `ExplorerApiException(message)` and `ExplorerPlanUnsupportedException(message)` in
  `domain/.../model/common/Exceptions.kt`, beside `ApiKeyNotConfiguredException` — the UI needs
  "explorer rejected the call", "the plan does not cover this chain" and "key not configured"
  (2.0.4 AC-3) to stay three distinct states.
  - Plan-gate detection is **message-driven** (Q6): match the explorer's own wording
    (`message == "NOTOK"` + a `result` text containing `"Free API access is not supported"`), not a
    chain-id constant. Keep the match in one small function with the observed body as its test.
- `refreshTransactionHistory`: evaluate both responses; merge only successes; an error envelope →
  `DataResult.Error(ExplorerApiException(...))`, a plan-gate envelope →
  `DataResult.Error(ExplorerPlanUnsupportedException(...))`. Do not change the key-absent guard's
  position (before the network call).
- UI mapping: `ExplorerPlanUnsupportedException` gets a **persistent inline notice** in TokenDetail
  (the 2.0.4 V1-deviation pattern — a standing condition is not a snackbar). **Decided and done
  2026-09-26:** `TokenDetailUiState.isHistoryPlanGated`, set by `TokenDetailViewModel`, rendered by a
  second notice item next to 2.0.4's, with
  `token_detail_history_plan_gated` = "Transaction history for this chain is not covered by the
  current Etherscan plan." `ExplorerApiException` deliberately gets **no** new banner in this item —
  it reaches the caller as `DataResult.Error` with the explorer's message (AC-4) and richer history
  surfacing stays with 2.8 (recorded in `validation.md`). Home surfaces no history, so no Home change.

### TG4 — Tests (JVM unit tests; no new dependency)

- `core:core-network` has **no tests today**; this item adds its first.
  - `BlockExplorerRequestShapeTest`: build the real `BlockExplorerApi` through Retrofit with an
    `okhttp3.Interceptor` that captures the outgoing `Request` and returns a canned JSON `Response`;
    assert scheme/host/path `/v2/api` and the `chainid`/`apikey`/`module`/`action` query values for
    each of 1, 11155111, 56, 137.
    - No MockWebServer: it is not in `specs/tech-stack.md`/the catalog, and a capturing interceptor
      uses okhttp, which the module already depends on.
  - `ChainConfigProviderTest`: base URL and non-blank key per chain (AC-2); `isExplorerConfigured` false
    for all four when the key is blank, true when set (AC-3); `isRpcConfigured` unchanged for 56/137.
  - `EtherscanEnvelopeParsingTest`: the four wire shapes of TG3 through the real Moshi instance,
    including the **verbatim** plan-gate body observed on 2026-09-26.
- `data`: extend `TransactionRepositoryImplTest` (created by 2.0.4) — error envelope → `DataResult.Error`,
  plan-gate envelope → `DataResult.Error` with `ExplorerPlanUnsupportedException`, `"No transactions found"`
  → `Success`, happy path merges native + token transfers, and the calls now carry the right `chainId`.
- Dependencies: `junit`, `mockk`, `truth`, `kotlinx-coroutines-test` are already declared in both
  modules; `okhttp`/`retrofit`/`moshi` are module `implementation` dependencies, which are visible to
  that module's own unit tests. **No catalog change, no tech-stack update needed.**

## Verification

- **Automatic:** `./gradlew :core:core-network:testDebugUnitTest :data:testDebugUnitTest
  :app:assembleDebug testDebugUnitTest :domain:test --continue`; then `detekt ktlintCheck` compared
  against the post-2.0.4 `main` (counts + per-file "no file worse" check, the 2.0.4 method).
- **Live proof (masked):** read the key from `local.properties` without echoing it, then
  `curl -s -G https://api.etherscan.io/v2/api --data-urlencode "chainid=<n>" --data-urlencode "module=account"
  --data-urlencode "action=txlist" --data-urlencode "address=<known-active>" --data-urlencode "apikey=$KEY"`
  → expect `"status":"1"` for 1 / 11155111 / 137, and the documented **plan-gate** envelope for 56;
  record only `status`/`message`/row count, never the key or the keyed URL.
  The 2026-09-26 baseline probe already returned exactly that (`validation.md` §P3).
- **Device walk (Pixel 6a, owner's real keys):** TokenDetail history shows ≥1 real transaction for a
  known-active address on mainnet (and Polygon, one key); on **BSC** the same screen shows the
  explicit plan notice instead of an empty list; a key-blanked build shows the not-configured state —
  including on BSC/Polygon (new); 0 crashes; screenshots.
  - **Prerequisite (manual half):** addresses with real history on mainnet/Polygon, supplied by the
    owner. The wallet used in 2.0.4 holds 0 wei, where an empty history is a legitimate result and
    therefore proves nothing.
  - **Suspended 2026-09-26 (owner decision):** funding the walk wallet is on hold, so the
    balance-dependent walk rows (mainnet/Polygon history) are suspended until the active account's
    balance is non-zero. The BSC plan notice, the blank-key state and the crash watch need no balance
    and stay walkable. Resume condition and evidence: `validation.md` §Suspended.
- **Not evidence:** a green build, a passing parse test, or a curl run alone. Both halves must pass
  before the roadmap row leaves `[ ]`.

## Risks

| Risk | Mitigation |
| --- | --- |
| **Realised 2026-09-26:** the free Etherscan plan refuses `chainid=56` ("Free API access is not supported for this chain") | Mapped as its own state (AC-8, Q6), message-driven so other plan-gated chains behave the same; a paid plan removes it with no code change. Verified by probe before coding. |
| The plan gate is matched by a brittle string, so the state silently degrades if Etherscan rewords it | Match lives in one function; its test uses the verbatim observed body; a reworded body falls back to `ExplorerApiException` (a visible error, never a silent empty). |
| Free-tier limits (≈5 req/s, 100k/day) make a refresh flaky | A refresh makes 2 calls; re-run once before filing a defect. |
| Tolerant envelope parsing swallows genuine parse errors | TG3's round-trip test covers the four shapes separately, including a malformed body. |
| `chainid` sent for the wrong chain (wrong chain's history) | `chainid` is the same `chainId` argument the repository already uses; the request-shape test asserts all four. |
| A keyless chain survives somewhere | AC-2 test asserts a non-blank explorer key for every supported chain. |
| The BSC/Polygon "Not configured" state contradicts 2.0.4's table and its device walk | Superseding row recorded in `requirements.md`; the change is named in the roadmap item and in 2.0.4's D2 resolution. |
| The migration drifts into 2.8's pagination/UI work | Non-goals list it explicitly; pagination stays 2.8. |

## Exit criteria

AC-1…AC-8 hold with recorded commands and results; `validation.md` written; roadmap row flipped to
`[x]` only after the owner's manual half; the branch merged to `main` and deleted by the owner.
