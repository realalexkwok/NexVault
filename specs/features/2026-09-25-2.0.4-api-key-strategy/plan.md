# 2.0.4 — API-key strategy — Technical plan

> Approved 2026-09-25 (plan mode). Requirements: `requirements.md` (Q1–Q3, AC-1–AC-6).
> Evidence: `validation.md`. Branch: `feature/2.0.4-api-key-strategy` — implemented and committed
> there (owner-directed, for review); merging to `main` is an owner action.

## Task groups

### TG1 — build wiring (`app/build.gradle.kts`)
- Load `rootProject.file("local.properties")` into a `java.util.Properties` at the top of the file
  (absent file → empty); helper `apiKey(name)` resolves `local → project.findProperty → ""`.
- Replace the five `buildConfigField` expressions (`:38-62`) with the helper.
- Secrets: no value is ever printed, logged or committed; `local.properties` stays git-ignored.

### TG2 — availability signal
- `core:core-network` `ChainConfigProvider`:
  - `fun isRpcConfigured(chainId: Int): Boolean` — true when the chain's RPC needs no key
    (56/137 public endpoints) or the Infura key is non-blank (1/11155111);
  - `fun isExplorerConfigured(chainId: Int): Boolean` — true when the chain's explorer key is
    non-blank.
- `domain/model/common/Exceptions.kt`: new `ApiKeyNotConfiguredException(message)`.

### TG3 — enforcement
- `data/TokenRepositoryImpl.kt:70` (native balance) and `:180` (token balances): when
  `!chainConfigProvider.isRpcConfigured(chainId)` → `DataResult.Error(ApiKeyNotConfiguredException(...))`
  **before** `web3jProvider.getWeb3j(...)`.
- `data/TransactionRepositoryImpl.kt:86` (history refresh): `!isExplorerConfigured(chainId)` → same
  error before `blockExplorerApiFactory.getApi(...)`.
- `feature-home/HomeViewModel.kt:124-136`: map `ApiKeyNotConfiguredException` to a new string
  (`home_error_not_configured`) instead of the generic resources; IOException/other unchanged.
- `feature-tokens`: same mapping where the history refresh result is surfaced, with
  `token_detail_history_not_configured`.

### TG4 — tests (`data`)
- `TokenRepositoryImplTest`: unconfigured chain → `DataResult.Error` with
  `ApiKeyNotConfiguredException`, `coVerify(exactly = 0) { web3jProvider.getWeb3j(any()) }`;
  configured chain → unchanged behavior (existing tests keep passing).
- `TransactionRepositoryImplTest`: same shape for the explorer path.
- No UI tests (TC-UI = 2.0.6).

## Verification
- **Automatic:** loader proof (script asserts the five BuildConfig fields are non-empty, values
  redacted); `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --continue`; gate delta
  vs `main` (detekt 65, ktlint 1454) with a stash-compare for the touched files.
- **Device — real keys (Pixel 6a):** Home renders live CoinGecko prices/chart; balance and history
  refreshes complete without error; TokenDetail chart renders; no "Not configured" anywhere; 0
  crashes; screenshots (no FLAG_SECURE on these screens).
- **Device — key-absent (honesty check):** blank `INFURA_API_KEY` (file backup → edit → restore),
  rebuild + install, confirm the explicit message, then restore and rebuild.

## Risks

| Risk | Mitigation |
| --- | --- |
| Build script change silently keeps keys empty | The loader proof asserts non-empty BuildConfig for all five fields. |
| Keys leak into logs or commits | Values are never printed; `local.properties` is git-ignored; the spec references key *names* only. |
| New state regresses the keyed happy path | Configured-path unit tests stay green and the device walk uses the real keys. |
| `findProperty` fallback masks a broken loader | The proof checks the local-file path specifically (temporarily unset is not needed — the helper prefers the file). |
