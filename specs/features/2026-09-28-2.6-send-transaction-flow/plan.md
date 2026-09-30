# 2.6 — Send transaction flow (plan)

> Owner-approved task groups. Decisions: `requirements.md` (Q1–Q5). Everything below stays inside
> the item's branch; gates must be green at every commit boundary.

## Task 0 — milestone-scan baseline (done first, per the roadmap policy)

1. `org.sonarqube` **7.4.0.8496** added to the catalog (`sonar` alias), applied at the root
   `build.gradle.kts` (whole build = one Sonar project), tech-stack row added. (DONE on the branch.)
2. Owner generates a project token in the local server UI (admin credentials are changed; the API
   refused admin/admin). The scan runs as
   `./gradlew sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<from-local.properties>` —
   the token is kept in `local.properties` (git-ignored), never in the build file, never printed.
3. Baseline report recorded in `validation.md` (counts per severity, project key `nexvault`).

## Task 1 — data layer: the seam and the write paths

1. **`core:core-network`**: new `ChainRpcClient` interface + `Web3jChainRpcClient` implementation:
   - `suspend fun estimateGas(from, to, value, data, chainId): BigInteger`
   - `suspend fun getGasPrice(chainId): BigInteger`
   - `suspend fun getTransactionCount(address, chainId): BigInteger`
   - `suspend fun getBalance(address, chainId): BigInteger`
   - `suspend fun callTokenBalance(contract, owner, chainId): BigInteger`
   - `suspend fun sendRawTransaction(signedHex, chainId): String`
   - web3j calls run on `Dispatchers.IO`; errors surface as `IOException`/`RuntimeException` for the
     repository to map. Registered in the core-network DI module.
2. **`data`**: `TransactionRepositoryImpl` implements `estimateGas` (FR1), `sendNativeTransaction`,
   `sendTokenTransaction` (FR2, both wallet types), the FR4 validations, and the pending-row
   upsert. Key material comes from `WalletStore` / `MnemonicManager` + `HDKeyManager`
   (session-gated via `WalletRepository.getMnemonicForBackup`), wiped after signing with
   `secureWipe()`.
3. Tests: `TransactionRepositoryImplTest` grows TC-REPO-003 (mock seam: gasPrice/limit/nonce/send →
   success hash + DAO upsert), TC-REPO-004 (balance check), estimate-gas tiers, token encoding,
   key-absent guard. MockK over the seam (plain interface — no final-class problem).

## Task 2 — domain use cases

`domain/usecase/transaction/`: `EstimateSendGasUseCase`, `SendTransactionUseCase` (thin delegation
to the repository, session + params passthrough), each with unit tests. Address validation reuses
the existing `isValidAddress` helper used by `AddCustomTokenUseCase`.

## Task 3 — feature-send UI

1. `core-ui`: generic `ConfirmationDialog` (title, content slot, confirm/dismiss; TC-UI-010) with
   its instrumented test in `app/src/androidTest` (the 2.0.6 recipe).
2. `feature:feature-send`: `SendViewModel` + `SendUiState` (form fields, token list, gas options,
   validation errors, review, submitting, result, key-absent), `SendScreen` (form/review/result
   states; `PinInputField` inside the confirmation dialog), Hilt wiring, strings.
3. VM tests: TC-VM-003/004/005 + key-absent + review/submit happy path.

## Task 4 — navigation re-enable

1. App: new `SEND` route (+ optional `tokenAddress`/`chainId` args); `feature-send` gets its own
   navigation graph; registered in `MainNavigation`.
2. Restore `HomeScreen.onSendClicked` and `TokenDetailScreen.onSendClicked` parameters
   (2.0.3's removed wiring); `MainScreen` passes real navigation callbacks. Receive/Swap untouched.

## Task 5 — verification

1. `detekt ktlintCheck` + `testDebugUnitTest :domain:test --continue` green with attributed totals;
   `:app:assembleDebug` APK path recorded.
2. Device walk on Sepolia (faucet-funded; owner or agent per Q3): create/fund wallet → send a small
   amount → PIN → hash + explorer pending → balance decrease. TC-INT-002 satisfied by the walk;
   `traceability.md` rows for the TCs this item owns get updated at the close.
3. `validation.md` + reviewer handoff; owner-directed close.
