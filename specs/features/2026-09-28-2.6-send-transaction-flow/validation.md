# 2.6 — Send transaction flow (validation record)

> Status: **in progress on `feature/2.6-send-transaction-flow`** (owner decisions: `requirements.md`
> Q1–Q5, 2026-09-28). Evidence fills in per task group; nothing below is claimed done until its
> command and observed result are recorded.

## Task 0 — Milestone static-analysis baseline (DONE 2026-09-28)

Setup, all on this branch: `org.sonarqube` **7.4.0.8496** in the catalog (`sonar` alias), applied at
the root `build.gradle.kts` (root-level by design — the whole build is one Sonar project), tech-stack
§6 row added, `sonar` task verified present. Host URL and token pass on the command line; the token
lives in `local.properties` (git-ignored), never printed.

Token journey, recorded because the discipline applies to tooling too: the first attempt hit the
server's changed admin credentials (401); a resume-era token authenticated but its account had zero
project visibility and no create rights ("not authorized to analyze ... not authorized to create");
the owner created the `NexVault` project and, after one revoked over-powered global token, minted a
**Project Analysis Token** scoped to `NexVault` — the least-privilege choice and the standing recipe
(project-scoped, not global; AGENTS/dev-environment updated accordingly below).

Baseline scan (the pre-2.6 tree — only the task-0 build changes and this spec were present):

```
./gradlew sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<local.properties> -Dsonar.projectKey=NexVault
-> BUILD SUCCESSFUL, exit 0 (2026-09-28)
```

Baseline measures, read back from `/api/measures/component?component=NexVault`:

| Metric | Baseline |
| --- | --- |
| ncloc | **12,121** |
| bugs | 3 |
| vulnerabilities | 5 |
| code smells | 70 |
| security hotspots | 0 |
| duplicated lines density | 0.3 |
| coverage | 0.0 (JaCoCo not wired — coverage joins the gate at 4.8) |

The gate at **2.10** is new-code-only: no new blocker/critical, no new security hotspots, duplication
not increasing, measured against these numbers.

### Task 0b — scan fix round (2026-09-28, owner-directed: "fix all security and reliability issues")

Second scan ran against the **committed** 2.6 tree (branch commits `d5d15e0`, `3af1f50`,
`7ce1421`; `./gradlew sonar … -Dsonar.projectKey=NexVault`, BUILD SUCCESSFUL). It reported
**8 open bugs/vulnerabilities — all in pre-2.6 code, none in the new 2.6 files**:

| # | Finding | Disposition |
| --- | --- | --- |
| 1 | `xml:S5332` implicit cleartext traffic | **Fixed** — `android:usesCleartextTraffic="false"` on `<application>` |
| 2 | `kotlin:S899` ignored `File.delete()` results in `WalletStore` (x2) | **Fixed** — wipes now fail loudly if any wallet file survives deletion |
| 3 | `kotlin:S3923` single-branch `nativeDecimalsForChain` | **Fixed** — replaced with the `NATIVE_DECIMALS` constant |
| 4 | `kotlin:S6474` no dependency-verification metadata | **Fixed** — `gradle/verification-metadata.xml` generated (`--write-verification-metadata sha256` over `:app:assembleDebug testDebugUnitTest :domain:test`); every build now verifies checksums. New/changed dependencies require regenerating it (tech-stack §6 note) |
| 5 | `kotlin:S6293` biometric prompt without `CryptoObject` | **Fixed** — `BiometricHelper.getBiometricGateCipher()` (AndroidKeyStore AES gate key, `setUserAuthenticationRequired(true)`, regenerated once if invalidated) and the unlock prompt now authenticates crypto-bound. **Device verification of the biometric path is owed** — the owner's phone was locked; the walk tomorrow covers it |
| 6 | `kotlin:S6288` non-user-auth master key (`KeyStoreManager` lines 46/116) | **Accepted as owner-decision debt** — see below |

Post-fix scan (BUILD SUCCESSFUL): **bugs 3 → 0, vulnerabilities 5 → 2** (the two accepted S6288),
security hotspots 0 → 0, duplication 0.3 → 0.3, ncloc 12,121 → 13,394.

**Accepted with rationale (needs the owner's ratification):** `kotlin:S6288` flags the
wallet-encryption master key (`setUserAuthenticationRequired(false)`, line 46) and the StrongBox
probe key (line 116). That flag is the **owner-approved 2.0.2b architecture**: the mnemonic is
protected by the KeyStore-wrapped AES-GCM layer plus the app-managed session gate (in-app PIN via
`VerifyPinUseCase`; biometric now crypto-bound via the fix above). Binding the master key to system
user-auth would force a system biometric/device-credential prompt on every encrypt/decrypt and
break the in-app PIN flow — a redesign, not a flag flip. It is the subject of the **4.7**
hardening item (TC-SECTEST-002/003). Server-side issue keys (accept in the UI if desired):
`3a18f518-87bc-43da-921e-3104c7ec1ce7`, `47b4a362-0949-4b32-8b1a-4d8c98d59a12` — the API accept
transition needs issue-admin rights the project analysis token does not have (403, expected).

## Task 1 — data layer (implemented, automatic half green)

- `ChainRpcClient` + `Web3jChainRpcClient` (core-network) — the mockable seam over web3j's final
  classes; bound in `NetworkModule`.
- `TransactionRepositoryImpl`: `estimateSendGas` (slow/normal/fast tiers, ERC-20 calldata encoded
  in-repo, fiat cost from the stored native price, gas limit carried in `GasEstimate`) and both send
  paths (session-gated key retrieval, EIP-155 signing for HD + imported wallets, balance validation
  incl. the native fee for token sends, pending row persisted, key bytes wiped after signing).
- Tests: `TransactionRepositoryImplTest` **13/13** (incl. TC-REPO-003 success + pending row,
  TC-REPO-004 insufficient balance), `SendUseCasesTest` 3/3.

## Task 2 — domain use cases (implemented)

`EstimateSendGasUseCase` + `SendTransactionUseCase` (native/token dispatch), thin seams between the
feature layer and the repository; 3/3 tests.

## Task 3 — feature-send UI (implemented, automatic half green)

`SendViewModel`/`SendUiState` (validation, debounced estimate, MAX = balance − fee, key-absent
state, PIN-confirmed submit) — `SendViewModelTest` **6/6** (TC-VM-003/004/005 + submit + wrong PIN);
`SendScreen` (form → review → `ConfirmationDialog`-hosted PIN → result with "View on Explorer");
core-ui `ConfirmationDialog` (TC-UI-010) — instrumented `ConfirmationDialogTest` (3 cases) written
and compiled (`:app:compileDebugAndroidTestKotlin` green); it runs tomorrow with the device.

## Task 4 — navigation re-enable (implemented, compiled)

`send?tokenAddress=` route in the main graph; Home and TokenDetail Send buttons re-enabled with the
2.0.3 wiring restored; Receive/Swap captions updated.

## Task 5 — verification (pending the device)

Full unit suite after the scan fixes: **314 tests, 0 failures, 0 errors, 1 skipped**; `detekt` and
`ktlintCheck` exit 0; `:app:assembleDebug` green; both Sonar scans recorded above. Still owed:
the instrumented run (TC-UI-010 + regression), the **Sepolia faucet walk** (TC-INT-002, incl. the
new crypto-bound biometric path on the unlock screen), then reviewer handoff and owner close.

## Handoff (what 2.6 deliberately leaves behind)

1. **S6288 master-key user-auth hardening** — accepted-with-rationale above; the redesign belongs to
   **4.7** (TC-SECTEST-002/003). Owner ratification of the acceptance recorded here.
2. **Biometric-path device verification** — implemented crypto-bound in 2.6; the phone was locked,
   so the biometric unlock walk is owed as part of tomorrow's 2.6 manual half.
3. QR scan, address book, custom-address saving — replanned to later items in `requirements.md` Q1.
