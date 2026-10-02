# NexVault — Roadmap

> **Order is strict.** Execute items top to bottom; nothing gets skipped, reordered, or
> started early without the owner's explicit approval. One roadmap item = one feature
> spec directory = one branch.
>
> Re-measured **2026-09-20**. Every status line below carries evidence; none of it is
> inherited from the `doc/prompts/` archive, which recorded *intent* and is now
> read-only history.

## Status legend

| Mark | Meaning |
| --- | --- |
| `[x]` | **Verified** — a `validation.md` exists, the automatic half passed with recorded commands, and the owner's manual half passed. |
| `[~]` | **Implemented, unverified** — code exists and compiles, but at least one half of verification is missing or failing. |
| `[ ]` | **Not started.** |
| `[!]` | **Blocked** — cannot proceed; the blocker is named inline. |

A `[~]` item is *not* done. It may not be cited as working, may not be built upon
without a compensating note in the new item's `requirements.md`, and cannot become `[x]`
without evidence.

## Branches

- `main` is the trunk. There is **no `develop` branch**, despite `doc/01`'s branching
  strategy section — that document describes an intention that was never implemented.
- One branch per item: `feature/<item>-<slug>` (e.g. `feature/2.0.1-core-security-tests`).
- Never stack a new branch on an unfinished feature branch.
- Agents do not `git commit` or `git push`; changes stay in the working tree for the
  owner. Merging into `main` is an owner action.

## Milestone static-analysis scan (owner decision 2026-09-28)

Every remaining phase ends with a **scan-and-close item**: it runs a static-analysis scan over
everything the phase built, reconciles the findings, and only then writes the phase's
`validation.md` and closes it. The checkpoints are **2.10** (Phase 2), **3.8** (Phase 3) and
**4.19** (repo close-out) — the same "record and close" shape as 2.0.7, so the strict order makes
a phase unable to end unscanned.

Policy (owner decisions 2026-09-28):

- **Server:** the owner's local SonarQube Community Build, verified running on 2026-09-28 as
  **26.9.0.129388** at `http://localhost:9000` (started with `sudo docker start sonarqube`).
  Code never leaves the machine — no SonarCloud.
- **Baseline first:** one baseline scan is recorded **before 2.6 starts**, so each checkpoint gates
  on **new code only** — no new blocker/critical issues, no new security hotspots, duplication not
  increasing. Coverage is *not* part of the gate until 4.8 sets the bar; it is **measured** (2.6
  task 0c/0d): the JaCoCo wiring reached 27.4%, then the owner-approved **Kover migration
  (2026-09-30)** made Robolectric-sandboxed classes measurable and took the server measure to
  **53.4%** (4,059/7,035) — 4.8 sets its bar from that.
- **Scanner:** **resolved 2026-09-28 by 2.6's task 0 — SonarScanner for Gradle** (`org.sonarqube`
  **7.4.0.8496**, root-applied; host URL and token on the command line, never in the build file).
  The installed npm `@sonar/scan` is JS/TS-only and is not used. The server token is a **project
  analysis token scoped to `NexVault`** (never a global one). See `dev-environment.md` §6b.
- **Findings are triaged, never silently ignored:** each checkpoint's `validation.md` records the
  scan command, the counts, what was fixed, and what was accepted as debt with a named owner.
- The scan is **not** a replacement for the per-commit gates: `detekt` and `ktlintCheck` stay the
  standing quality gates (AGENTS.md); the scan adds the milestone-wide view (duplication, complexity
  trends, security hotspots).

## Current position

> **Phase 1.9 (Legacy code review) is CLOSED 2026-09-25 — all 15 pre-SDD features reviewed and
> signed off.** Spec: `specs/features/2026-09-21-legacy-code-review/` (validation.md records the
> gate closure and every finding handed to its owning item).
>
> **2.0.5 (quality gates) CLOSED 2026-09-27 as `[x]` — VERIFIED, merged to `main` (`--no-ff`) and
> pushed; the local feature branch is deleted.** Both halves passed. **Automatic:** both gates green
> for the first time — `./gradlew detekt ktlintCheck --continue` → BUILD SUCCESSFUL (exit 0),
> **detekt 65 → 0** and **ktlint 1556 → 0** — with the build and the 274-test suite unchanged, and
> the reviewer reproducing it from a fully fresh `--rerun-tasks` run (`729d663`, 633 tasks, exit 0,
> zero findings). **Manual:** the owner walked the app on the Pixel 6a on 2026-09-27 and reported it
> satisfied, which the device corroborates — Home rendered with live prices and a drawn chart on
> this build, 0 crashes across the whole session.
> **2.0.6 (TC traceability) CLOSED 2026-09-27 as `[x]`** — reviewer PASS (automatic + diff +
> device, V1–V8) confirmed by the owner, merged to `main` (`--no-ff`), pushed, the local feature
> branch deleted. All 52 `doc/07` ids now traced — 25 mapped to test methods carrying
> `// TC-XXX-NNN` markers (audit-proven, zero orphans), 27 registered to named owners; suite
> **274 → 299 tests / 0 failures**; both gates green; instrumented 5/5 on the Pixel 6a (the
> TC-INT-001 creation-flow E2E included); one production defect found and fixed (the 2.0.4/2.0.4b
> token-detail notices were dead states). Evidence:
> `specs/features/2026-09-27-2.0.6-tc-traceability/validation.md`.
> **2.0.7 (record and close) CLOSED 2026-09-28 — PHASE 2.0 IS COMPLETE (`[x]`).** The closing
> record — fresh evidence (suite **299 / 0 / 0 / 1** from a 571-task `--rerun-tasks` run, both gates
> exit 0, instrumented **5/5** on the Pixel, TC audit 52 = 25 ∪ 27), the Phase 0 flip table
> (15 rows → `[x]`; 1.2 and 1.4 stay `[~]` for open 4.17/4.18 findings), the 2.0.4b M3/M4 device
> checks and the consolidated Handoff — is
> `specs/features/2026-09-20-2.0.0-stabilization/validation.md` §"2.0.7 — Phase 2.0 close".
> 4.15 closed as refuted. **The next open entry point is 2.6** (Send transaction flow);
> Phase 2.6+ is unblocked. The only remaining verification debt is 2.0.4b's (M1/M2 suspended pending
> funding, M5 crash watch owed — M3/M4 closed by the 2.0.7 close).
>
> **Milestone scans added 2026-09-28 (owner decision):** each remaining phase now ends with a
> scan-and-close item — **2.10**, **3.8**, **4.19** — running the local SonarQube over what the phase
> built, gating on new code only, with a baseline recorded before 2.6 starts. See "Milestone
> static-analysis scan" below.

> **Blocked at 2.0.2b (2026-09-21).** The first run on a real device found that
> onboarding creates the wallet without ever showing the mnemonic, leaving the app
> unusable and the wallet unbacked-up. Nothing else in Phase 2.0 can be verified until
> this is fixed, because no flow can get past the first screen.
>
> **2.0.2b fix implemented, device-walked and CLOSED 2026-09-25.** Build green,
> **249 tests / 0 failures / 1 skipped**, both gate counts lower than at HEAD, and the full
> onboarding flow observed on the Pixel 6a (create, verify, Set PIN, cold-start unlock, mnemonic
> and private-key import, abandoned-attempt recovery). The walk also found and fixed two defects
> the code alone did not show. Evidence:
> `specs/features/2026-09-25-2.0.2b-onboarding-repair/validation.md`; merged to `main` on the
> owner's direction.
>
> **2.0.4 (API-key strategy) CLOSED 2026-09-26 — reviewer verification PASS, owner-directed close;
> merged to `main` and the feature branch deleted.** Its one open finding, **D2 — the Etherscan V1
> endpoint is deprecated**, was decided by the owner the same day: the migration became its own item,
> **2.0.4b** (below), which **closed 2026-09-26 as `[~]`**: merged to `main`, reviewer-verified on the
> automatic half, its device-walk evidence still owed (M1/M2 suspended pending funding, M3–M5
> owner-pending) — the rest of Phase 2.0 (2.0.5 → 2.0.6 → 2.0.7) is proceeded singly or as
> owner-approved grouped items (the G1–G5 grouping precedent). Phase 2.6+ still waits on Phase 2.0.
>
> Note for later items: the build now loads `local.properties` into `BuildConfig`, and
> `core:core-network` runs the Moshi codegen (`ksp`) — both were broken/absent before 2.0.4.
>
> What the first run has already bought, beyond the two defects it found: the app is
> confirmed to build, install, launch, and render on a Pixel 6a, and one prediction
> (a stray ActionBar) was refuted rather than acted on — which is the whole argument for
> running the thing instead of reasoning about it.

---

## Funding-suspended verification (wake-up register)

**Owner's rule (2026-09-30):** test cases that need a funded wallet stay **suspended** — never
failed, never silently dropped — until the owner signals funding with any wording (e.g.
"funded", "funds ready"). On that signal, resume every funded row in one pass, in order.

**The single authoritative register of pending manual/device test cases — the funded rows and
every other manual case such as the second-phone QR scan — is
[`specs/pending-manual-test-cases.md`](pending-manual-test-cases.md) (adopted 2026-10-02).**
This section intentionally carries no duplicate row table; read that file first whenever funding
or a manual walk is due. The 2.10 Phase 2 walk re-reads it and executes whichever rows the owner
funded or released by then.



> **2.8 (Transaction history) CLOSED 2026-10-02 as `[x]` — owner close after the reviewer's PASS and
> the re-verification of the fixes (`039bfc8`, re-checked in `29f1d41`), merged to `main` (`--no-ff`),
> pushed, the local feature branch deleted.** Delivered: the history tab (sticky date groups, the AC's
> five chips, pull-to-refresh, fetch-on-scroll capped at 50 explorer pages, empty/notice states) and the
> transaction detail screen (full fields, "Check status now", chain-correct "View on Explorer"), plus
> the two data-layer shortcuts the roadmap flagged — `getTransactionHistory` now honours
> `page`/`pageSize`, `refreshTransactionHistory` pages the explorer, and `updateTransactionStatus` asks
> the chain through the new V2 `gettxreceiptstatus` endpoint (a blank inner status stays PENDING; the
> reviewer's MAJOR finding on that mapping was fixed). Evidence: 473 tests / 0 failures / 1 skipped
> (reviewer re-run), both gates green, SonarQube **coverage 56.4%** with **0 bugs, 0 vulnerabilities,
> 0 criticals** and no open issue in any 2.8 file. **2.10 is CLOSED `[x]` (2026-10-02, owner close
> after the reviewer's PASS, no findings) — PHASE 2 IS COMPLETE.** See the 2.10 row below and its
> `validation.md`. **The next open entry point is 3.1** (WalletConnect v2); the only verification
> debt left for later phases is 2.0.4b's M1/M2/P4 (suspended pending funding) in
> `specs/pending-manual-test-cases.md`.

---

## Phase 0 — Historical: the prompt-driven era (retired)

Tasks 1.1–2.5 were implemented by feeding `doc/prompts/01…15` to an agent, one file per
task, with no feature spec, no verification step and **no code review**. The code they
produced is real and mostly sound. The legacy CR gate (Phase 1.9, below) reviewed them one
by one; each row links to its CR task file.

**Re-graded 2026-09-27 by 2.0.7.** Every row sat `[~]` on two grounds — *never verified on a
device* and *both quality gates red*. Both grounds are gone: the app has been walked since
2026-09-25 (2.0.2b → 2.0.6, with instrumented tests since 2.0.6), and both gates are green since
2.0.5. Rows whose CR findings are all closed now read `[x]`; **1.2 and 1.4 stay `[~]`** because
findings 1.2-6, 1.2-7 and 1.4-4 (all string/token adoption, owned by 4.17) and 1.2-5's composable
residue (4.18) are still open. Per-row basis: `features/2026-09-20-2.0.0-stabilization/validation.md`
§"2.0.7 — Phase 2.0 close" (flip table).

Evidence of the era's failure mode: `doc/04-IMPLEMENTATION-PLAN-PHASE1.md` ends with
"All Phase 1 unit tests pass" and "Detekt and ktlint pass with zero issues" — both
statements are false today, and there is no record they were ever true.

| Item | Deliverable | Evidence in tree | CR task | Status |
| --- | --- | --- | --- | --- |
| 1.1 Project scaffolding | Multi-module Gradle build, version catalog | `settings.gradle.kts` (20 modules), `gradle/libs.versions.toml`; finding 1.1-3 closed 2026-09-27 (instrumented 5/5 on the device) | [1.1](features/2026-09-21-legacy-code-review/tasks/1.1-project-scaffolding.md) | `[x]` |
| 1.2 Design system & theme | `core:core-ui` | 29 source files: `theme/` 6, `components/` 16, `animation/` 3, `util/` 2, `mapper/` + `preview/` 2 — findings 1.2-6/1.2-7 open (4.17), 1.2-5 → 4.18 | [1.2](features/2026-09-21-legacy-code-review/tasks/1.2-design-system-theme.md) | `[~]` — open findings |
| 1.3 Security module | `core:core-security` | 10 source + 5 test files (KeyStore, Tink, BIP-39/44, biometric, PIN validation) — suite repaired by 2.0.1: **64 tests, 0 fail, 1 skipped**; flows walked | [1.3](features/2026-09-21-legacy-code-review/tasks/1.3-security-module.md) | `[x]` |
| 1.4 DataStore & preferences | `core:core-datastore` | 8 source + 4 test files; 29 tests pass — finding 1.4-4 open (4.17) | [1.4](features/2026-09-21-legacy-code-review/tasks/1.4-datastore-preferences.md) | `[~]` — open finding |
| 1.5 Domain models & repository interfaces | `domain` | 32 source files (7 models, 5 repository interfaces, 20 use cases) + 12 test files; 71 tests pass | [1.5](features/2026-09-21-legacy-code-review/tasks/1.5-domain-models-repositories.md) | `[x]` |
| 1.6 Data layer | `data` | 11 source files: 5 repository impls + 5 mappers + `RepositoryModule`; 53 tests pass; 1.6-1 closed by 2.0.2b | [1.6](features/2026-09-21-legacy-code-review/tasks/1.6-data-layer.md) | `[x]` |
| 1.7 Onboarding (parts 1–2) | `feature:feature-onboarding` | 12 source + 5 test files; 35 tests pass; device walk M1–M11 (2.0.2b) + the creation E2E | [1.7](features/2026-09-21-legacy-code-review/tasks/1.7-onboarding.md) | `[x]` |
| 1.8 Auth / unlock | `feature:feature-auth` | 3 source + 1 test file; 15 tests pass; cold-start unlock and lockout walked | [1.8](features/2026-09-21-legacy-code-review/tasks/1.8-auth-unlock.md) | `[x]` |
| 1.9 Main scaffold & navigation shell | `app` | `MainScreen` with 5 tabs — 4 render `PlaceholderTabScreen` (Phase 2.6–2.8 scope); shell re-walked in 2.0.3 | [1.9](features/2026-09-21-legacy-code-review/tasks/1.9-main-scaffold-navigation.md) | `[x]` |
| 2.1 Network module | `core:core-network` | 19 files: CoinGecko + explorer APIs, Web3j provider, interceptors, adapters — **20 tests** (2.0.4/2.0.4b explorer + 2.0.6 adapters); live data walked | [2.1](features/2026-09-21-legacy-code-review/tasks/2.1-network-module.md) | `[x]` |
| 2.2 Database module | `core:core-database` | 10 files: 4 entities, 4 DAOs, DB, DI; schema `1.json` — **3 Robolectric DAO tests** (2.0.6) | [2.2](features/2026-09-21-legacy-code-review/tasks/2.2-database-module.md) | `[x]` |
| 2.3 Chain management | Chain switching | 3 chain use cases, `ChainUiMapper`, `ChainSelectorDropdown`, `ChainBadge`, `ChainIconMapper` — chain tests exist in `domain`/`data`; 2.3-1 fixed + verified; chain switch walked | [2.3](features/2026-09-21-legacy-code-review/tasks/2.3-chain-management.md) | `[x]` |
| 2.4 Home dashboard | `feature:feature-home` | 4 files: `HomeScreen`, `HomeViewModel`, `HomeUiState`, `AddTokenDialog` — **2 VM tests** (TC-VM-001/002, 2.0.6); walked with live prices/chart | [2.4](features/2026-09-21-legacy-code-review/tasks/2.4-home-dashboard.md) | `[x]` |
| 2.5 Token detail | `feature:feature-tokens` | 3 files: `TokenDetailScreen`, `TokenDetailViewModel`, `TokenDetailUiState` — **6 VM tests** (2.0.6, matrix appendix); walked | [2.5](features/2026-09-21-legacy-code-review/tasks/2.5-token-detail.md) | `[x]` |

Prompt files 16–29 (Send, Receive, History, default token list, all of Phase 3, unit
tests, UI tests, performance pass) were **never written**. That work now flows through
the spec workflow — no prompt file will be authored for it.

---

## Phase 1.9 — Legacy code review (pre-SDD, Tasks 1.1–2.5) `✓ closed 2026-09-25 — 15/15 signed off`

**Goal:** code-review every feature implemented in the prompt-driven era, because none of
them ever received one. The reviewer records findings only; a separate **developer
session** fixes them, and a finding row is deleted from the record only after its fix is
verified.

- Spec directory: `specs/features/2026-09-21-legacy-code-review/` — decisions
  (`requirements.md`, owner Q1–Q9), method (`plan.md`), 15 task files (`tasks/`,
  index `tasks/README.md`), evidence (`validation.md`).
- Order is strict: 1.1 → 2.5, one task at a time; a task closes when its checklist
  verdicts and findings are recorded and it is **signed off** — by the owner, the reviewer,
  or the developer, once the fixes are verified and the user confirms. The gate closes when
  all 15 tasks are signed off.
- Known defects already owned by roadmap items (2.0.2b, the `TransactionRepositoryImpl`
  write paths, 2.0.3 empty callbacks, …) are **pre-registered** in the task files, not
  re-created.
- Reviewer write scope: record files and test files/test projects only.
- **Branch & close:** fixes run on a reviewer-created temp branch (`cr/<task>-<slug>`); close =
  mark the item done, commit, merge to `main`, delete the local branch, push `main` — by the
  reviewer or the developer, once fixes are verified and the user confirms.
- After the gate closes, the **next undone roadmap item** (Phase 2.0 remainder, 2.0.2b
  first) is proceeded — singly or as owner-approved grouped items.

---

## Phase 2.0 — Stabilization & verification debt

**Goal:** make the existing code true. No new features. Closes when the app has been run
on a device, the security suite compiles and passes, the dead ends are gone, and both
quality gates are green.

**Status 2026-09-27: the goal is met and the phase is closed by 2.0.7** — the app has been walked
and instrumented-tested on a Pixel 6a since 2.0.2b, `core-security`'s suite compiles and passes
(64 tests, 1 skipped), the dead ends are disabled with visible captions (2.0.3), and both gates are
green (2.0.5). 2.0.1–2.0.6 are `[x]`; 2.0.4b remains `[~]` for its M1/M2/M5 verification debt only.

Order within the phase is strict: 2.0.1 → 2.0.2 → **2.0.2b** → 2.0.3 → 2.0.4 → **2.0.4b** → 2.0.5 →
2.0.6 → 2.0.7.
2.0.2b was inserted on 2026-09-21: 2.0.2's first real run exposed a critical defect that
blocks every remaining step. 2.0.4b was inserted on 2026-09-26: 2.0.4's device walk exposed
that the explorer still speaks the **deprecated Etherscan V1** API, so transaction history
cannot return data (finding D2).

### 2.0.1 — Repair the `core-security` test suite `[x]`
**CLOSED 2026-09-27 by the Phase 2.0 close (2.0.7).** Automatic half done 2026-09-21 — the suite
compiles and runs for the first time; **64 tests, 0 failures, 1 skipped** (61 at the repair, +3
added by 2.0.6). The manual half for a test-repair item is the owner's sign-off on the nine
test-vs-implementation judgments, recorded as an explicit confirmation at the close
(`specs/features/2026-09-20-2.0.0-stabilization/validation.md` §2.0.7, M-A1).

Originally: 64 tests existed and had never executed, because
`:core:core-security:compileDebugUnitTestKotlin` did not compile:

| File | Error |
| --- | --- |
| `EncryptionManagerTest.kt:10` | `Unresolved reference 'mockito'` — Mockito is not a dependency; the project uses MockK |
| `MnemonicManagerTest.kt:61,68,69` | `No value passed for parameter 'passphrase'` |
| `MnemonicManagerTest.kt:92,99,105` | `Unresolved reference 'suggestWords'` |
| `SecurityUtilsTest.kt:15,112` | `Unresolved reference 'secureWipe'` |
| `SecurityUtilsTest.kt:43` | `Unresolved reference 'toHex'` |
| `SecurityUtilsTest.kt:42` | `Argument type mismatch: Int, but Byte was expected` |

**For each one, decide explicitly whether the test or the implementation is wrong** and
record the decision in `validation.md`. Changing production crypto to satisfy a stale
test is forbidden — `passphrase` may well be a deliberate hardening that the test simply
predates.

Outcome: **eight of the nine defects were stale test code; one was a real production
bug** — `getWordList()` returned 100 of 2048 BIP-39 words. A ninth defect
(`PasswordValidatorTest.testStrengthCalculationStrong`, threshold off by one) surfaced
only once the suite finally ran. See `validation.md` for the per-defect judgment table.

Exit criteria: `:core:core-security:testDebugUnitTest` compiles and passes; every
resolved symbol is traced to a real production API in `validation.md`. ✅ met
2026-09-21.

### 2.0.2 — First run on a device `[x]`
**CLOSED 2026-09-27 by the Phase 2.0 close (2.0.7).** Device: **Pixel 6a, now Android 17
(API 37)** over adb; the first runs were on API 36.
Evidence: `specs/features/2026-09-20-2.0.0-stabilization/validation.md` §2.0.2 and §2.0.2b.

Done:

- `app/src/main/AndroidManifest.xml` declared **no `<activity>`** and no `android:name`
  on `<application>`: `monkey` reported *"No activities found to run"*, so the app **had no
  entry point and could never have been launched**, and `@HiltAndroidApp` was never
  registered. Fixed — `.MainActivity` with a MAIN/LAUNCHER filter, `.NexVaultApplication`
  on the application element. This, not the placeholder tabs, is why the GUI never worked.
- Installed, launched, and rendered the Welcome screen on the device: dark theme,
  edge-to-edge, no crash. The predicted stray ActionBar does **not** exist — `MainActivity`
  extends plain `FragmentActivity`, so a `MaterialComponents` theme builds no ActionBar.
  Item 4.14 is closed as refuted.
- The SDK 37 toolchain and the `compileSdk` 37.2 / `targetSdk` 37 bump.

Not done in this item: the rest of the happy path — one tap past Welcome it hit **2.0.2b**.

Exit criteria: met by the 2.0.2b walk (Welcome → mnemonic → verify → Set PIN → Home, cold-start
unlock, both import paths; written trace M1–M11) and by every walk since, up to the instrumented
creation-flow E2E (2.0.6). No crash was filed without an owner: the two defects the first run found
were fixed in 2.0.2b, and the stray-ActionBar prediction was refuted and closed (4.14).

### 2.0.2b — Repair the onboarding flow `[x]`
**CLOSED 2026-09-25 — owner-directed close after the device walk passed.** Own spec:
`specs/features/2026-09-25-2.0.2b-onboarding-repair/` (requirements with owner decisions Q1–Q3,
plan, validation with both halves). Merged to `main`; the temporary feature branch is deleted.

What landed (owner decisions: **B + A** for the flow, **KeyStore-only + session gate** for
CR 1.6-1, single item):

- The wallet is no longer created in `CreateWalletViewModel.init`: a draft (mnemonic + address) is
  generated and shown, and persisted only when the user acknowledges it and taps Continue.
- `is_wallet_set_up` is now written only by the new `CompleteOnboardingUseCase`, called after the
  PIN is stored. The root router checks the `AppStateManager` session first, so completing
  onboarding cannot bounce the graph through Unlock mid-flow (import path included).
- CR 1.6-1: wallet material is encrypted with the KeyStore-wrapped layer only (the void
  wallet-UUID PBKDF2 layer — `doubleEncrypt`/`DoubleEncryptedData` — is gone), and
  `getMnemonicForBackup`/`addAccount` are gated on the unlocked session, with an onboarding
  exemption for the Verify step.
- Abandoned onboarding attempts are wiped before a new wallet is persisted, so `mnemonic.enc`
  (single-file) cannot be silently overwritten.
- Two further defects surfaced by the device walk and were fixed in the same pass: the mnemonic
  grid crashed (`LazyVerticalGrid` inside a scrolling column — it had never rendered before), and
  onboarding completion was cancelled mid-write by the router tear-down (now unlocks first, inside
  `NonCancellable`).

Evidence: build passes; **249 tests, 0 failures, 1 skipped**; detekt 71 → 65 and ktlint 1462 → 1454
versus HEAD, with **no file's detekt count increased**. **Device walk PASSED 2026-09-25** on the
Pixel 6a: create → mnemonic → verify → Set PIN → Home; cold start → Unlock → Home; mnemonic import
(canonical vector → address `0x9858EfFD…Eda94`); private-key import (key `1` → address
`0x7E5F4552…95Bdf`); abandoned attempt → Welcome + single wallet row; no crashes after the fixes.
Per-step evidence: `specs/features/2026-09-25-2.0.2b-onboarding-repair/validation.md` (M1–M11).
**Signed off by owner direction 2026-09-25** (close: merge to `main`, push, delete the temporary
branch). The three optional human checks (visual grid, biometric, back-navigation) were offered and
not separately reported — recorded as such in the validation record, not silently omitted.

**BLOCKER, found 2026-09-21 by the first real run.** The wallet is created but the user
never sees the mnemonic, and the app is left unusable on the device.

Tapping **Create New Wallet** lands on the Unlock screen instead of the mnemonic. The
wallet really was created — `files/wallet/mnemonic.enc` (386 bytes) and a real BIP-44
address `0xDc6D56BfFA21b1E9bb3C6B02F7c7cC071d351BEe` are on disk — but
`security_preferences` holds **no `password_hash`**, so no PIN was ever set.

Root cause: `CreateWalletViewModel` calls `createWalletUseCase` from its `init` block, which
persists the wallet *and* flips `is_wallet_set_up`; `NexVaultApp` uses that flag in a
`remember`-derived `routingKey` wrapped in `key(...)`, so the whole root NavHost is torn
down and rebuilt at the auth graph mid-flow. The mnemonic screen and the **Verify
Mnemonic** step are never reached.

Why it outranks everything else here: the mnemonic is the only recovery path for a
non-custodial wallet. It is generated, encrypted and stored, but never shown and never
verified — the wallet is unbacked-up from birth, with no consent step. And the install is
now unrecoverable: a PIN is demanded that was never set.

**Planning must also re-read legacy CR finding 1.6-1** (deferred here 2026-09-23): the
mnemonic's/private key's encryption "password" is the wallet UUID stored in plaintext in
`wallet_metadata`, so the inner PBKDF2 layer holds zero secret and mnemonic retrieval has no auth
gate — the remediation must decide (encrypt with the PIN once set / drop the inner layer / re-key)
and enforce authentication before retrieval.

Remediation options (A: move the flag write to the end of onboarding — smallest unblock;
B: stop creating the wallet in `init` — matches the security intent; C: drop the reactive
root router — kills the whole bug class). **Owner decision 2026-09-25: B + A** (C out of scope);
CR 1.6-1 remediated by **dropping the inner layer and enforcing an unlocked-session gate**.
Spec: `specs/features/2026-09-25-2.0.2b-onboarding-repair/`.

Exit criteria: with cleared app data, onboarding runs Welcome → mnemonic shown → verify →
Set PIN → main, and the mnemonic that appears is the one that decrypts `mnemonic.enc`.

### 2.0.3 — Close the navigation dead ends `[x]`
**CLOSED 2026-09-25 — reviewer accepted (verification commits `da3d9ff`/`c3858cf`, incl. a device
re-walk: captions render, controls disabled, taps inert, 0 crashes) and the owner directed the
close (merge `main`, push, delete the branch).** Own spec:
`specs/features/2026-09-25-2.0.3-navigation-dead-ends/`. Owner decision: **disable + visible
explanation** (no destinations exist yet: Send 2.6, Receive 2.7, Swap 3.3, History 2.8).

- Home's Send/Receive/Swap and TokenDetail's Send/Receive/See All are disabled, each row with a
  phase-explaining caption; the empty-lambda wiring in `MainScreen` is deleted.
- The four placeholder tabs were already informative ("Coming in Phase X") — not dead ends,
  unchanged.
- Verified: build green, **249 tests / 0 failures / 1 skipped**, gates **identical to main**
  (detekt 65, ktlint 1454), device walk passed (captions render, buttons disabled, taps inert,
  Home → TokenDetail and all tabs still work, 0 crashes). The disabled See All is diff-verified
  only — no transaction data exists without API keys (2.0.4).

`app/src/main/java/com/nexvault/wallet/ui/main/MainScreen.kt` wires Home → TokenDetail,
but passes empty lambdas for send, receive, swap, and history. Combined with the four
placeholder tabs, the app currently has no path to any Phase 2 feature.

Exit criteria: either the callbacks navigate somewhere real, or the buttons that depend
on them are disabled with a visible explanation. A click that silently does nothing is
the defect — not the absence of the destination.

### 2.0.4 — Decide the API-key strategy `[x]`
**CLOSED 2026-09-26 — owner-directed close after the reviewer's verification PASS (`bf8daf5`, of
`3b57c48`).** Own spec: `specs/features/2026-09-25-2.0.4-api-key-strategy/` (requirements with the
per-screen table, plan, validation with both halves and the D2 resolution). Merged to `main` with a
merge commit and pushed; the local `feature/2.0.4-api-key-strategy` branch is deleted.
**Next open entry point: 2.0.5** (2.0.4b closed 2026-09-26 as `[~]`, its device-walk evidence owed).

Owner decisions: **real keys are the supported path** (supplied 2026-09-25); a missing keyed
dependency shows an explicit **"Not configured"** state, distinct from network errors; CoinGecko
stays key-optional.

What landed:

- `app/build.gradle.kts` finally **loads `local.properties`** (previously `project.findProperty`
  saw neither it nor `~/.gradle/gradle.properties`, so `BuildConfig` carried empty strings).
- The Alchemy/CoinGecko lines' inline `#` notes were moved to their own lines first — under
  `java.util.Properties` an inline `#` is part of the value.
- `ChainConfigProvider.isRpcConfigured/isExplorerConfigured` + `ApiKeyNotConfiguredException`; the
  RPC/explorer repository paths fail before touching the network; Home shows a persistent
  "not configured" notice and TokenDetail says why its history is empty.
- **D1 (found by the walk, fixed):** `core:core-network` never declared
  `ksp(moshi-kotlin-codegen)` — the `@JsonClass` DTOs had no generated adapters, so **every
  Retrofit call died with "Unable to create converter" before any I/O**. That is why every
  network-backed screen has looked empty since Phase 2; the owner's keys could not have helped.
  Now 42 adapters generate, CoinGecko answers 200, and the app shows live prices.
- **D2 (open, owner decision):** the explorer base URL is Etherscan **V1**, which Etherscan now
  rejects with `NOTOK — deprecated V1 endpoint`. Options (A migrate to V2 here / B defer to 2.8 /
  C mainnet-only V2) and the recommendation are in `validation.md` §D2. **Owner decision 2026-09-26:**
  none of A/B/C verbatim — the migration becomes its own tracked item, **2.0.4b** (below), executed
  after this item and before 2.0.5. The decision is recorded in that `validation.md`; the migration
  itself is **not** implemented.

Evidence: build green; **255 tests, 0 failures, 1 skipped**; detekt/ktlint identical to `main`
(65 / 1471, no file worse); device walk — live price `$2,691.24`, 24h `+0.58%`, chart renders,
key-absent notice renders when `INFURA_API_KEY` is blanked, 0 crashes.

All five `local.properties` keys are `your_key_here`. Determine, per screen, what the
correct behaviour without keys is (explicit "not configured" state vs. empty state vs.
retry), and make the network-backed screens honour it. Faking data to make a screen look
populated is forbidden.

**Owner decision (answered 2026-09-25):** real keys supplied and are the supported path;
key-absent degradation is the documented fallback, with an explicit "Not configured" state per
keyed surface (recorded in the item's `validation.md`).

### 2.0.4b — Migrate the explorer to Etherscan API V2 `[~]`
**CLOSED 2026-09-26 as `[~]` — MERGED TO `main` (`--no-ff`) and pushed, NOT VERIFIED; the local
feature branch is deleted.** Basis: reviewer verification **PASS** on `e82eba2` (`744251d`, R1–R9;
R10 device re-walk blocked by a locked phone). The manual half: **M3/M4 PASSED 2026-09-28** (the BSC
plan notice and the blank-key "Not configured" notice both rendered on the Pixel 6a — see the 2.0.7
close record), M1/M2 remain suspended with the owner's funding decision and **M5 is owed** — the row
moves to `[x]` only when M1/M2 pass.
**ADDED 2026-09-26 by owner decision on 2.0.4 finding D2.** Own spec:
`specs/features/2026-09-26-2.0.4b-etherscan-v2-migration/` (requirements with the owner Q/A and the
per-screen table, plan with the task groups and the implementation decisions, validation with the
automatic evidence, §Suspended and §Close). Branch was cut from the post-2.0.4 `main` (`e4d1f02`).
**Owner decision 2026-09-26: funding the walk wallet is suspended**, so the balance-dependent device
walk rows (mainnet/Polygon history) are suspended too **until the active account's balance is not
zero**; the BSC plan notice, the blank-key state and the crash watch need no balance and stay
walkable. Evidence and resume condition: the item's `validation.md` §Suspended / §Close.

Why it is a defect and not a chore: the app's explorer hosts are all V1 —
`api.etherscan.io`, `api-sepolia.etherscan.io`, `api.bscscan.com`, `api.polygonscan.com`
(`ChainConfigProvider.kt:75-102`), with `@GET("api")` and no `chainid`
(`BlockExplorerApi.kt`). V1 was fully deprecated on 15 August 2025
([Etherscan KB](https://kb.etherscan.com/etherscan-api-v1-will-be-fully-deprecated-by-15th-august-2025)),
so every `refreshTransactionHistory` call now returns
`{"status":"0","message":"NOTOK","result":"…deprecated V1 endpoint…"}` — and because 2.0.4's DTOs
declare `result: List<TransactionDto>` (non-null), the envelope does not even parse: the failure is
swallowed by a generic `catch`. **Transaction history cannot return data at all**, and 2.8 would
build its list on top of a dead source.

Owner decisions (2026-09-26):

- **All four chains through V2**: one endpoint `https://api.etherscan.io/v2/api` (base
  `https://api.etherscan.io/v2/`) with the mandatory `chainid` query parameter for 1, 11155111, 56
  and 137, and **one `ETHERSCAN_API_KEY`** for all of them — the V2 unified key
  ([V2 announcement](https://info.etherscan.com/switch-to-etherscan-api-v2-by-may-31-2025/)).
  No new `local.properties` key; no BscScan/PolygonScan-specific key. **Verified 2026-09-26 by a
  masked probe** (`validation.md` §P3): the free key answers `status:"1"` for 1 / 11155111 / 137 and is
  **plan-gated on 56** — *"Free API access is not supported for this chain."* — so BSC history is a
  third state, not a key problem.
- **Scope**: endpoint + `chainid` + **error-envelope surfacing** + **plan-gate surfacing**. The
  silent-empty behaviour is part of the defect: an explorer error must become an explicit
  `DataResult.Error`, the plan gate a distinct exception with a persistent notice (message-driven, no
  chain id hard-coded, so a paid plan later needs no code change), while a legitimate
  `"No transactions found"` stays a successful empty result. True pagination and the History UI stay
  with 2.8.
- **Consequence to read with 2.0.4's per-screen table**: BSC/Polygon stop being keyless on the
  explorer side (`isExplorerConfigured` requires the key for every supported chain), so when
  `ETHERSCAN_API_KEY` is blank those history surfaces now show the explicit "Not configured" state —
  a user-visible change superseding that table's BSC/Polygon row. Their **RPCs** stay public and
  keyless, so `isRpcConfigured` is unchanged.
- **Verification**: unit tests on the built request (path `/v2/api` + `chainid`/`apikey` per chain,
  via a capturing OkHttp interceptor — no new dependency), a masked live `curl` against V2, and a
  device walk on a **known-active address** (owner-supplied: the 2.0.4 wallet holds 0 wei, where an
  empty history proves nothing).

Exit criteria: AC-1…AC-8 of the item's `requirements.md` hold with recorded evidence — V2 +
`chainid` asserted per chain, one key configured for all four, error envelopes and the 56 plan gate
surfaced as distinct states, masked `curl` returning `status:"1"` on 1/11155111/137, TokenDetail
listing real history on the Pixel 6a (and the plan notice on BSC), and detekt/ktlint not worse than
the merge base.

### 2.0.5 — Turn both quality gates green `[x]`
**CLOSED 2026-09-27 as `[x]` — VERIFIED, merged to `main` (`--no-ff`) and pushed; the local feature
branch is deleted.** Basis: reviewer verification **PASS** on `b0dfb92…5b1bfb4` (`729d663`, V1–V10 +
A4 — both gates re-run from a fully fresh `--rerun-tasks` execution, 633 tasks, exit 0, zero
findings) for the automatic half, and the owner's device walk on the Pixel 6a the same day for the
manual half — reported satisfied and corroborated by the device showing Home fully rendered with
live prices and a drawn chart on this build, 0 crashes across the session. Own spec:
`specs/features/2026-09-26-2.0.5-quality-gates/` (requirements with the owner Q1–Q4 and the
disabled-rule inventory, plan, validation with the automatic evidence, the manual half and the
close record).

Baseline at `main` (`aa539d6`), re-measured on the Linux host: **1556 ktlint findings across 184
files** and **65 detekt issues / 78 weighted**, both gates exit 1. (The 1656/178 and 75 figures
this section previously carried were the 2026-09-20 measurement — 2.0.2b, 2.0.4 and 2.0.4b moved
them; the evidence appendix E4/E5 keeps both.)

**The two owner decisions the item asked for were answered 2026-09-26:**

1. *ktlint* — **(b) codify the existing style.** `.editorconfig` disables the 11
   `ktlint_official` layout rules (`multiline-expression-wrapping` 481, `function-signature` 321,
   `function-expression-body` 104, `annotation` 92, `argument-list-wrapping` 58,
   `chain-method-continuation` 43, `class-signature` 19, `if-else-wrapping` 4,
   `multiline-if-else` 3, `parameter-list-wrapping` 2, `condition-wrapping` 1 = 1128 findings) and
   sets `ktlint_function_naming_ignore_when_annotated_with = Composable` (93 composables). Every
   hygiene rule stays on. Option (a) was measured rather than dismissed: reformatting would have
   rewritten 181 files (+6616/−5954 lines) and Android Studio's *Reformat Code* would undo it.
2. *detekt* — **(b) deliberate config + real fixes, no baseline file.** One uniform
   `TooManyFunctions` ceiling of 25 instead of 11, `CommentOverPrivateFunction` off (comments
   already carry weight 0), `MaxLineLength` stays **120** with every long line wrapped, and the 2
   `ComplexCondition`s extracted into a named local.
3. Root scripts — **inside the gate now**: the plugin is applied to the root project too, so
   `build.gradle.kts` / `settings.gradle.kts` are linted (Q3).

What landed, in four commits: `.editorconfig` + root plugin + `ktlintFormat` output (126 files,
+261/−318 — whitespace, import order and trailing commas only, proven with
`git diff --ignore-all-space --ignore-blank-lines`); the 15 non-auto-correctable ktlint findings
fixed in code (11 wildcard imports, 2 over-long literals, `Dimens.kt` → `NexVaultDimens.kt`,
`_selectedChartDays` → `selectedChartDaysFlow`); the detekt config plus 23 wrapped lines and the
`UnlockViewModel` guards; then the records.

Evidence: `./gradlew detekt ktlintCheck --continue` → **BUILD SUCCESSFUL, exit 0** (detekt 65 → 0,
ktlint 1556 → 0); `:app:assembleDebug testDebugUnitTest :domain:test --continue` → **274 tests /
0 failures / 0 errors / 1 skipped**, APK
`/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk` (47,986,669 bytes);
0 tracked Kotlin lines above 120 characters; per-rule attribution in `validation.md` §A3.

Exit criteria (AC-1…AC-8 of the item's `requirements.md`): **all eight ✅ met** — AC-1…AC-7 with the
evidence above (reviewer-confirmed), AC-8 by the owner's device walk the same day. The item is
**done**: `[x]`.

Close record: `validation.md` §"Close / sign-off" (reviewer PASS `729d663`, seven commits
`b0dfb92…0eb34f4` plus this verification update, `--no-ff` merge `0985563`, branch deleted, the
manual half and the honesty notes).

### 2.0.6 — Build the TC traceability `[x]`
`doc/07-TEST-CASES.md` specifies 52 cases (TC-SEC 8, TC-SECTEST 5, TC-UC 4, TC-REPO 5,
TC-NET 3, TC-DB 3, TC-VM 10, TC-UI 10, TC-INT 4). No test method references a TC id, so
nobody can tell which specified behaviours are actually covered.

Exit criteria: each TC id is mapped to a test method, or registered as an unimplemented
roadmap item. The 10 TC-UI cases have no home at all today — the only instrumented test
is the `ExampleInstrumentedTest` template stub.

**VERIFIED 2026-09-27, reviewer PASS, owner-directed close → `[x]`.** 25 of 52 ids are mapped to
test methods with `// TC-XXX-NNN` markers (audit-proven: 52 = 25 ∪ 27, zero orphans, 25/25 markers
adjacent to their `@Test`), and 27 are registered as unimplemented with named owners (2.6 ×8,
2.8 ×2, 3.1/3.2/3.3 ×1 each, 3.4 ×3, 4.7 ×3, 4.8 ×2, 4.13 ×1, 4.18 ×5 — the new row below).
The suite grew **274 → 299 tests / 0 failures / 0 errors / 1 skipped** (+25 attributed:
TC-NET ×6, TC-DB ×3, TC-VM-001/002, TokenDetail VM ×6, TC-UC-001, refresh-use-case ×3,
TC-VM-009, TC-SEC-003/007/008), including the repo's first Robolectric suite (`NexVaultDaoTest`)
and the TC-INT-001 instrumented creation-flow E2E — **5/5 instrumented tests passed on the Pixel
6a, re-driven by the reviewer**. Both gates exit 0. The legacy CR findings 1.2-5/2.1-1/2.2-1/2.4-1/
2.5-1 are closed or replanned by it, finding 1.1-3 (the `ExampleInstrumentedTest` stub) is fixed,
and the item fixed one production defect found by its own new tests: `RefreshTransactionHistoryUseCase`
swallowed the repository's `DataResult`, so the 2.0.4/2.0.4b token-detail notices (missing key,
plan gate) were dead states. Evidence: `specs/features/2026-09-27-2.0.6-tc-traceability/` (the
authoritative register is `traceability.md`; the close record is `validation.md` §"Close / sign-off"
— reviewer commits `e7b0c5f` + `1efe2e8`, `--no-ff` merge, branch deleted). The roadmap row's
earlier "only instrumented test is the template stub" claim was already stale — `BackupPolicyTest`
(3 device tests) has existed since 2.0.2.

### 2.0.7 — Record and close `[x]`
Write `specs/features/2026-09-20-2.0.0-stabilization/validation.md` covering 2.0.1–2.0.6
with commands, observed results, and absolute artifact paths; then flip the Phase 0 rows
that earn it to `[x]`, and carry forward anything still deferred as a Handoff list.

**CLOSED 2026-09-28 as `[x]` — owner-directed after both halves, with the Phase 0 re-grade accepted,
and reviewer-verified PASS (`bb141fb`, V1–V7 executed on `main`; owner confirmation the same day).
Merged to `main` (`--no-ff`), pushed, branch deleted.** The umbrella section
(`validation.md` §"2.0.7 — Phase 2.0 close") carries: the per-item summary 2.0.1 → 2.0.6; the fresh
evidence block (suite **299 / 0 / 0 / 1** from a 571-task `--rerun-tasks` run, both gates exit 0,
APK `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk`, TC audit
52 = 25 ∪ 27 with 25/25 adjacency, instrumented **5/5** on the Pixel 6a); the Phase 0 flip table
(**15 rows → `[x]`** — 12 Phase 0 whose findings are all closed, plus 2.0.1, 2.0.2 and 2.9 — with
1.2/1.4 staying `[~]` for open 4.17/4.18 findings and 2.0.4b for M1/M2/M5); the consolidated Handoff
(union of all seven Phase 2.0 handoff lists); and the 2.0.4b **M3/M4 rows, both walked and passing on
the device 2026-09-28** (the BSC plan notice and the blank-key "Not configured" notice — the two
states 2.0.6's fix un-dead-coded). Manual half: M-A1 (the owner's confirmation of 2.0.1's nine
judgments) and M-A3 (flip-table acceptance) both confirmed 2026-09-28; M-A2 was the agent-executed
M3/M4 device walk at the owner's instruction. Records updated in the same pass: the Phase 0 table +
header, 2.0.1/2.0.2/2.9, the current-position block, AGENTS.md (standing gate → **Phase 2.0 closed,
next entry 2.6**; plus the corrected key-blanking note), mission.md, tech-stack, and 4.15 closed as
refuted.

---

## Phase 2 — Core features (remaining) `✓ CLOSED 2026-10-02 by 2.10`

Unblocked only after Phase 2.0 closes. **The phase is complete: 2.6, 2.7, 2.8, 2.9 and
2.10 are `[x]`; 2.0.4b's M1/M2/P4 stay suspended pending funding (the global register:
`specs/pending-manual-test-cases.md`).**

| Item | Scope | Landing module | Acceptance | Status |
| --- | --- | --- | --- | --- |
| 2.6 Send transaction flow | Form → review (gas slow/normal/fast) → submit → result, native + ERC-20 | `feature:feature-send` + `TransactionRepositoryImpl` | `AC-2.6` | `[x]` — **closed 2026-09-30 (owner close)**: 29 commits; 417 unit tests + 10/10 instrumented; gates green; coverage 53.4% (Kover migration); 0 criticals; TC-INT-002 mapped (funded submit deferred to 2.10); evidence in `specs/features/2026-09-28-2.6-send-transaction-flow/validation.md` close record |
| 2.7 Receive screen | QR (ZXing) + copy + share, chain badge | `feature:feature-receive` | `AC-2.7` | `[x]` — **closed 2026-09-30 (owner close after reviewer verification)**: 10 commits; 443 tests + gates green; server coverage 54.1%, 0 bugs/vulnerabilities/criticals; TC-UI-006 mapped (send-form QR scan, CameraX + zxing); handoff — device re-run + second-phone QR scan at 2.8 |
| 2.8 Transaction history | Date-grouped list, filters, pagination, detail screen | `feature:feature-history` | `AC-2.8` | `[x]` — **closed 2026-10-02 (owner close after reviewer PASS + fix re-verification)**: 473 tests / 0 failures / 1 skipped; gates green; server coverage 56.4%, 0 bugs/vulnerabilities/criticals; both roadmap shortcuts (paging, chain receipt checks) closed with tests; handoff — device re-run + walk + second-phone QR scan at 2.10 |
| 2.9 Default token list | Seed tokens on wallet creation / chain switch | `data` + `core:core-database` | Phase 2 checklist | `[x]` — implemented by the legacy CR 1.5 fix round (creation paths + `RefreshBalancesUseCase` chain-switch seeding); closed by 2.0.7 on the device evidence (seeded ETH/USDC rows rendered in the 2.0.5 walk) + `RefreshBalancesUseCaseTest` |
| **2.10 Milestone scan & Phase 2 close** | Run the static-analysis scan over everything Phase 2 built (new-code gate), triage the findings, verify the Phase 2 checklist, then record and close the phase | repo-wide (`app`, `feature:*`, `data`) | Phase 2 checklist + the scan gate | `[x]` — **closed 2026-10-02 (owner close after reviewer PASS, no findings)**: scan gate green (duplication 0.5 → 0.1, 0 new blockers/criticals/hotspots; coverage 56.4%), new S107 + 2.8 duplication fixed, 473 unit tests / 0 fail / 1 skip, both gates fresh green, instrumented 11/11 on the closed and fixed revisions, History walk + BSC plan notice + M5 crash watch done, funded Sepolia send closed on-chain (MAN-005 `[x]`; tx `0xc4db4903…298f83`), QR reader row closed (MAN-001 `[x]` — owner-approved zbar decode). Evidence: `specs/features/2026-10-02-2.10-milestone-scan-phase2-close/validation.md` |

**2.6 entry conditions (verified in code):** `GasEstimate`, `GasOption` and
`SendTransactionParams` already exist in `domain/model/transaction/`, and
`TransactionRepository` already declares `estimateGas`, `sendNativeTransaction`,
`sendTokenTransaction`, `getTransactionHistory`, `refreshTransactionHistory`,
`getRecentTransactionsForToken`, `getTransactionDetail`, `getPendingTransactions`,
`updateTransactionStatus`. `TransactionRepositoryImpl` implements the read paths for real
but returns `UnsupportedOperationException` for **all three write paths** and for
`estimateGas` — so 2.6 is precisely: implement those three methods against
`Web3jProvider`, then build the UI.

**Known shortcuts inside 2.5/2.8 territory** to address while implementing:
`getTransactionHistory` ignores its `page` / `pageSize` arguments (returns the full
observed list), and `updateTransactionStatus` returns the cached entity without
consulting the chain.

**2.8's data source:** the explorer must be on **Etherscan V2 (2.0.4b)** before history can return
anything — until then `refreshTransactionHistory` cannot produce a row, whatever 2.8's UI does.

---

## Phase 3 — Advanced features

| Item | Scope | Landing module | Acceptance | Status |
| --- | --- | --- | --- | --- |
| 3.1 WalletConnect v2 | Pairing via QR, session approval, `personal_sign` / `sendTransaction`, session list | `feature:feature-dapp` | `AC-3.1` | `[ ]` |
| 3.2 NFT gallery | ERC-721/1155 list, metadata, attributes, IPFS images | `feature:feature-nft` | `AC-3.2` | `[ ]` |
| 3.3 Token swap | Aggregator quote, ERC-20 approval, testnet execution | `feature:feature-swap` | `AC-3.3` | `[ ]` |
| 3.4 Settings & network management | Security, network CRUD, address book, recovery-phrase export | `feature:feature-settings` | `AC-3.4` | `[ ]` |
| 3.5 Background sync | WorkManager balance/pending-tx sync with constraints | `core:core-network` + `data` | `AC-3.5` | `[ ]` |
| 3.6 Deep linking | `ethereum:` and `wc:` intent handling | `app` | `AC-3.6` | `[ ]` |
| 3.7 Final polish | Icon, splash, edge-to-edge, i18n (EN + ZH), R8 release run | `app` | `AC-3.6`, Phase 3 checklist | `[ ]` |
| **3.8 Milestone scan & Phase 3 close** | Static-analysis scan over Phase 3 with the new-code gate, triage, Phase 3 checklist, then record and close | repo-wide | Phase 3 checklist + the scan gate | `[ ]` — see "Milestone static-analysis scan" above |

`NftEntity`/`NftDao` (2.2) and `AddressBookEntity`/`AddressBookDao` (2.2) already exist
as storage for 3.2 and 3.4. WorkManager (2.11.1) and the WalletConnect BOM (1.35.2) are
already in the version catalog for 3.5 and 3.1.

---

## Phase 4 — Tech debt & hardening

| Item | Scope |
| --- | --- |
| 4.1 | Extract convention plugins into a `build-logic/` module (20 modules duplicate their config today) |
| 4.2 | Decide `core:core-common`'s fate: populate it with the utilities it was meant to hold, or remove it from `app`/`data`/all features |
| 4.3 | Add CI so the gates run automatically instead of only on demand |
| 4.4 | Encrypt Room with SQLCipher (currently plaintext on disk) |
| 4.5 | Decide `junit-jupiter` (catalog-only, unused): adopt JUnit 5 or delete the entry |
| 4.6 | Configure Spotless with real formatters, or remove the plugin — today it is a no-op that implies coverage it does not provide |
| 4.7 | Security hardening per `doc/08`: `FLAG_SECURE` on sensitive screens, private-key zeroing after signing |
| 4.8 | Reach the `doc/08` code-quality bar: ≥70% coverage on `domain` + `data`, KDoc on all public API |
| 4.9 | Clear the Gradle deprecation warnings blocking a Gradle 10 upgrade |
| 4.10 | Decide the `androidx.biometric` alpha pin (`1.4.0-alpha05`) |
| 4.11 | Fix the password-strength scale: `PasswordValidator.calculateStrength` can never exceed **75** while its `coerceIn(0, 100)` implies 100, so a strength meter would top out three-quarters of the way up its own bar. Found during 2.0.1; no production caller yet. |
| 4.12 | Split `SecureUtils` / `SecurityUtils` — two unrelated objects in one file, one hosting extension functions. It caused three of the six 2.0.1 test defects by making the API surface guessable-but-wrong. |
| 4.13 | Port the `@Ignore`d AndroidKeyStore tests from 2.0.1 to `app/src/androidTest` once 2.0.2 has established a device. |
| 4.14 | ~~Normalise the Android theme~~ **REFUTED 2026-09-21** — the Welcome screen renders with no ActionBar: `MainActivity` extends plain `FragmentActivity`, not `AppCompatActivity`, so the `MaterialComponents` theme never builds one. The prediction was wrong; nothing to fix. |
| 4.15 | ~~Decide whether `targetSdk 37` runtime behaviour needs verification before 3.7; the only available device is API 36.~~ **CLOSED 2026-09-27 by 2.0.7 as refuted** — the Pixel 6a was upgraded to **Android 17 / API 37**, so every 2.0.x walk since 2.0.2's toolchain bump has run on the target API; nothing separate remains to check. |
| 4.16 | Migrate `hiltViewModel` to `androidx.hilt.lifecycle.viewmodel.compose` — the old `androidx.hilt.navigation.compose` entry point is deprecated. Surfaced by the first build on the remote host; handle alongside the next `hilt-navigation-compose` bump. |
| 4.17 | Adopt design tokens, string resources and component previews across feature modules and `core-ui` (legacy CR findings 1.2-1 … 1.2-4; owner-approved 2026-09-21). |
| 4.18 | Instrumented Compose UI tests for the screens that already exist — Welcome, CreateWallet, `PinInputField`, Home token list and pull-to-refresh (TC-UI-001…005 registered by 2.0.6, incl. the 2.0.2b onboarding-grid handoff and the finding 1.2-5 composable residue). Screen-level TC-UI cases for later screens land with their feature items (2.6/2.8/3.2/3.4). |
| **4.19 Milestone scan & repo close-out** | Final static-analysis scan over the whole repository (new-code gate plus a reprioritised look at the accepted-debt list), triage, then the repo-level record and close — see "Milestone static-analysis scan" above |

---

## Evidence appendix — baseline measured 2026-09-20, updated 2026-09-21

Commands run from the repository root; `JAVA_HOME` set to the Android Studio JBR.

| # | Command | Result |
| --- | --- | --- |
| E1 | `./gradlew :app:assembleDebug` | **PASS** → `/Users/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk` |
| E2 | `./gradlew testDebugUnitTest --continue` (2026-09-21, after 2.0.1) | **223 tests, 222 pass, 1 skipped, 0 fail** — `domain` 63, `core-security` 61 (1 skipped), `feature-onboarding` 30, `core-datastore` 27, `data` 27, `feature-auth` 14, `app` 1 |
| E2b | Same, pre-2.0.1 baseline | **162 pass** across 6 modules; `core-security`'s 64 tests did not compile at all |
| E3 | `./gradlew :core:core-security:compileDebugUnitTestKotlin` | **FAIL → FIXED 2026-09-21** — was 16 compile errors (Mockito, `passphrase`, `suggestWords`, `secureWipe`, `toHex`, `Int`/`Byte`) |
| E4 | `./gradlew detekt` | **FAIL** — 75 issues (`TooManyFunctions`, `MaxLineLength`), config `maxIssues: 0` — **PASS from 2026-09-26 (2.0.5): 0 issues, exit 0** (see E9) |
| E5 | `./gradlew ktlintCheck` | **FAIL** — 1656 violations in 178 of 221 Kotlin files — **PASS from 2026-09-26 (2.0.5): 0 findings, exit 0** (see E9) |
| E6 | `adb devices` / `ls $ANDROID_HOME/emulator` / `ls ~/.android/avd` | **empty / missing / empty** — no device, no emulator, no AVD |
| E7 | `grep -r TODO\|FIXME --include=*.kt app core domain data feature` | **0** — the `doc/08` "no TODO/FIXME" criterion passes |
| E8 | `find . -path "*src/androidTest*" -name "*.kt"` | **1 file** — the template stub only; no real instrumented coverage |
| E9 | `./gradlew detekt ktlintCheck --continue; echo $?` (2026-09-26, 2.0.5) | **BUILD SUCCESSFUL, exit 0** — detekt **65 → 0**, ktlint **1556 → 0**; run on Linux with `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`. Attribution per rule: `specs/features/2026-09-26-2.0.5-quality-gates/validation.md` §A3. Independently reproduced fresh by the reviewer (2026-09-27, `--rerun-tasks`, 633 tasks, exit 0). Both gates are green only when someone runs them — there is still no CI (4.3) |
| E10 | Same run, after 2.0.5 | `:app:assembleDebug testDebugUnitTest :domain:test --continue` → **274 tests, 0 failures, 0 errors, 1 skipped**; detekt covers `main` + `test` only, ktlint also covers `androidTest` and the root scripts |

These numbers are the reference point for every claim in this roadmap. Re-run them before
asserting that anything has changed.
> **2.6 (Send transaction flow) CLOSED 2026-09-30 as `[x]` — owner-directed close ("close 2.6"),
> merged to `main` (`--no-ff`), pushed, the local feature branch deleted.** The send flow
> (form → review → gas slow/normal/fast → PIN-signed submit → result hash + explorer link,
> native + ERC-20) is implemented and verified: 417 unit tests / 0 failures, instrumented **10/10**
> on the Pixel 6a (TC-INT-002 walk automated as `SendFlowE2ETest`), both gates green, SonarQube
> **coverage 53.4%** with **0 criticals** (Kover 0.9.11 migration, owner-approved 2026-09-30).
> Deferred: the funded on-chain submit → 2.10's walk (funding register), QR scan → 2.7,
> address book → 3.4, dispatcher injection → 4.8. **The next open entry point is 2.7**
> (Receive screen — QR + copy + share).

> **2.7 (Receive screen) CLOSED 2026-09-30 as `[x]` — owner close after reviewer verification,
> merged to `main` (`--no-ff`), pushed, the local feature branch deleted.** Delivered: the Receive
> screen (chain badge, QR of the address, full copyable/shareable address, network-compatibility
> warning) and the send flow's QR scanner (CameraX `Preview`/`ImageAnalysis` + zxing decode, TC-UI-006
> mapped). Evidence: 443 unit tests / 0 failures / 1 skipped, both gates green, SonarQube **coverage
> 54.1%** with **0 bugs, 0 vulnerabilities, 0 criticals** and no open issue in any 2.7 file; all ten
> reviewer findings resolved (`69c3796`, `fba7234`) and re-verified (`d75b328`). Owed: the device
> suite re-run on the closed revision plus the second-phone QR scan → **2.8**. **The next open
> entry point is 2.8** (Transaction history).
