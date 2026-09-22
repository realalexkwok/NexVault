# Phase 1.9 — Legacy code review — Validation record

> Evidence record. One section per CR task; each section is filled when that task is reviewed.
> Two halves: **automatic** = the agent scan (commands + observed results, `file:line` evidence);
> **manual** = the owner's per-task sign-off.
> Findings lifecycle: record → developer session fixes → reviewer verifies → finding row deleted
> (`requirements.md` Q6). Nothing is "done" without a command and its observed result.

## Gate status

| Task | Scan evidence | Owner sign-off | Status |
| --- | --- | --- | --- |
| 1.1 Project scaffolding | 2026-09-21 (A1–A8) + fix verification (V1–V8) | 2026-09-21 | `[x]` |
| 1.2 Design system & theme | 2026-09-21 (B1–B5) + fix verification (V1–V12) | 2026-09-21 | `[x]` |
| 1.3 Security module | 2026-09-21 (S1–S6) + fix verification (V1–V8) | 2026-09-22 | `[x]` |
| 1.4 DataStore & preferences | 2026-09-22 (D1–D5) | — | `[?]` |
| 1.5 Domain models & repository interfaces | — | — | `[ ]` |
| 1.6 Data layer | — | — | `[ ]` |
| 1.7 Onboarding | — | — | `[ ]` |
| 1.8 Auth / unlock | — | — | `[ ]` |
| 1.9 Main scaffold & navigation shell | — | — | `[ ]` |
| 2.1 Network module | — | — | `[ ]` |
| 2.2 Database module | — | — | `[ ]` |
| 2.3 Chain management | — | — | `[ ]` |
| 2.4 Home dashboard | — | — | `[ ]` |
| 2.5 Token detail | — | — | `[ ]` |

---

## Task 1.1 — Project scaffolding

> Status: **CLOSED 2026-09-21** — scan + fix verification complete (A1–A8, V1–V8); findings
> 1.1-1 and 1.1-2 fixed and verified (rows deleted); finding 1.1-3 recorded and handed to the
> developer session's next round. Owner sign-off below.

### Automatic half — scan (commands and observed results)

All commands from the repository root with `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
(AGENTS.md's macOS JBR path does not exist on this machine; JDK 17 is the project's own JVM target).

| # | Check | Command | Result |
| --- | --- | --- | --- |
| A1 | Build | `./gradlew :app:assembleDebug --console=plain` | **UP-TO-DATE (pass)** — artifact `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk` (45,024,167 bytes, 2026-09-21 09:04). Full clean-build PASS is accepted evidence E1. |
| A2 | detekt | `./gradlew detekt --continue` | **FAIL — 10 tasks, 91 weighted issues**: app 3 · data 29 · domain 9 · core-database 4 · core-datastore 8 · core-security 11 · core-ui 5 · feature-auth 9 · feature-home 2 · feature-onboarding 11. Owned by 2.0.5. |
| A3 | ktlint | `./gradlew ktlintCheck --continue` | **FAIL — 39 tasks**. Samples: `:app:ktlintAndroidTestSourceSetCheck` (import order, wildcard import, missing trailing newline in `ExampleInstrumentedTest.kt`); `:app:ktlintTestSourceSetCheck`. Owned by 2.0.5. |
| A4 | TODO/FIXME | `grep -rnE 'TODO|FIXME' --include='*.kt' app core data domain feature` | **0** (template `<!-- TODO -->` comments existed in the two backup-rule XMLs — removed by the 1.1-1 fix, see V1). |
| A5 | Hardcoded versions | `grep -nE '"[0-9]+\.[0-9]+' --include='build.gradle.kts' -r app core data domain feature` | Only a commented-out Netty block (`app/build.gradle.kts:7–13`) and `versionName = "1.0"` (`:30`) — no hardcoded dependency versions. |
| A6 | Module graph | read `settings.gradle.kts` + 9 module build files | 20 modules declared, matching tech-stack §2; `data` → five `core:*` (no core-ui) — wording drift was finding 1.1-2 (fixed, V4). |
| A7 | Entry point | read `app/src/main/AndroidManifest.xml` | `.MainActivity` MAIN/LAUNCHER + `.NexVaultApplication` — 2.0.2 fix verified in tree. |
| A8 | Backup posture | read manifest + `res/xml/backup_rules.xml` + `res/xml/data_extraction_rules.xml` | `allowBackup="true"` + stock template rules → was **finding 1.1-1 (High)** — fixed, V1–V3. |

### Fix verification — developer handoff re-checked by the reviewer (2026-09-21)

Per the role split, the reviewer did **not** trust the developer's claims; every one was re-verified:

| # | Claim | Reviewer's command | Observed result | Verdict |
| --- | --- | --- | --- | --- |
| V1 | `allowBackup=false` + excludes in both rule files | read `AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml` | `android:allowBackup="false"` (line 7); `full-backup-content` excludes all 9 domains (root, file, database, sharedpref, external, device_root, device_file, device_database, device_sharedpref) with path `.`; `cloud-backup` **and** `device-transfer` each exclude the same 9; template TODO comments replaced by real rationale comments | ✅ matches claim |
| V2 | APK carries the new manifest | `aapt2 dump xmltree app/build/outputs/apk/debug/app-debug.apk --file AndroidManifest.xml` (build-tools 37.0.0) | `android:allowBackup(...)=false`, `fullBackupContent` and `dataExtractionRules` attributes present | ✅ |
| V3 | Device: ALLOW_BACKUP flag gone | `adb shell dumpsys package com.nexvault.wallet.debug` | `flags=[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ]` — **no ALLOW_BACKUP** | ✅ |
| V4 | tech-stack §2 wording fixed | read `specs/tech-stack.md:65` | "`data` | `domain` + five of the six `core:*` modules — … `core-ui` is deliberately **not** a dependency (2.3 AC 25; verified in `data/build.gradle.kts`)." | ✅ finding 1.1-2 verified-fixed |
| V5 | Build + unit regression | `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --rerun-tasks` | **PASS** — fresh run; unit suites summed from 29 result XMLs: **223 tests, 0 failures, 0 errors, 1 skipped** (claim was 223 pass / 1 skipped / 0 fail) | ✅ matches claim |
| V6 | Device tests | `./gradlew :app:connectedDebugAndroidTest --rerun-tasks` on Pixel 6a (adb-tls) | `BackupPolicyTest` **3/3 pass** (claim verified); `ExampleInstrumentedTest` **1/1 FAIL** — `expected:<com.nexvault.wallet[]> but was:<com.nexvault.wallet[.debug]>` at `ExampleInstrumentedTest.kt:22` → **finding 1.1-3 recorded** | ✅ backup test verified; 1.1-3 confirmed |
| V7 | `bmgr backupnow` | `adb shell bmgr backupnow com.nexvault.wallet.debug` | `Unable to run backup` · `Backup finished with result: Backup is not allowed` | ✅ matches claim |
| V8 | App launches / Welcome renders | not re-run by the reviewer (launch already evidenced by 2.0.2 on the same device; the 1.1-1 change touches only backup posture) | accepted as owner-relayed | ✅ (manual half) |

**Reviewer note (not a finding):** the `device_*` domains inside `<cloud-backup>` are belt-and-suspenders —
cloud backup is governed by the five standard domains plus `allowBackup=false`; the extra entries are
harmless if ignored, and the new `BackupPolicyTest` locks the intended posture in.

### Findings lifecycle record

| ID | Severity | State |
| --- | --- | --- |
| 1.1-1 (High, backup posture) | fixed by developer; **verified by reviewer (V1–V3, V5–V8) → row deleted** | Closed |
| 1.1-2 (Low, tech-stack §2 wording) | fixed by developer; **verified by reviewer (V4) → row deleted** | Closed |
| 1.1-3 (Low, `ExampleInstrumentedTest` package assertion) | **Open** — recorded in the task file for the developer session's next fix round | Open |

### Manual half

Owner sign-off: **DONE 2026-09-21** — the reviewer closed the item per the owner's branch sign-off
instruction (fixes verified by the reviewer V1–V8 and confirmed by the user). Open finding 1.1-3
stays in the task file's findings table as the transfer channel for the developer session.

---

## Task 1.2 — Design system & theme

> Status: **SCAN + FIX VERIFICATION COMPLETE 2026-09-21 — 1.2-1 … 1.2-4 verified-fixed (rows deleted);
> 3 open findings (1.2-5, 1.2-6, 1.2-7) — ready to close on user confirmation.**

### Automatic half — commands and observed results

| # | Check | Command | Result |
| --- | --- | --- | --- |
| B1 | Build | `./gradlew :core:core-ui:assembleDebug --console=plain` | **BUILD SUCCESSFUL** (2026-09-21) |
| B2 | detekt (module) | `./gradlew :core:core-ui:detekt` | **FAIL — 5 weighted issues** (pre-registered 2.0.5, not re-created) |
| B3 | Previews | `grep -rn '@Preview' core/core-ui` | **0** → finding 1.2-2 |
| B4 | Hardcoded values in features | `grep -rnE '[0-9]+\\.(dp|sp)' feature` · `grep -rnE 'Color\\(0x' feature app` · `grep -rnE 'Text\\(\"[A-Za-z]' feature` | 149 dim literals · 1 color (`TokenDetailScreen.kt:333`) · 10 string hits; no `feature/*/res/values` strings → finding 1.2-1 |
| B5 | Component token discipline | `grep -rn 'Color(0x' core/core-ui/src/main/.../components` | only `TransactionRow.kt:62,64` → finding 1.2-3; `NexVaultButton` spot-read token-compliant |

### Fix verification — developer handoff re-checked by the reviewer (2026-09-21)

Every claim was re-verified from the tree; none was taken on trust.

| # | Claim | Reviewer's check | Observed result | Verdict |
| --- | --- | --- | --- | --- |
| V1 | Strings moved out of features (Fixed A) | `grep -rnE 'Text\\(\"' feature` | **0** (was 10) — per-feature `strings.xml` exist: tokens 16 keys, onboarding 48, home 24, auth 11 + 1 plurals; `errorRes`/`errorArgs`/`errorMessage` mechanism across auth/home/onboarding/tokens VMs + UiStates + screens (`errorMessage ?: stringResource(errorRes, *errorArgs)`) | ✅ substance verified; the "19 new keys" count is inaccurate — **99** feature keys exist |
| V2 | `BiometricHelper` takes `@StringRes` (Fixed B) | read `BiometricHelper.kt:40–58` + `UnlockScreen.kt:68–78` | `createPromptInfo(@StringRes title/subtitle/negativeButtonText: Int)` with `R.string` defaults; `core-security/res/values/strings.xml` has 3 keys; `UnlockScreen` passes its own ids | ✅ |
| V3 | Core-ui dims → tokens (Fixed C) | `grep -rnE '[0-9]+\\.(dp|sp)' core/core-ui/src/main` minus `Dimens.kt`/`Type.kt` | only `@Preview` blocks (ShimmerPlaceholder, SimpleLineChart) and the Typography scale remain — production components: **0**. `Dimens.kt` gained **12** new tokens (claim said 6) | ✅ |
| V4 | Feature dims/colors/contentDescriptions | fresh greps on `feature/` | dims **0** (was 149), `Color(0x` **0** (was 1), literal contentDescription **0** — fixed beyond the handoff's stated scope | ✅ |
| V5 | Previews (1.2-2) | `grep -rn '@Preview' core/core-ui` | **0 → 17**; every component file covered (`ChainUi.kt` is a data class). Strict per-composable previews remain a 4.17 nicety (reviewer note) | ✅ |
| V6 | TransactionRow colors (1.2-3) | read `TransactionRow.kt` | `NexVaultTheme.colors.info` / `.positive` — hardcoded colors gone; `info` token added to `NexVaultColors`; SimpleLineChart preview `lineColor = NexVaultTheme.colors.positive` | ✅ |
| V7 | Fonts (1.2-4 — not even listed in the handoff) | read `Type.kt` + `res/font/` | `NexVaultFontFamily = FontFamily(Font(R.font.inter_regular…))` with `inter_{regular,medium,semibold,bold}.ttf` bundled + OFL licence note | ✅ fixed |
| V8 | Build | `./gradlew :app:assembleDebug --rerun-tasks` | **PASS** (fresh, 624 tasks) — APK `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk` **45,904,999 B, 21:05**. The handoff's 46,978,341 B (20:57) is not reproduced by the fresh run; build correctness is what counts | ✅ |
| V9 | Tests | `testDebugUnitTest :domain:test --rerun-tasks`, sums from 29 XMLs | **223 / 0 failures / 0 errors / 1 skipped** — matches the claim | ✅ |
| V10 | Gates | `detekt ktlintCheck --rerun-tasks` | detekt: 10 failing tasks, **87 weighted issues** (was 91 — improved by 4); ktlint: 39 failing tasks, ≈1390 violation lines in the log (was ≈1553 — improved). The handoff's "72 vs 75" / "444→438" use different counting bases than the reviewer's 91→87 weighted / 1553→1390; direction (improvement) confirmed on the reviewer's own basis | ✅ direction confirmed |
| V11 | Boundaries | `git status` | 49 changed files, all under `app|core|feature|data|domain` — **nothing under `specs/` or `doc/`**; on branch `cr/1.2-design-system-theme`, uncommitted | ✅ |
| V12 | New candidates | reads of the named lines | 1.2-6 confirmed (`ImportWalletScreen.kt:276,342`, `UnlockScreen.kt:255`); **"N monogram" refuted** (`TokenIcon.kt:47` is data-driven); 1.2-7 confirmed (`SetPinViewModel.kt:189–193` dangling "Failed to set PIN: "); **"Verification error: " not reproduced** (no such string in the tree) | ✅ recorded as 1.2-6 / 1.2-7 with the refutations |

**Reviewer notes (reporting inaccuracies, substance unaffected):** "19 new keys" is actually 99 feature
string keys (+1 plurals, +3 core-security); "3 test files" is 4 (`UnlockViewModelTest`,
`CreateWalletViewModelTest`, `ImportWalletViewModelTest`, `SetPinViewModelTest`); "6 new dim
tokens" is 12; APK size and gate counts use different measurement bases than the reviewer's fresh run.

### Findings lifecycle record

| ID | Severity | State |
| --- | --- | --- |
| 1.2-1 (Medium, hardcoded values in features) | fixed + verified (V1, V4) → **row deleted**; residual interpolated strings re-recorded as 1.2-6 | Closed |
| 1.2-2 (Low, zero previews) | fixed + verified (V5) → **row deleted** | Closed |
| 1.2-3 (Low, TransactionRow colors) | fixed + verified (V6) → **row deleted** | Closed |
| 1.2-4 (Low, fonts) | fixed + verified (V7) → **row deleted** | Closed |
| 1.2-5 (Medium, no tests) | untouched — **Open** (2.0.6) | Open |
| 1.2-6 (Low, 3 interpolated strings) | **Open** (4.17) | Open |
| 1.2-7 (Low, dangling "Failed to set PIN: ") | **Open** (4.17) | Open |

### Manual half

Sign-off: **DONE 2026-09-21** — user-confirmed close per Q9 (fixes verified by the reviewer V1–V12).
Open findings 1.2-5 (2.0.6), 1.2-6 and 1.2-7 (4.17) remain in the task file's findings table as the
transfer channel for their owning items.

## Task 1.3 — Security module

> Status: **SCAN + FIX VERIFICATION COMPLETE 2026-09-22 — all 5 findings verified-fixed (rows deleted)
> — ready to close on user confirmation.**

### Automatic half — commands and observed results

| # | Check | Command | Result |
| --- | --- | --- | --- |
| S1 | Suite + build | `./gradlew :core:core-security:testDebugUnitTest :core:core-security:assembleDebug --rerun-tasks` | **BUILD SUCCESSFUL** — fresh sums: **61 tests, 0 failures, 0 errors, 1 skipped** |
| S2 | Logging | `grep -rnE 'Log\\.|println|printStackTrace' core/core-security/src/main` | **0** |
| S3 | Wipe discipline | `grep -rn 'secureWipe' core/core-security/src/main` + full reads | present in EncryptionManager (5 sites) + HDKeyManager; **absent** in WalletStore plaintext byte arrays → 1.3-3; `clearPassword` absent in SecurityUtils hash path → 1.3-2 |
| S4 | Plaintext storage | read `WalletStore.kt` | mnemonic double-encrypted, private keys password-encrypted, biometric password under biometric key — no plaintext writes ✓ |
| S5 | Checksum algorithm | read `SecurityUtils.kt:84,152–155` + `SecurityUtilsTest.kt:87–100` | NIST SHA3-256 ≠ Ethereum Keccak-256 → **1.3-1**; test asserts only format, cannot catch it |
| S6 | Test-vector strength | read `HDKeyManagerTest.kt:14–26` | canonical mnemonic vector but format-only assertion → **1.3-5**; also `KeyStoreManager.isStrongBoxAvailable` orphan key → **1.3-4** |
| — | Warnings | fresh run log | no compiler warnings (C11 ✓) |

### Fix verification — developer commit `eb00e49` re-checked by the reviewer (2026-09-22)

| # | Claim | Reviewer's check | Observed result | Verdict |
| --- | --- | --- | --- | --- |
| V1 | 1.3-1 Keccak-256 + EIP-55 | read `SecurityUtils.kt:78–100` + `SecurityUtilsTest.kt:87–113` | `Hash.sha3` (web3j Keccak-256) replaces `MessageDigest` SHA3-256; per-char nibble `(i % 2 == 0 → high else low)`, uppercase iff `>= 8`; `MessageDigest`/SHA3 code fully removed; tests assert the 4 official EIP-55 spec vectors + a web3j `Keys.toChecksumAddress` cross-check — the vectors would fail under NIST SHA3-256, so this is real regression protection | ✅ |
| V2 | 1.3-2 clearPassword | read `SecurityUtils.kt` hash/verify paths | `spec.clearPassword()` in `finally` in both `hashPassword` and `verifyPassword` (matching EncryptionManager) | ✅ |
| V3 | 1.3-3 WalletStore wipes | read `WalletStore.kt` | `mnemonicBytes.secureWipe()` / `passwordBytes.secureWipe()` in `finally` after encryption in `storeMnemonic` / `storeBiometricEncryptedPassword` | ✅ |
| V4 | 1.3-4 StrongBox probe | read `KeyStoreManager.kt:102–140` | API < 28 → false before any `setIsStrongBoxBacked` (NoSuchMethodError avoided); `FEATURE_STRONGBOX_KEYSTORE` short-circuit; probe alias deleted in `finally` with cleanup failure swallowed | ✅ |
| V5 | 1.3-5 exact vector | read `HDKeyManagerTest.kt` | `assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", address)` — the canonical BIP-39/BIP-44 vector | ✅ |
| V6 | Fresh suite + build | `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --rerun-tasks` | **PASS** — repo **224 tests, 0 failures, 0 errors, 1 skipped** (was 223; +1 new checksum cross-check test); core-security **62 / 0 / 1** | ✅ matches claim |
| V7 | Gates | `detekt ktlintCheck --rerun-tasks` | detekt 10 failing tasks / **87 weighted** (unchanged from reviewer baseline — no regression; the developer's "72 vs 75" uses a different counting basis); ktlint 39 failing tasks (unchanged) | ✅ no regression |
| V8 | Device (per owner) | `:app:connectedDebugAndroidTest` on Pixel 6a | `BackupPolicyTest` **3/3 pass**; `ExampleInstrumentedTest` 1 fail = **known open finding 1.1-3**, not a regression. Note: the first device attempt aborted (0 tests) because adb listed the same Pixel 6a twice (`10.42.0.139:45187` + adb-tls); the stale transport was disconnected and the run re-executed cleanly | ✅ |

**Algorithm decision recorded (1.3-1):** the developer chose **fix, not delete** — rationale:
"send/receive address display (2.6/2.7) needs correct checksums". Accepted and noted here so 2.6/2.7
planning knows `checksumAddress` is now EIP-55-correct and available for display use.

### Findings lifecycle record

| ID | Severity | State |
| --- | --- | --- |
| 1.3-1 | Medium | fixed + verified (V1, V6) → **row deleted** (decision recorded above) |
| 1.3-2 | Low | fixed + verified (V2) → **row deleted** |
| 1.3-3 | Low | fixed + verified (V3) → **row deleted** |
| 1.3-4 | Low | fixed + verified (V4) → **row deleted** |
| 1.3-5 | Medium | fixed + verified (V5) → **row deleted** |

### Manual half

Sign-off: **DONE 2026-09-22** — user-confirmed close per Q9 (fixes verified by the reviewer V1–V8,
device run included per owner).

## Task 1.4 — DataStore & preferences

> Status: **SCAN + FIX VERIFICATION COMPLETE 2026-09-22 — 1.4-1 … 1.4-3 verified-fixed (rows deleted);
> 1 open finding (1.4-4) — ready to close on user confirmation.**

### Automatic half — commands and observed results

| # | Check | Command | Result |
| --- | --- | --- | --- |
| D1 | Suite + build | `./gradlew :core:core-datastore:testDebugUnitTest :core:core-datastore:assembleDebug --rerun-tasks` | **BUILD SUCCESSFUL — 27 tests, 0 failures, 0 skipped** |
| D2 | Logging | `grep -rnE 'Log\\.|println' core/core-datastore/src/main` | **0** |
| D3 | Lockout terminal state | read `AppStateManager.kt` + `UnlockViewModel.kt:215–225` + usage greps | `WalletWiped` wipes nothing; VM navigates to onboarding with `is_wallet_set_up` true → **1.4-1**; thresholds 5/8/10/15/20 unit-tested |
| D4 | Display info | read `AppStateManager.kt:88–99` + `WalletModels.kt` | hardcoded `address = ""` / `"Account 1"` despite `AccountMetadata.address`; no production consumer → **1.4-2** |
| D5 | Chain persistence | read `PreferenceModels.kt` + `ChainMapper.kt:32–37` | `NetworkType` lacks BSC/POLYGON (has GOERLI); BSC/Polygon collapse to MAINNET on persistence → **1.4-3** (AC-2.1) |

### Fix verification — developer commit `4b20427` re-checked by the reviewer (2026-09-22)

| # | Claim | Reviewer's check | Observed result | Verdict |
| --- | --- | --- | --- | --- |
| V1 | 1.4-1 wipe at 20 | read `AppStateManager.kt` + `AppStateManagerTest.kt:105–125` | ≥20 → `wipeWallet()` (`walletStore.wipeAll()` + `keyStoreManager.deleteAllKeys()` + `resetApp()` clearing all three stores incl. failed-attempt counter and `is_wallet_set_up`) before `WalletWiped`; test asserts **0 wipes at 19** and full wipe at 20; cumulative counter kept per decision | ✅ matches owner decision |
| V2 | 1.4-2 display info | read `AppStateManager.kt` | `WalletDisplayInfo` + `currentWalletDisplayInfo` **deleted** (the allowed delete option); no remaining consumers | ✅ |
| V3 | 1.4-3 NetworkType | read `PreferenceModels.kt` + `ChainMapper.kt` + `ChainMapperTest.kt` | enum = MAINNET/SEPOLIA/BSC/POLYGON; both mapper directions round-trip all four chains; unknown → MAINNET; legacy GOERLI/CUSTOM stored values degrade via the existing `valueOf` catch; no stray refs (only the KDoc mention) | ✅ |
| V4 | Fresh suite + build | `./gradlew :app:assembleDebug testDebugUnitTest :domain:test --rerun-tasks` | **PASS** — repo **227 tests, 0 failures, 1 skipped** (matches claim; 224 + 3 new ChainMapperTest cases); datastore module 30/0/0 | ✅ |
| V5 | Gates | `detekt ktlintCheck --rerun-tasks` | detekt 10 failing tasks / **85 weighted** (reviewer baseline 87 → improved by 2; developer's "71 vs 75" is a different basis); ktlint 39 tasks unchanged | ✅ no regression |
| V6 | Device (clean env per owner) | fresh install + launch + `connectedDebugAndroidTest` on Pixel 6a | `BackupPolicyTest` **3/3 pass**; `ExampleInstrumentedTest` fail = known **1.1-3**; `installDebug` → `am start` → process alive (pid 24501), **0 FATAL**, `MainActivity` topResumed | ✅ |
| V7 | Adb environment | `adb devices -l` | the Pixel 6a appeared **twice** again (fresh `10.42.0.139:41999` endpoint + adb-tls); the stale transport was disconnected before the clean runs — noted for future device runs | ✅ handled |

**New finding recorded during verification:** 1.4-4 (Low, 4.17) — the 15-attempt warning message at
`AppStateManager.kt:107` is a hardcoded English user-facing string in `core-datastore` (the 1.2
sweep only covered `feature/`).

### Findings lifecycle record

| ID | Severity | State |
| --- | --- | --- |
| 1.4-1 (High, wipe at 20) | fixed + verified (V1, V4) → **row deleted** (owner decision recorded) | Closed |
| 1.4-2 (Low, display info) | fixed + verified (V2) → **row deleted** | Closed |
| 1.4-3 (High, NetworkType) | fixed + verified (V3, V4) → **row deleted** | Closed |
| 1.4-4 (Low, hardcoded warning string) | **Open** (4.17) | Open |

### Manual half

Sign-off: **pending** — verification complete; ready to close on user confirmation (date: —).

## Task 1.5 — Domain models & repository interfaces

_(filled during the review)_

## Task 1.6 — Data layer

_(filled during the review)_

## Task 1.7 — Onboarding

_(filled during the review)_

## Task 1.8 — Auth / unlock

_(filled during the review)_

## Task 1.9 — Main scaffold & navigation shell

_(filled during the review)_

## Task 2.1 — Network module

_(filled during the review)_

## Task 2.2 — Database module

_(filled during the review)_

## Task 2.3 — Chain management

_(filled during the review)_

## Task 2.4 — Home dashboard

_(filled during the review)_

## Task 2.5 — Token detail

_(filled during the review)_
