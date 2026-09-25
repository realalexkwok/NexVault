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

## Current position

> **Phase 1.9 (Legacy code review) is CLOSED 2026-09-25 — all 15 pre-SDD features reviewed and
> signed off.** Spec: `specs/features/2026-09-21-legacy-code-review/` (validation.md records the
> gate closure and every finding handed to its owning item).
>
> **The next open entry point is 2.0.3 (navigation dead ends)** — the Phase 2.0 remainder
> (2.0.3 → 2.0.4 → 2.0.5 → 2.0.6 → 2.0.7) is proceeded singly or as owner-approved grouped items
> (the G1–G5 grouping precedent). Phase 2.6+ still waits on Phase 2.0.

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
> **The next open entry point is 2.0.4 (API-key strategy)**, then 2.0.5 → 2.0.7 — singly or as
> owner-approved grouped items (the G1–G5 grouping precedent). The owner filled all five keys in
> `local.properties` (2026-09-25), so 2.0.4 now plans around real keys; note for its planning: the
> build does **not** currently load `local.properties` (`app/build.gradle.kts` reads
> `project.findProperty`, which sees neither that file nor `~/.gradle/gradle.properties`), so the
> wiring must be added there. Phase 2.6+ still waits on Phase 2.0.
>
> What the first run has already bought, beyond the two defects it found: the app is
> confirmed to build, install, launch, and render on a Pixel 6a, and one prediction
> (a stray ActionBar) was refuted rather than acted on — which is the whole argument for
> running the thing instead of reasoning about it.

---

## Phase 0 — Historical: the prompt-driven era (retired)

Tasks 1.1–2.5 were implemented by feeding `doc/prompts/01…15` to an agent, one file per
task, with no feature spec, no verification step and **no code review**. The code they
produced is real and mostly sound — but **every item below is `[~]`, because not one of
them was ever verified on a device and both quality gates they claimed to satisfy are
red.** The legacy CR gate (Phase 1.9, below) reviews them one by one; each row links to
its CR task file.

Evidence of the era's failure mode: `doc/04-IMPLEMENTATION-PLAN-PHASE1.md` ends with
"All Phase 1 unit tests pass" and "Detekt and ktlint pass with zero issues" — both
statements are false today, and there is no record they were ever true.

| Item | Deliverable | Evidence in tree | CR task | Status |
| --- | --- | --- | --- | --- |
| 1.1 Project scaffolding | Multi-module Gradle build, version catalog | `settings.gradle.kts` (20 modules), `gradle/libs.versions.toml` | [1.1](features/2026-09-21-legacy-code-review/tasks/1.1-project-scaffolding.md) | `[~]` |
| 1.2 Design system & theme | `core:core-ui` | 29 source files: `theme/` 6, `components/` 16, `animation/` 3, `util/` 2, `mapper/` + `preview/` 2 | [1.2](features/2026-09-21-legacy-code-review/tasks/1.2-design-system-theme.md) | `[~]` |
| 1.3 Security module | `core:core-security` | 10 source + 5 test files (KeyStore, Tink, BIP-39/44, biometric, PIN validation) — suite repaired by 2.0.1: **61 tests, 0 fail, 1 skipped** | [1.3](features/2026-09-21-legacy-code-review/tasks/1.3-security-module.md) | `[~]` |
| 1.4 DataStore & preferences | `core:core-datastore` | 8 source + 4 test files; 27 tests pass | [1.4](features/2026-09-21-legacy-code-review/tasks/1.4-datastore-preferences.md) | `[~]` |
| 1.5 Domain models & repository interfaces | `domain` | 32 source files (7 models, 5 repository interfaces, 20 use cases) + 10 test files; 63 tests pass | [1.5](features/2026-09-21-legacy-code-review/tasks/1.5-domain-models-repositories.md) | `[~]` |
| 1.6 Data layer | `data` | 11 source files: 5 repository impls + 5 mappers + `RepositoryModule`; 27 tests pass | [1.6](features/2026-09-21-legacy-code-review/tasks/1.6-data-layer.md) | `[~]` |
| 1.7 Onboarding (parts 1–2) | `feature:feature-onboarding` | 12 source + 5 test files; 30 tests pass | [1.7](features/2026-09-21-legacy-code-review/tasks/1.7-onboarding.md) | `[~]` |
| 1.8 Auth / unlock | `feature:feature-auth` | 3 source + 1 test file; 14 tests pass | [1.8](features/2026-09-21-legacy-code-review/tasks/1.8-auth-unlock.md) | `[~]` |
| 1.9 Main scaffold & navigation shell | `app` | `MainScreen` with 5 tabs — **4 render `PlaceholderTabScreen`** | [1.9](features/2026-09-21-legacy-code-review/tasks/1.9-main-scaffold-navigation.md) | `[~]` |
| 2.1 Network module | `core:core-network` | 19 files: CoinGecko + explorer APIs, Web3j provider, interceptors, adapters — **no tests** | [2.1](features/2026-09-21-legacy-code-review/tasks/2.1-network-module.md) | `[~]` |
| 2.2 Database module | `core:core-database` | 10 files: 4 entities, 4 DAOs, DB, DI; schema `1.json` — **no tests** | [2.2](features/2026-09-21-legacy-code-review/tasks/2.2-database-module.md) | `[~]` |
| 2.3 Chain management | Chain switching | 3 chain use cases, `ChainUiMapper`, `ChainSelectorDropdown`, `ChainBadge`, `ChainIconMapper` — **no tests** | [2.3](features/2026-09-21-legacy-code-review/tasks/2.3-chain-management.md) | `[~]` |
| 2.4 Home dashboard | `feature:feature-home` | 4 files: `HomeScreen`, `HomeViewModel`, `HomeUiState`, `AddTokenDialog` — **no tests** | [2.4](features/2026-09-21-legacy-code-review/tasks/2.4-home-dashboard.md) | `[~]` |
| 2.5 Token detail | `feature:feature-tokens` | 3 files: `TokenDetailScreen`, `TokenDetailViewModel`, `TokenDetailUiState` — **no tests** | [2.5](features/2026-09-21-legacy-code-review/tasks/2.5-token-detail.md) | `[~]` |

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

Order within the phase is strict: 2.0.1 → 2.0.2 → **2.0.2b** → 2.0.3 → 2.0.4 → 2.0.5 → 2.0.6 → 2.0.7.
2.0.2b was inserted on 2026-09-21: 2.0.2's first real run exposed a critical defect that
blocks every remaining step.

### 2.0.1 — Repair the `core-security` test suite `[~]`
**Automatic half DONE 2026-09-21** — 61 tests, 0 failures, 1 skipped; the suite compiles
and runs for the first time. Stays `[~]` until the owner reviews the nine
test-vs-implementation judgments recorded in
`specs/features/2026-09-20-2.0.0-stabilization/validation.md`.

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

### 2.0.2 — First run on a device `[~]`
**IN PROGRESS 2026-09-21.** Device: **Pixel 6a, Android 16 (API 36)**, adb over Wi-Fi.
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

Not done: the rest of the happy path. One tap past Welcome it hits **2.0.2b**.

Exit criteria: the main happy path has been observed on a device, with screenshots or a
written trace, and any crash is filed as its own roadmap item.

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

### 2.0.4 — Decide the API-key strategy `[ ]`
All five `local.properties` keys are `your_key_here`. Determine, per screen, what the
correct behaviour without keys is (explicit "not configured" state vs. empty state vs.
retry), and make the network-backed screens honour it. Faking data to make a screen look
populated is forbidden.

**Owner decision required:** supply real keys, or keep placeholders and treat
key-absent degradation as the supported path for now. Either answer is acceptable; it
must be written down.

### 2.0.5 — Turn both quality gates green `[ ]`
Current: **1656 ktlint violations across 178 of 221 Kotlin files**, and **75 detekt
issues** against `maxIssues: 0`.

The ktlint number is a style-vocabulary conflict, not 1656 independent mistakes: the
code follows IntelliJ defaults while ktlint 1.x defaults disagree
(`multiline-expression-wrapping` 468, `function-signature` 306,
`argument-list-wrapping` 150, `trailing-comma-on-call-site` 113,
`function-expression-body` 106, `annotation` 92, `function-naming` 76).

**Two owner decisions required:**

1. *ktlint* — (a) run `ktlintFormat` once and accept a reformat of ~178 files as an
   isolated, no-logic-change commit, or (b) add an `.editorconfig` plus rule
   configuration that codifies the style the code already has. (a) is the conventional
   answer; (b) preserves history and is faster but leaves the codebase on a
   non-standard dialect.
2. *detekt* — (a) refactor the offenders (`TokenRepositoryImpl` 19/11 functions,
   `WalletRepositoryImpl` 14/11, `AuthRepositoryImpl` 13/11, plus `MaxLineLength`), or
   (b) raise the thresholds, or (c) introduce a baseline file. A baseline hides the debt
   rather than clearing it and is the weakest option.

Whichever options are chosen, formatting lands as its **own change**, separate from any
logic edit, and must land **before** Phase 2 feature work resumes.

### 2.0.6 — Build the TC traceability `[ ]`
`doc/07-TEST-CASES.md` specifies 52 cases (TC-SEC 8, TC-SECTEST 5, TC-UC 4, TC-REPO 5,
TC-NET 3, TC-DB 3, TC-VM 10, TC-UI 10, TC-INT 4). No test method references a TC id, so
nobody can tell which specified behaviours are actually covered.

Exit criteria: each TC id is mapped to a test method, or registered as an unimplemented
roadmap item. The 10 TC-UI cases have no home at all today — the only instrumented test
is the `ExampleInstrumentedTest` template stub.

### 2.0.7 — Record and close `[ ]`
Write `specs/features/2026-09-20-2.0.0-stabilization/validation.md` covering 2.0.1–2.0.6
with commands, observed results, and absolute artifact paths; then flip the Phase 0 rows
that earn it to `[x]`, and carry forward anything still deferred as a Handoff list.

---

## Phase 2 — Core features (remaining)

Unblocked only after Phase 2.0 closes.

| Item | Scope | Landing module | Acceptance | Status |
| --- | --- | --- | --- | --- |
| 2.6 Send transaction flow | Form → review (gas slow/normal/fast) → submit → result, native + ERC-20 | `feature:feature-send` + `TransactionRepositoryImpl` | `AC-2.6` | `[ ]` |
| 2.7 Receive screen | QR (ZXing) + copy + share, chain badge | `feature:feature-receive` | `AC-2.7` | `[ ]` |
| 2.8 Transaction history | Date-grouped list, filters, pagination, detail screen | `feature:feature-history` | `AC-2.8` | `[ ]` |
| 2.9 Default token list | Seed tokens on wallet creation / chain switch | `data` + `core:core-database` | Phase 2 checklist | `[~]` — implemented by the legacy CR 1.5 fix round (creation paths + `RefreshBalancesUseCase` chain-switch seeding); verification per the Phase 2 flow |

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
| 4.15 | Decide whether `targetSdk 37` runtime behaviour needs verification before 3.7; the only available device is API 36. |
| 4.16 | Migrate `hiltViewModel` to `androidx.hilt.lifecycle.viewmodel.compose` — the old `androidx.hilt.navigation.compose` entry point is deprecated. Surfaced by the first build on the remote host; handle alongside the next `hilt-navigation-compose` bump. |
| 4.17 | Adopt design tokens, string resources and component previews across feature modules and `core-ui` (legacy CR findings 1.2-1 … 1.2-4; owner-approved 2026-09-21). |

---

## Evidence appendix — baseline measured 2026-09-20, updated 2026-09-21

Commands run from the repository root; `JAVA_HOME` set to the Android Studio JBR.

| # | Command | Result |
| --- | --- | --- |
| E1 | `./gradlew :app:assembleDebug` | **PASS** → `/Users/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk` |
| E2 | `./gradlew testDebugUnitTest --continue` (2026-09-21, after 2.0.1) | **223 tests, 222 pass, 1 skipped, 0 fail** — `domain` 63, `core-security` 61 (1 skipped), `feature-onboarding` 30, `core-datastore` 27, `data` 27, `feature-auth` 14, `app` 1 |
| E2b | Same, pre-2.0.1 baseline | **162 pass** across 6 modules; `core-security`'s 64 tests did not compile at all |
| E3 | `./gradlew :core:core-security:compileDebugUnitTestKotlin` | **FAIL → FIXED 2026-09-21** — was 16 compile errors (Mockito, `passphrase`, `suggestWords`, `secureWipe`, `toHex`, `Int`/`Byte`) |
| E4 | `./gradlew detekt` | **FAIL** — 75 issues (`TooManyFunctions`, `MaxLineLength`), config `maxIssues: 0` |
| E5 | `./gradlew ktlintCheck` | **FAIL** — 1656 violations in 178 of 221 Kotlin files |
| E6 | `adb devices` / `ls $ANDROID_HOME/emulator` / `ls ~/.android/avd` | **empty / missing / empty** — no device, no emulator, no AVD |
| E7 | `grep -r TODO\|FIXME --include=*.kt app core domain data feature` | **0** — the `doc/08` "no TODO/FIXME" criterion passes |
| E8 | `find . -path "*src/androidTest*" -name "*.kt"` | **1 file** — the template stub only; no real instrumented coverage |

These numbers are the reference point for every claim in this roadmap. Re-run them before
asserting that anything has changed.
