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
**The owner accepted both in the UI (2026-09-29): the next scan reports vulnerabilities 0.**

### Task 0c — coverage wiring (2026-09-29, owner-directed: "scan for coverage")

Coverage was 0.0 because JaCoCo was never wired. It is now measured:

- Root `build.gradle.kts`: applies the **bundled Gradle `jacoco` plugin** to every Android library
  and JVM module (no new dependency — the agent comes from AGP 9.4.1's `jacocoVersion 0.8.13`),
  configures one `jacocoTestReport` (XML-only) per module over `testDebugUnitTest` / `test`
  (`data/build/jacoco/testDebugUnitTest.exec` etc.), excludes generated code (R/BuildConfig/DI
  factories/tests), and feeds `sonar.coverage.jacoco.xmlReportPaths` via a `sonar.*` project
  property; the `sonar` task depends on all reports.
- Dependency verification caught and then recorded the jacoco artifacts (asm 9.8/9.9,
  agents 0.8.13/0.8.14) — expected friction of the S6474 fix.
- Modules **with** unit tests (11): database, datastore, network, security, data, auth, home,
  onboarding, send, tokens, domain — aggregate **2344/8299 lines = 28%** locally.
- Modules **without** unit tests (app, core-common, core-ui, feature-receive/settings/swap)
  contribute 0% — so the **server-wide coverage measure is 9.6%**, which is the honest number the
  4.8 gate will set its bar from (alongside a per-module floor for the tested 11).

Post-wiring scan (BUILD SUCCESSFUL): coverage **0.0 → 9.6**, bugs 0, vulnerabilities **0** (the
two S6288s accepted server-side), hotspots 0, code smells 83, duplication 0.3 — all recorded for
the 2.10 gate comparison.

### Task 0d — coverage campaign (2026-09-29, owner-directed: "at least 50%")

- **Fixed first:** all 8 CRITICAL `kotlin:S3776` cognitive-complexity findings (incl. the cited
  `TransactionRepositoryImpl.sendTransaction` 23→~5) plus the 3 `kotlin:S1192` literal duplications
  the split surfaced — commit `e60510f`; the re-scan reports **zero critical/blocker issues**.
- **Test-writing campaign:** suite grew **299 → 410 tests** (0 failures): core-ui component tests
  (Robolectric Compose, 11 cases) + formatting utils, database entity/DI tests, data mappers,
  token/transaction repository paths, network adapters/envelope/interceptors/factories, 15 thin
  domain use cases, Home (11) / TokenDetail (12) / Send (14) ViewModel paths.
- **Measurement plumbing fixed:** the scanner only auto-integrates JaCoCo for JVM projects, so all
  eleven Android modules' reports were silently ignored (server showed one module: 12.0%). Each
  Android module now applies the Sonar plugin and declares its own report path — commit `1de3e93`;
  the same scan jumped to **26.5%**, then **27.4%** after the remaining use-case tests.
- **Known limitation, recorded honestly:** Robolectric's sandbox classloader defines app classes
  itself, bypassing the JaCoCo agent. Three standard remedies were tried and exhausted under
  Gradle 9.6 UTP + AGP 9.4: (1) `@Config(instrumentedPackages=…)` — the sandbox does not route the
  agent transformer; (2) offline instrumentation of the runtime-library dir — the sandbox does not
  read it; (3) offline instrumentation of the ASM-transformed dir — same, and JaCoCo 0.8.13+'s
  analyzer refuses instrumented class files by design. Consequence: the ~4,300 executable lines of
  core-ui, core-database and the five feature screens stay 0-covered. The plain-JVM tests in those
  modules DO count. Revisit at **4.8** with the device-based instrumented coverage (available from
  2026-09-30) or a Kover migration (has known AGP-9 report-task breakage, kotlinx-kover #785).

### Task 0e — Kover migration: Robolectric coverage unlocked (2026-09-30, owner-approved)

JaCoCo could not see Robolectric-sandboxed classes; after the owner approved the migration, the
measurement now runs on **Kover 0.9.11** (`org.jetbrains.kotlinx.kover`, applied per reporting
module from the root build script, Kover XML at `build/reports/kover/report.xml`, same
`sonar.coverage.jacoco.xmlReportPaths` property, generated-code/test filters replacing the old
excludes, Kover artifacts recorded in `gradle/verification-metadata.xml`).

- **Go/no-go gate passed:** `:core:core-ui:koverXmlReport` reports **774/1297 lines covered (60%)**
  for the Compose components — JaCoCo measured **20/1334 (1.5%)** for the same tests.
- Robolectric enabled in the five feature modules, with screen tests added: `OnboardingScreensTest`
  (Welcome/Create/Verify), `UnlockScreenCoverageTest`, `HomeScreenCoverageTest`,
  `TokenDetailScreenCoverageTest`, `SendScreenCoverageTest`.
- **Suite: 410 → 417 tests, 0 failures.** Gates green. `:app:assembleDebug` green.
- Local aggregate: 2,575/9,744 (26%) under JaCoCo → **4,547/9,187 (49%)** under Kover.
- **Server measure: 27.4% → 53.4% (4,059/7,035).** One trap worth recording: stale JaCoCo XMLs left
  on disk kept being imported (the first post-migration scan read 28.0% with the old
  `lines_to_cover=7727`); deleting `build/reports/jacoco` and re-scanning imported the Kover XMLs —
  19 modules importing, coverage 53.4%.

Known follow-ups from the post-migration scan (2.6 code):
- `xml:S5332` / `xml:S6358` in `app/src/debug/AndroidManifest.xml` — **fixed** (the debug manifest
  now repeats the main manifest's `allowBackup`/`dataExtractionRules`/`usesCleartextTraffic`).
- `kotlin:S6532` in `WalletStore.wipeAllFiles` — **fixed** (`check(...)` idiom).
- `kotlin:S6310` ("avoid hardcoded dispatchers", across `WalletStore` and the new
  `Web3jChainRpcClient`) — **accepted debt**: `Dispatchers.IO` at the repository boundary is this
  codebase's established pattern; injecting dispatcher qualifiers is a repo-wide refactor, not a
  2.6 change. Owned by **4.8**.
- `kotlin:S6288` x2 (master key user-auth binding) — previously accepted in the UI; the fresh
  analysis re-created the issue instances (fingerprint change), so they are OPEN again and need the
  owner's acceptance once more (the analysis token cannot transition issues, HTTP 403).

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

## Task 5 — verification (device half in progress)

Full unit suite after the scan fixes: **410 tests, 0 failures, 0 errors, 1 skipped**; `detekt` and
`ktlintCheck` exit 0; `:app:assembleDebug` green; Sonar scans recorded above (27.4% coverage,
zero criticals/blocks, bugs 0, vulnerabilities 0).

**Instrumented suite, device half DONE (2026-09-30, Pixel 6a over mDNS):**
`ANDROID_SERIAL=adb-26111jEGR13989-MJzh3R._adb-tls-connect._tcp :app:connectedDebugAndroidTest`
→ **BUILD SUCCESSFUL, 8/8**: BackupPolicyTest 3/3, **ConfirmationDialogTest 3/3 (TC-UI-010)**,
**CreationFlowE2ETest 1/1 (TC-INT-001)**, ExampleInstrumentedTest 1/1. The dialog test needed a
debug-only `DialogTestHostActivity` (MainActivity already sets content; the rule's default empty
activity resolves to the wrong process under this manifest).

**Device-side coverage, attempted and blocked (recorded for 4.8):** AGP 9.4 has no
`createDebugAndroidTestCoverageReport` task; `DeviceTest.codeCoverageEnabled` is read-only at
configuration time; the runner's `coverage=true` argument breaks every Compose test ("No compose
hierologies found"). The exec-collection path needs either a future AGP fix or a Kover migration.

Still owed for the 2.6 close: the **Sepolia faucet walk** (TC-INT-002, incl. the new crypto-bound
biometric path on the unlock screen), then reviewer handoff and owner close.

## Handoff (what 2.6 deliberately leaves behind)

1. **S6288 master-key user-auth hardening** — accepted-with-rationale above; the redesign belongs to
   **4.7** (TC-SECTEST-002/003). Owner ratification of the acceptance recorded here.
2. **Biometric-path device verification** — implemented crypto-bound in 2.6; the phone was locked,
   so the biometric unlock walk is owed as part of tomorrow's 2.6 manual half.
3. QR scan, address book, custom-address saving — replanned to later items in `requirements.md` Q1.
