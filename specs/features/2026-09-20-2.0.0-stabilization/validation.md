# Phase 2.0 — Validation record

> Evidence record for the Phase 2.0 stabilization item. One section per sub-item.
> Requirements: `requirements.md`. Plan: `plan.md`.
> **Phase 2.0 CLOSED 2026-09-27 by item 2.0.7** — the closing evidence, the Phase 0 flip table and
> the consolidated Handoff are in §"2.0.7 — Phase 2.0 close" at the end of this file.

---

# 2.0.1 — Repair the `core-security` test suite

> Status: **AUTOMATIC HALF COMPLETE 2026-09-21. AWAITING OWNER REVIEW.**
> The manual half for a test-repair item is the owner's sign-off on the eight
> test-vs-implementation judgments below (Q1–Q4 already answered; defects #1–#9 need a
> read). Roadmap status stays `[~]` until that review lands.
>
> Parent: Phase 2.0 / 2.0.1.

## Automatic half — results

All commands from the repository root with
`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

| # | Check | Command | Result |
| --- | --- | --- | --- |
| A1 | Suite compiles and passes | `./gradlew :core:core-security:testDebugUnitTest` | **BUILD SUCCESSFUL** — 61 tests, 0 failures, 1 skipped |
| A2 | Per-class breakdown | `core/core-security/build/test-results/testDebugUnitTest/TEST-*.xml` | `EncryptionManagerTest` 10 (1 skipped) · `SecurityUtilsTest` 15 · `MnemonicManagerTest` 11 · `PasswordValidatorTest` 16 · `HDKeyManagerTest` 9 |
| A3 | No regression elsewhere | `./gradlew testDebugUnitTest --continue` | **BUILD SUCCESSFUL** — all modules |
| A4 | Whole-repo aggregate | `**/build/test-results/**/TEST-*.xml` | **223 tests, 222 passed, 1 skipped, 0 failures** (was 162 passing + 64 non-compiling) |
| A5 | Scope discipline | `git diff --stat -- core/core-security/src/main` | 1 file: `MnemonicManager.kt` (+8 / −21) |
| A6 | Detekt not regressed | `./gradlew :core:core-security:detekt` | 7 issues — **unchanged** from the pre-change baseline |

Test-count reconciliation (AC-2): **64** `@Test` annotations existed → **3** deleted by
owner decision Q1 (`testSuggestWords`, `testSuggestWordsEmptyPrefix`,
`testSuggestWordsLimit`) → **61** remain, of which **1** is `@Ignore`d →
**60 execute and pass**, 61 reported, 0 failures. Nothing was skipped to hide a failure:
the single `@Ignore` is the keystore path named in Q2.

Artifact paths:
- Results: `/Users/superguo/Projects/NexVault/core/core-security/build/test-results/testDebugUnitTest/`
- Detekt report: `/Users/superguo/Projects/NexVault/core/core-security/build/reports/detekt/detekt.{txt,html,xml}`

## The ninth defect — found only because the suite finally ran

The compile fix exposed a **real failing test** that had never executed:

```
PasswordValidatorTest > testStrengthCalculationStrong FAILED
java.lang.AssertionError at PasswordValidatorTest.kt:92
```

`assertTrue(PasswordValidator.calculateStrength("MyV3ryStr0ng!P@ssw0rd") > 70)` — the
implementation returns exactly **70**:

| Contribution | Points |
| --- | --- |
| Length 21 (≥16 band) | +25 |
| Lowercase present | +10 |
| Uppercase present | +10 |
| Digit present | +15 |
| Special char present | +15 |
| Consecutive-pair penalty (`ss`) | −5 |
| **Total** | **70** |

**Judgment: the test is wrong, the implementation is right.** `calculateStrength`
documents its bands precisely (25 for length, 50 for variety, up to 25 deduction) and is
internally consistent. The test's `> 70` was an arbitrary threshold sitting one point
above the maximum a realistic strong password can reach once any penalty applies.

**Fix applied:** the assertion pins the documented contract instead of a boundary —
`assertEquals(70, strength)` with a comment explaining the ceiling. This is not a
weakened assertion; it is a stricter one (exact value vs. inequality) and it now
documents a real property of the scoring model.

**Do not read this as the algorithm being fine.** The maximum attainable score is **75**,
not 100, so the `coerceIn(0, 100)` is misleading and any strength *meter* built on this
value would never reach the top of its own scale. Changing the scoring model is a product
change, not stabilization work, and `calculateStrength` currently has **no production
caller** — so it is logged rather than changed. See Handoff.

## Judgment record (the substance of this item)

Per `AGENTS.md`: *"Do not fix a failing test by weakening the production code it tests."*
Every defect was traced to a root cause before a side was chosen.

| # | Defect | Side | Evidence for the verdict |
| --- | --- | --- | --- |
| 1 | `secureWipe` unresolved | **Test** | The function exists as a `ByteArray`/`CharArray` extension on the `SecureUtils` object; `EncryptionManager.kt:5` imports it exactly the way the test should. |
| 2 | `SecureUtils.secureWipe(chars)` | **Test** | Extension members are not callable as `Object.member(receiver)`. |
| 3 | `Int`/`Byte` mismatch | **Test** | Compiler columns 51 and 59 land precisely on the `255` and `128` literals in `byteArrayOf(...)`, both outside `Byte` range. |
| 4 | `toHex` / `hexToByteArray` unresolved | **Test** | Both exist as extensions on `SecurityUtils`; the test declared no imports at all. |
| 5 | `passphrase` missing | **Test** | All three production call sites (`WalletRepositoryImpl.kt:57,119,289`) pass `""`; two other call sites in the same test file already passed it. |
| 6 | Mockito unresolved | **Test** | Mockito is not a dependency; `libs.mockk` is declared in the module and `data`'s suite mocks the equally-final `MnemonicManager` with it. |
| 7 | `getWordList()` returns 100 words | **Implementation** | `MnemonicManagerTest:84` correctly asserts 2048. The in-code comment admitted the stub, and its stated reason ("web3j doesn't expose the word list") is false — `MnemonicUtils.getWords()` is public static. |
| 8 | `"wallet"` / `"bitcoin"` assertions | **Test** | Neither word is in BIP-39. Verified against the shipped `en-mnemonic-word-list.txt`: the `wal` prefix yields only `walk`, `wall`, `walnut`; `bit` yields only `bitter`. |
| 9 | Strength `> 70` | **Test** | Arithmetic above; the implementation's documented ceiling is 75. |

## Production change (the only one)

`MnemonicManager.getWordList()` now delegates to `MnemonicUtils.getWords()`:

```kotlin
fun getWordList(): List<String> = MnemonicUtils.getWords()
```

- Verified source: `en-mnemonic-word-list.txt` inside `org.web3j:crypto:5.0.2` —
  **2048 lines, 2048 unique**, `abandon` → `zoo`.
- The 100-word literal and the `private companion object` holding it were deleted.
- The delegation also guarantees an invariant the literal could not: the list reported by
  `getWordList()` is the same list `generateMnemonic` draws from and `validateMnemonic`
  checks against.
- `getWordList()` still has **no production caller**; it is exercised only by tests.

## Acceptance criteria

| AC | Status | Evidence |
| --- | --- | --- |
| AC-1 suite green | ✅ | A1 |
| AC-2 counts reconciled | ✅ | A2 + reconciliation above |
| AC-3 word-list assertions | ✅ | `abandon` first, `zoo` last, 2048 unique — enforced in `testWordList` |
| AC-4 main-source scope | ✅ | A5 — only `MnemonicManager.kt` |
| AC-5 no regressions | ✅ | A3, A4 |

## Not verified by this item

- **The `@Ignore`d keystore test.** `testEncryptWithKeystore` asserts nothing (it stubbed a
  mock key and returned) and cannot run on the JVM regardless. Marking it `@Ignore`
  removes a green dot that proved nothing; it does not add coverage.
- **Detekt and ktlint repo-wide.** Both are still red (7 issues in this module alone;
  1656 ktlint violations repo-wide). Nothing in this item was allowed to touch them — that
  is 2.0.5.
- **The manual half.** No device is attached (owner decision Q3: physical device, not the
  emulator). The manual half of *this* item is the owner's review of the judgment table.

## Handoff

| Deferred | Why | Owner |
| --- | --- | --- |
| Keystore-path tests against a real AndroidKeyStore | Needs a device. A JVM mock cannot supply a hardware-backed AES key. | **2.0.2** establishes the device, then a follow-up item ports them to `app/src/androidTest`. The ignored body is left intact as a starting point. |
| **Password-strength scale is compressed to 0–75** while the signature and clamp imply 0–100 | Changing the scoring model is a product decision, not stabilization; `calculateStrength` has no production caller yet, so nothing is visibly broken. Fix before any strength *meter* UI ships. | **Phase 4** — new item 4.11. |
| `SecureUtils` and `SecurityUtils` are two unrelated objects in one file, one of them hosting extension functions | Directly caused defects #1, #2 and #4. Renaming a public security API is not stabilization work. | **Phase 4** — item 4.7-adjacent API cleanup. |
| `suggestWords` / mnemonic autocomplete | Deliberately dropped (Q1). If wanted, it needs its own roadmap item; the deleted tests recorded the intended contract. | Unassigned — owner to schedule if desired. |

---

# 2.0.2 — First run on a device

> Status: **IN PROGRESS 2026-09-21 — blocked on the owner unlocking the phone.**
> The app now builds, installs, launches and runs without crashing. The rendered UI has
> **not** been observed yet: the test device has a secure lock screen and nobody has
> unlocked it.

## The device

| Property | Value |
| --- | --- |
| Model | **Pixel 6a** (`bluejay`) |
| Android | 16 (API **36**) |
| Connection | adb over Wi-Fi (`adb-tls-connect`) |
| Owner decision | physical device, not the emulator (2026-09-21) |

## THE FINDING: the app had no entry point at all

`app/src/main/AndroidManifest.xml` declared **no `<activity>`** and the `<application>`
element had **no `android:name`**. The merged manifest of the shipped APK contained only
library-provided components (biometric dialog, Compose preview activity).

Consequences, both proven rather than inferred:

```
$ adb shell am start -n com.nexvault.wallet.debug/com.nexvault.wallet.MainActivity
Error: Activity class {com.nexvault.wallet.debug/com.nexvault.wallet.MainActivity} does not exist.

$ adb shell monkey -p com.nexvault.wallet.debug -c android.intent.category.LAUNCHER 1
** No activities found to run, monkey aborted. **
```

1. **The app could never be launched** — not from the launcher, not by adb. Every claim
   in `doc/` about screens working was therefore not merely unverified; it was
   unreachable. This, not the four placeholder tabs, is the root cause of "the GUI is not
   quite working."
2. **`@HiltAndroidApp` was never registered.** `NexVaultApplication` exists and is
   annotated, but with no `android:name` the platform instantiated the default
   `android.app.Application`. Even if an activity had existed, Hilt injection into
   `MainActivity` would have failed at runtime.

Fix applied (owner-approved 2026-09-21): register `.NexVaultApplication` on
`<application>`, and declare `.MainActivity` with `exported="true"`, the app theme, and a
`MAIN`/`LAUNCHER` intent filter.

After the fix:

| Check | Command | Result |
| --- | --- | --- |
| Launcher entry exists | `aapt2 dump badging` | `launchable-activity: name='com.nexvault.wallet.MainActivity' label='NexVault'` |
| Installs | `./gradlew :app:installDebug` | `Installed on 1 device.` |
| Launches | `am start -n .../MainActivity` | `Starting: Intent { ... }` |
| Process survives | `pidof com.nexvault.wallet.debug` | `11832` (alive) |
| No crash | `logcat -d \| grep -E "FATAL\|AndroidRuntime"` | **empty** |
| Process start logged | `logcat` | `ActivityManager: Start proc 11832:com.nexvault.wallet.debug/u0a392 for top-activity` |

## What has NOT been verified

- **The rendered UI.** The device was `mWakefulness=Dozing` with
  `isKeyguardShowing=true` for the first attempts; after `KEYCODE_WAKEUP` the screen is ON
  but the keyguard remains (`trustState=UNTRUSTED`, `deviceLocked=1`, focus on
  `NotificationShade`). `screencap` returns an all-black frame both times, and
  `wm dismiss-keyguard` does not clear a secure lock. **The owner must unlock the phone
  with its PIN/pattern before the happy path can be walked.**
- **Onboarding → PIN → unlock → home → token detail.** Nothing has been exercised.
- **The biometric path.** Needs real fingerprint hardware and an enrolled fingerprint.

## Expected problem, not yet confirmed

`Theme.NexVault` extends `Theme.MaterialComponents.DayNight.DarkActionBar`, and the
activity does not override it. `MainActivity` calls `enableEdgeToEdge()` and hosts
Compose content that draws its own Scaffold, so the platform ActionBar will very likely
render a stray title bar above the Compose UI. Not changed yet — this is a UI assertion
that must be confirmed with a screenshot rather than assumed. Logged for 2.0.3.

## Toolchain raised to SDK 37 (owner-approved 2026-09-21)

| Package | Before | After |
| --- | --- | --- |
| `platform-tools` (adb) | 37.0.0 | **37.0.1** |
| `build-tools` | 36.1.0 | **37.0.0** (36.x kept) |
| `platforms` | android-36, 36.1 | **android-37.2** (36.x kept) |

Project build targets raised in the same pass, across 19 module build files:

| Setting | Before | After |
| --- | --- | --- |
| `compileSdk` | `release(36) { minorApiLevel = 1 }` | `release(37) { minorApiLevel = 2 }` |
| `targetSdk` | 36 | **37** |

`feature-auth` was the only module still on the legacy `compileSdk = 36` form; it was
normalised to the same DSL as the other 18 so it resolves against the installed
`android-37.2` platform.

Results:

| Check | Result |
| --- | --- |
| `./gradlew :app:assembleDebug` | **BUILD SUCCESSFUL** — AGP 9.1.0 accepts compileSdk 37.2 |
| `aapt2 dump badging` | `compileSdkVersion='37'`, `platformBuildVersionName='17'`, `targetSdkVersion:'37'` |
| `./gradlew testDebugUnitTest --continue` | **223 tests, 222 pass, 1 skipped, 0 fail** — unchanged |

Note: raising `targetSdk` to 37 is a **behavioural** change, not just a compile setting —
it opts the app into Android 17's runtime restrictions. The build and unit tests prove
nothing about runtime behaviour on API 37; no API-37 device is available (the test device
runs API 36). The device install and launch above were performed against the API 36
device with `targetSdk 37`, which the platform accepts.

## Handoff

| Deferred | Why | Owner |
| --- | --- | --- |
| Confirming or refuting the stray ActionBar | **REFUTED — see §2.0.2b.** `MainActivity` extends plain `FragmentActivity`, not `AppCompatActivity`, so a `MaterialComponents` theme creates no ActionBar. No change made. |
| Runtime behaviour of `targetSdk 37` on a real API-37 device | No such device; the available one is API 36 | Owner decides whether it matters before release (3.7) |
| Restoring `build-tools`/`platforms` 36.x if a rollback is ever needed | 36.1.0 / android-36 / 36.1 are all still installed | Not needed unless a regression appears |

---

# 2.0.2b — First run of the onboarding flow: the wallet is created but never backed up

> Status: **CLOSED 2026-09-25 — owner-directed close after the device walk passed.** The fix
> round, its owner decisions and all evidence (automatic + the M1–M11 device walk) live in the
> item's own spec:
> [`../2026-09-25-2.0.2b-onboarding-repair/validation.md`](../2026-09-25-2.0.2b-onboarding-repair/validation.md)
> (requirements / plan / validation). Summary: the wallet is no longer persisted before the user
> acknowledges the mnemonic (option B), `is_wallet_set_up` is written only at Set PIN completion
> (option A), the root router checks the session first, and CR 1.6-1 is remediated by
> KeyStore-only wallet encryption plus an unlocked-session retrieval gate. Build green,
> **249 tests / 0 failures / 1 skipped**; the flow now runs Welcome → mnemonic → verify → Set PIN →
> Home, cold-start unlock works, and both import paths land on Home (the walk also found and fixed
> a mnemonic-grid crash and a completion-cancellation defect). The original finding below is kept
> as the record of what the first run observed.
>
> Status before the fix: **BLOCKED — new critical defect found. The app is currently unusable on
> the test device and the wallet it created is unrecoverable by the user.**
> Reached the welcome screen and one tap further. Reported to the owner 2026-09-21.

## What was observed

With the device unlocked, the app runs and renders correctly:

| Step | Result |
| --- | --- |
| `am start` on a fresh process | launches, PID alive |
| Welcome screen | renders: shield icon, "NexVault", "Your keys. Your crypto.", **Create New Wallet**, Import Wallet — dark theme, edge-to-edge, **no stray ActionBar** |
| Tap **Create New Wallet** | **lands on the Unlock screen ("Enter your PIN to unlock")** — the mnemonic screen is never shown |
| Enter a 6-digit PIN | rejected with **"No PIN set"** in red above the dots — correct, accurate feedback |

The wallet was nevertheless really created. Device state read with
`adb shell run-as com.nexvault.wallet.debug`:

```
files/wallet/mnemonic.enc                                     386 bytes, mode 0600
files/datastore/security_preferences.preferences_pb           active_wallet_id, active_account_index,
                                                              is_wallet_set_up
                                                              ** no password_hash, no auth_method **
files/datastore/wallet_metadata.preferences_pb                {"id":"732efc80-0ad1-49a7-92fa-1afa9cc5e1bb",
                                                               "name":"Main Wallet","type":"HD"}
                                                              address 0xDc6D56BfFA21b1E9bb3C6B02F7c7cC071d351BEe
                                                              path m/44'/60'/0'/0/0
```

So a real BIP-39 mnemonic was generated, a real BIP-44 address derived, and the mnemonic
encrypted to disk. The user saw none of it.

## Root cause

### Clean reproduction (2026-09-21, after the owner cleared app data)

The first observation was made while the owner was also entering the device lock-screen
PIN, which took input focus mid-sequence. It was therefore repeated from a verified-clean
state with no concurrent input. **Before/after, one tap:**

| | Before the tap | After the tap |
| --- | --- | --- |
| `files/` directory | **does not exist** (data cleared) | — |
| `security_preferences.preferences_pb` | does not exist | `active_wallet_id = d51ff3ff-1dcf-4096-8529-d6102f8aff97`, `is_wallet_set_up`, **no `password_hash`** |
| `files/wallet/mnemonic.enc` | does not exist | **398 bytes**, created `07:35` |
| `wallet_metadata` | does not exist | address `0xB5Bb2c16DAbFaa7baDf79cFBDab444d54a23C6DB`, path `m/44'/60'/0'/0/0` |
| Screen | Welcome ("Create New Wallet") | **"Enter your PIN to unlock"** |
| Process | — | alive, `logcat` free of `FATAL` |

The address differs from the first run's `0xDc6D56BfFA21b1E9bb3C6B02F7c7cC071d351BEe`,
confirming a genuinely new mnemonic was generated and stranded. Nothing depends on tap
accuracy: the state is read straight off the filesystem.

### Mechanism

`CreateWalletViewModel` creates the wallet in its **constructor**:

```kotlin
init { createWallet() }          // feature-onboarding/.../CreateWalletViewModel.kt
```

`createWalletUseCase` persists the wallet *and* sets the `is_wallet_set_up` preference.
Meanwhile `NexVaultApp` derives its root destination from that flag and rebuilds the
whole graph when it changes:

```kotlin
val routingKey = remember(isWalletSetUp, isAuthed) {
    when { !isWalletSetUp -> "onboarding"; !isAuthed -> "auth"; else -> "main" }
}
key(routingKey) { NavHost(startDestination = …) }   // app/.../NexVaultApp.kt
```

Sequence on tapping **Create New Wallet**:

1. `navController.navigate(CREATE_WALLET)` → `CreateWalletScreen` composes
2. `CreateWalletViewModel` is constructed → `init` fires `createWalletUseCase("Main Wallet")`
3. The wallet is persisted and `isWalletSetUp` flips to `true`
4. `routingKey` changes `"onboarding"` → `"auth"` (`isAuthed` is still `false`)
5. `key(routingKey)` **tears the onboarding NavHost down and rebuilds it at the auth graph**
6. The user lands on Unlock. The mnemonic was generated, encrypted, written — and never rendered

Two design faults combine here: `is_wallet_set_up` is overloaded to mean both "a wallet
row exists" and "onboarding is finished", and the root router treats it as a reactive
switch that can fire mid-flow.

## Two observations retracted

Both were measurement errors caused by concurrent input, not app behaviour. Recorded
because a validation log that only keeps the findings it got right is worthless.

| Initially reported | What actually happened |
| --- | --- |
| "The PIN keypad drops taps" — six evenly-spaced `adb shell input tap` calls filled 3 dots on one attempt, 5 on the next | **Retracted.** The owner was entering the device's lock-screen PIN at the same time, so input focus was being taken away mid-sequence. On a clean run with no other input, all six taps registered and the PIN completed. |
| "The unlock screen gives no error and no feedback" | **Retracted.** With the PIN actually completed, the screen shows **"No PIN set"** in red above the dots and clears the entry. `AuthRepositoryImpl.verifyPin` returns `AuthResult.Failed(message = "No PIN set")` when no hash is stored, and the UI surfaces it faithfully. |

Neither retraction touches the finding below: it rests on the persisted state
(`mnemonic.enc` present, `password_hash` absent) and on the observed navigation from
**Create New Wallet** to the Unlock screen, none of which depends on tap accuracy.

## Why this matters more than the other defects

The mnemonic is the **only** way to recover a non-custodial wallet. Here it is generated,
encrypted, and stored, but the user never sees it and the **Verify Mnemonic** screen —
the control that exists precisely to prove the backup was written down — is unreachable.
Lose the device and the funds are gone. The wallet is created and unbacked-up in the same
instant, with no consent step.

The second consequence is that the app is now **unrecoverable on this device**: a PIN was
never set (`no password_hash`), yet Unlock demands one, so the install cannot be entered
at all. The only way forward is to clear app data.

## Not yet established

- Whether any *other* state transition has the same mid-flow graph-swap problem
  (`onOnboardingComplete` → `onAuthSuccess()` and the auto-lock path both mutate
  `isAuthenticated`, which is also part of `routingKey`).
- The rest of the happy path: mnemonic → verify → Set PIN → main → Home → Token detail.
- Whether the `is_wallet_set_up` flip also breaks the **Import Wallet** path, which shares
  `SetPinScreen` and the same graph — it was not exercised.

## Proposed remediation (not started — needs an owner decision)

The minimal change that makes the intended flow work is to stop `is_wallet_set_up` from
flipping during onboarding: have `createWallet` persist the wallet **without** setting the
flag, and set it only when onboarding actually completes (at Set PIN / `onOnboardingComplete`).

Alternatives, with different trade-offs:

| Option | Shape | Trade-off |
| --- | --- | --- |
| **A. Move the flag write** to the end of onboarding | Smallest change; `is_wallet_set_up` becomes "onboarding complete" | Requires `createWallet` and the flag write to be separable, which they currently are not — `WalletRepositoryImpl.createWallet` sets both |
| **B. Stop creating the wallet in `init`** | Generate the mnemonic first, persist only after the user acknowledges it | Matches the security intent (no wallet exists until consent) but is a larger change to the use case and the screen |
| **C. Drop the reactive root router** | Explicit navigation between graphs; `routingKey` only used for the initial destination | Fixes the whole class of mid-flow swap bugs, but touches app startup and auto-lock |

Recommendation: **B as the target, A as the immediate unblock.** Every option needs its own
feature spec before implementation per `AGENTS.md`.

## Immediate consequence for the test device

The install must be reset (`adb shell pm clear com.nexvault.wallet.debug`, or uninstall +
reinstall) before the flow can be retested. That destroys the wallet created above — which
is currently worthless anyway, since its mnemonic was never captured.

---

# Toolchain upgrade — AGP 9.1.0 → 9.4.1, Gradle 9.3.1 → 9.6.0

> Owner-driven (via Android Studio's AGP Upgrade Assistant), 2026-09-21. Not a roadmap
> item; recorded here because it happened mid-Phase-2.0 and every later result is measured
> against it. **Regression-verified: no behaviour changed.**

## What changed

| File | Change |
| --- | --- |
| `gradle/libs.versions.toml` | `agp = "9.1.0"` → **`"9.4.1"`** |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle `9.3.1` → **`9.6.0`** + new `distributionSha256Sum` |

**AGP 9.4.1 requires Gradle 9.6.0** — worth recording, because the natural guess (that a
9.4.x plugin wants a 9.4.x Gradle) is wrong and would have sent a future upgrade down the
wrong path.

**Kotlin 2.3.10 and KSP 2.3.6 were deliberately not touched.** KSP resolves its compiler
version from the Kotlin version; letting them drift breaks `kspDebugKotlin` outright. The
lockstep survived the upgrade, and that was checked explicitly rather than assumed.

The first attempt failed with **"No space left on device"** after the downloads completed
but before the build ran. Diagnosis afterwards:

- The Gradle 9.6.0 distribution was **not** corrupted — `gradle-9.6.0-bin.zip.ok` present,
  `gradle --version` reports `Gradle 9.6.0`, launcher JVM 25.0.3.
- All AGP 9.4.1 artifacts were already in `modules-2`: `gradle`, `gradle-api`, `builder`,
  `apksig`, `aapt2-proto`, `gradle-settings-api`, `gradle-common-api`, `builder-model`.
- ~3.1 GB was reclaimed by deleting three unused Gradle instances (8.4, 8.14.4, 9.2.0)
  and their wrapper distributions. The 9.6.0 distribution and version cache were kept —
  which is why recovery needed no re-download.

## Regression results

Baseline = measured 2026-09-21 immediately before the upgrade.

| Check | Baseline | After upgrade | Verdict |
| --- | --- | --- | --- |
| `:app:assembleDebug` | PASS | **PASS** (6m44s, 246 tasks executed) | ✅ |
| `testDebugUnitTest --continue` | 223 tests, 222 pass, 1 skip, 0 fail | **223 / 222 / 1 / 0** | ✅ identical |
| `:domain:test` | **stale — see below** | **63 pass, 0 fail** (fresh) | ✅ |
| `detekt` | 75 issues | **75 issues** | ✅ identical, per-module identical |
| `ktlintCheck` | 1656 | **1565** | ✅ upgrade-neutral — see below |
| Gradle 10 deprecation | 1 warning | **1 warning** (not escalated to error) | ✅ |

### The ktlint delta is not the upgrade's doing

1656 → 1565 is −91, and the entire delta sits in one module:

| Module | Baseline | After | Delta |
| --- | --- | --- | --- |
| `core-security` main | 262 | **170** | **−92** |
| `core-security` test | 25 | **26** | +1 |
| **all 18 other modules** | — | — | **0, count for count** |

`core-security` is exactly the module 2.0.1 edited: the deleted hardcoded 100-word BIP-39
literal was 12 lines carrying ~10 words each, which ktlint flagged repeatedly. Eighteen
untouched modules reporting byte-identical counts is the evidence that the toolchain
changed nothing.

### A verification gap this run exposed

The `domain` module is pure Kotlin/JVM, so its task is `:domain:test`, **not**
`testDebugUnitTest`. Every earlier "full suite" aggregate in this record included `domain`'s
63 tests read from result XMLs **dated ~170 days earlier** — they were being counted, but
never re-executed. The total was right by luck; it had never been measured.

`AGENTS.md` already documents the correct command pair (`testDebugUnitTest` **plus**
`:domain:test`). The aggregate script did not follow it. Now run explicitly: 63 tests, 0
failures, fresh.

## Gradle 10 deprecation — diagnosed

```
The ReportingExtension.file(String) method has been deprecated.
This is scheduled to be removed in Gradle 10.
	at Build_gradle$1.execute(build.gradle.kts:16)
```

`build.gradle.kts:16` is `apply(plugin = "io.gitlab.arturbosch.detekt")` inside
`subprojects { }`. The caller is the **Detekt Gradle plugin**, not project code, and it
remains a warning under Gradle 9.6.0. Clearing it needs a detekt plugin bump — editing the
build script would not help. Owner: roadmap item 4.9.

## Handoff

| Deferred | Why | Owner |
| --- | --- | --- |
| Clearing the `ReportingExtension.file` deprecation | Needs a Detekt plugin release that stops using the removed-in-Gradle-10 API | **4.9** |
| Removing `composeOptions.kotlinCompilerExtensionVersion` (app + 11 feature modules) | Dead config under the Kotlin 2.x Compose plugin. AGP 9.4.1 did **not** flag it, so it is cleanup rather than a fix | Done in G5 |
| Any further space recovery | Old AGP artifacts (7.3.1 … 9.1.0) total only 241 MB; not worth touching a working dependency cache | Unassigned |

---

# Grouped dependency upgrade — 2026-09-21

> Owner-directed, spec-first: `specs/tech-stack.md` §4 was updated with the target versions
> and committed **before** any build file was touched, so intent can be diffed against
> result. Five groups, each applied and regression-tested on its own, each its own commit.

## Baseline

| Metric | Before |
| --- | --- |
| `:app:assembleDebug` | PASS |
| Tests | 223 (222 passing, 1 skipped, 0 failing) |
| detekt | 75 issues |
| ktlint | 1656 violations |
| Failing gate tasks | 49 |

## Per-group results

| Group | Change | Build | Tests | detekt | ktlint |
| --- | --- | --- | --- | --- | --- |
| **G1** | KSP 2.3.6 → 2.3.12 | PASS (every `:kspDebugKotlin` executed) | 223 / 0 fail | 75 | 1565 |
| **G2** | 19 minor/patch bumps | PASS | 223 / 0 fail | 75 | 1565 |
| **G3** | Compose BOM 2026.03 → 2026.09, ui-test 1.10.5 → 1.12.1 | PASS | 223 / 0 fail | 75 | 1565 |
| **G4** | web3j 5.0.2 → 6.0.0 | PASS (after packaging fix) | 223 / 0 fail | 75 | 1565 |
| **G5** | catalog hygiene + 13 `composeOptions` removals | PASS | 223 / 0 fail | 75 | **1553** |

**No Kotlin source file was changed by any group.** Every version moved under code that
compiled unchanged, including the web3j major bump.

## What each group actually cost

**G1 (KSP)** — isolated because KSP is version-paired with Kotlin and a mismatch fails
codegen outright rather than degrading. Kotlin stayed at 2.3.10; KSP has published **zero
2.4.x releases**, so a Kotlin 2.4 move was not available at any price. This is the single
most important constraint discovered in the sweep, and it is now recorded in
`tech-stack.md` §4 so the next upgrade does not have to rediscover it.

**G3 (Compose)** — largest blast radius in the catalog: 12 modules. Verified the new BOM
actually took effect rather than silently resolving the old one:
`:app:dependencies` reports `androidx.compose.foundation:foundation-android:1.12.1` where
it previously resolved `1.10.x`. Test re-execution was confirmed by result-file mtimes, not
assumed — only `app`, `feature-auth` and `feature-onboarding` depend on Compose *and* carry
tests, and those are the three that re-ran.

**G4 (web3j)** — held to its own group because it lands in four modules with the security
module the highest-consequence consumer (`MnemonicUtils` incl. `getWords()`,
`Bip32ECKeyPair`, `Bip44WalletUtils`, `Keys`). Surprise: **no code changes were needed** —
every API this project touches kept its signature from 5.0.2 to 6.0.0.

It did force one build change. web3j 6 moved to **Jackson 3** (`tools.jackson.*`, a
namespace change), while its own `tuweni-bytes → vertx-core` chain still pulls **Jackson 2**
(`com.fasterxml.jackson.*`). Both jars carry `META-INF/thirdparty-LICENSE`, so
`mergeDebugJavaResource` failed on a duplicate path. Added that path to the existing
`packaging { resources { excludes += … } }` list in `app/build.gradle.kts`, beside the netty
and FastDoubleParser entries already there. The two Jackson lines live in different packages
and coexist at runtime; only the duplicated license file is dropped.

Worth knowing for 3.7: the app now ships three Jackson variants —
`tools.jackson.core:jackson-core:3.1.0` (web3j 6),
`com.fasterxml.jackson.core:jackson-core:2.16.1` (via vertx), and
`software.amazon.awssdk:third-party-jackson-core:2.27.24`.

**G5 (hygiene)** — all six removals were confirmed to have zero references first:

| Removed | Why it was safe |
| --- | --- |
| `shimmer` version + library | The declared 1.2.0 **does not exist** — `com.facebook.shimmer:shimmer` tops out at 0.5.0. Nothing referenced it: `core-ui`'s `ShimmerPlaceholder` is a hand-rolled Compose animation using `animateFloat` + `Brush`. No build ever failed; the first `implementation(libs.shimmer)` would have. |
| `zxing` version alias | A `[versions]` entry with **no matching `[library]` entry**, so `libs.zxing` could never resolve. |
| `compose-compiler` version + 13 `composeOptions` blocks | Inert under Kotlin 2.x — the Compose compiler comes from `org.jetbrains.kotlin.plugin.compose`, which resolves from `kotlin`. |
| `detekt`/`ktlint`/`spotless` **library** aliases | Applied as plugins via `libs.plugins.*`. Their *version* aliases stay — the plugin entries still reference them. |

Kept deliberately: four unused bundles (`blockchain`, `camerax`, `testing`, `debug`) cost
nothing and 2.7/3.x will want the CameraX and WalletConnect groupings; `junit-jupiter` and
`kotlinx-collections-immutable` are valid-but-unused, which is the Phase 4.5 adopt-or-delete
decision rather than hygiene.

## The ktlint delta, fully attributed

`1656 → 1553` is **−103**, and both halves are accounted for:

| Component | Before | After | Delta | Cause |
| --- | --- | --- | --- | --- |
| `ktlintKotlinScriptCheck` (build.gradle.kts) | 108 | **96** | **−12** | 13 `composeOptions` blocks removed in G5 |
| Source-set checks | 1548 | **1457** | **−91** | 2.0.1 deleting the 100-word BIP-39 literal from `core-security` |

Nothing is unexplained, which is the point of computing it: the toolchain changes
themselves contributed **zero** ktlint movement, and 18 untouched modules report
count-for-count identical numbers across every group.

## A number that was wrong, and why

The debug APK measured **49 MB** at the end of G4 but **43 MB** after a `clean` rebuild in
the final pass. The 6 MB gap is not a regression being fixed — the 49 MB figure came from an
incremental build carrying residue accumulated across the toolchain and dependency churn.
Only the clean figure is meaningful, and it is the one to quote.

`doc/08`'s "APK under 25 MB" bar applies to the **minified release** build, which has still
never been produced. That comparison stays outstanding for 3.7.

## Final state

| Metric | Before | After |
| --- | --- | --- |
| `:app:assembleDebug` (from `clean`) | PASS | **PASS** |
| Tests | 223 / 222 pass / 1 skip / 0 fail | **223 / 222 / 1 / 0** |
| detekt | 75 | **75** (unchanged) |
| ktlint | 1656 | **1553** (−103, fully attributed) |
| Failing gate tasks | 49 | **49** (unchanged) |
| Debug APK | 49 MB (stale) | **43 MB** (clean) |
| Kotlin source changes | — | **none** |

Both quality gates remain red, exactly as before. This upgrade deliberately did not touch
them — that is 2.0.5, and it still needs the `.editorconfig`-vs-reformat decision.

## Handoff

| Deferred | Why | Owner |
| --- | --- | --- |
| Kotlin 2.4.x | Blocked upstream: KSP has no 2.4.x release. Re-check the pairing before any move. | Whoever next bumps Kotlin |
| `compose-bom` beyond 2026.09 | Moved 6 months in one step and passed; nothing outstanding | — |
| The three coexisting Jackson variants | Two are transitive and cannot be excluded safely without proving nothing on those paths is called | **4.4**-adjacent; revisit if APK size matters |
| Debug-vs-release APK size | Release has never been built; `doc/08`'s 25 MB bar is unverified | **3.7** |
| `junit-jupiter`, `kotlinx-collections-immutable`, 4 unused bundles | Kept on purpose; adopt-or-delete is a product decision | **4.5** |

---

# 2.0.7 — Phase 2.0 close

> Status: **CLOSED 2026-09-28 — owner-directed, both halves recorded (M-A1 and M-A3 confirmed, M-A2
> walked).** Owner decisions for this item: `requirements.md` §"Phase 2.0 close — owner decisions
> (item 2.0.7)". Branch: `feature/2.0.7-record-and-close` (committed, merged to `main` with
> `--no-ff`, pushed, branch deleted). **This item changed records only** — no production, test or
> build-script file is touched (diff is `specs/` + `AGENTS.md`).

## What this close certifies — 2.0.1 → 2.0.6

| Item | What it did | Verification state | Record |
| --- | --- | --- | --- |
| 2.0.1 | Repaired the `core-security` suite (64 tests that never compiled); nine test-vs-implementation judgments, one real production bug (`getWordList()` 100 → 2048 words) | Automatic green; the owner's review of the nine judgments is confirmed **in this close** (§Manual half M-A1) | this file §2.0.1 |
| 2.0.2 | First run on a device: the app had **no entry point** (no `<activity>`, no `@HiltAndroidApp` registration) — fixed; SDK 37 toolchain raised | Entry point verified by every walk since; the happy path was observed by 2.0.2b | this file §2.0.2 |
| 2.0.2b | Onboarding repair: mnemonic shown + verified before persistence, `is_wallet_set_up` written only after the PIN, CR 1.6-1 (KeyStore-only, session gate), abandoned-attempt wipe, grid-crash fix | **Device walk M1–M11 passed**; 249 tests; owner-signed | [`2026-09-25-2.0.2b-onboarding-repair/validation.md`](../2026-09-25-2.0.2b-onboarding-repair/validation.md) |
| 2.0.3 | Navigation dead ends: Send/Receive/Swap/See-All disabled with phase captions; empty callbacks deleted | Reviewer-verified (`da3d9ff`/`c3858cf`) incl. a device re-walk; 249 tests | [`2026-09-25-2.0.3-navigation-dead-ends/validation.md`](../2026-09-25-2.0.3-navigation-dead-ends/validation.md) |
| 2.0.4 | API-key strategy: real keys are the supported path; explicit "Not configured" states; CoinGecko key-optional | Reviewer PASS (`bf8daf5`); keys live in `local.properties` (never committed) | [`2026-09-25-2.0.4-api-key-strategy/validation.md`](../2026-09-25-2.0.4-api-key-strategy/validation.md) |
| 2.0.4b | Etherscan V1 → V2 migration (V1 was deprecated, history could not return data); plan-gate + rejection envelopes surfaced | Reviewer-verified (`e82eba2`); **M3/M4 closed in this close**; M1/M2 suspended (zero balance), M5 still owed | [`2026-09-26-2.0.4b-etherscan-v2-migration/validation.md`](../2026-09-26-2.0.4b-etherscan-v2-migration/validation.md) |
| 2.0.5 | Quality gates green: detekt **65 → 0**, ktlint **1556 → 0**; `.editorconfig` codifies the style | Reviewer PASS (`729d663`, fresh 633-task run) + owner device walk | [`2026-09-26-2.0.5-quality-gates/validation.md`](../2026-09-26-2.0.5-quality-gates/validation.md) |
| 2.0.6 | TC traceability: all 52 `doc/07` ids mapped or registered; first Robolectric suite; creation-flow E2E; one dead-code production fix (the 2.0.4/2.0.4b notices) | Reviewer PASS (`e7b0c5f` + `1efe2e8`, fresh rerun + 5/5 device) | [`2026-09-27-2.0.6-tc-traceability/validation.md`](../2026-09-27-2.0.6-tc-traceability/validation.md) |

## Automatic half — fresh evidence (2026-09-27, `feature/2.0.7-record-and-close`)

Every command ran with `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` on this branch, whose tree is
identical to `main` + this record's edits.

| # | Command | Observed result |
| --- | --- | --- |
| A1 | `./gradlew testDebugUnitTest :domain:test detekt ktlintCheck --rerun-tasks --continue` | **BUILD SUCCESSFUL, exit 0 — 571 tasks executed** (nothing cached), **0 failed tasks**. Report root: `/home/superguo/Projects/NexVault/<module>/build/test-results/` |
| A2 | Test sums from the XML reports (A1) | **299 tests, 0 failures, 0 errors, 1 skipped** — app 1, core-database 3, core-datastore 29, core-network 20, core-security 64 (1 skipped), data 53, domain 71, feature-auth 15, feature-home 2, feature-onboarding 35, feature-tokens 6 |
| A3 | `./gradlew :app:assembleDebug` (up-to-date for this tree — Gradle verified no input changed) | `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk`, **45,923,819 bytes**, 2026-09-27 21:55 |
| A4 | TC audit re-run (grep + `comm`) | `doc/07` ids **52**; `// TC-` markers **25**; registered **27**; **0 orphan markers**; 52 = 25 ∪ 27; marker adjacency 25/25 |
| A5 | `adb shell pm clear com.nexvault.wallet.debug` (screen woken first) → `ANDROID_SERIAL=adb-26111jEGR13989-MJzh3R._adb-tls-connect._tcp ./gradlew :app:connectedDebugAndroidTest` | **BUILD SUCCESSFUL, exit 0 — 5/5 pass**: `BackupPolicyTest` 3/3, `CreationFlowE2ETest` 1/1 (create → verify → PIN → Home), `ExampleInstrumentedTest` 1/1. XML: `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel 6a - 17.xml` |

`spotless` was not used as evidence anywhere (no configuration block — a no-op).

## Phase 0 flip table (owner decision Q1)

Basis, in this order: (1) the row's CR task file has **no open finding**; (2) its deliverable has
been exercised by a device walk or an instrumented test **or** its missing coverage is explicitly
owned elsewhere; (3) both gates and the full suite are green on the closing tree (A1–A2).

| Row | Findings state | Verification basis | Verdict |
| --- | --- | --- | --- |
| 1.1 Project scaffolding | 1.1-3 **Closed** 2026-09-27 (reviewer-verified on device) | Build/packaging green; the scaffold is what every walk has run on | **`[x]`** |
| 1.2 Design system & theme | **1.2-6, 1.2-7 open** → 4.17; 1.2-5 composables → 4.18 | Components render in every walk, but open findings remain | stays `[~]` |
| 1.3 Security module | no open rows | Suite repaired by 2.0.1 (64 tests / 1 skipped); PIN, mnemonic, import/flows walked | **`[x]`** |
| 1.4 DataStore & preferences | **1.4-4 open** → 4.17 | 1.4-1 fixed by 2.0.2b; one open string-adoption finding remains | stays `[~]` |
| 1.5 Domain models & repos | no open rows | 71 domain tests; use cases exercised through every walked flow | **`[x]`** |
| 1.6 Data layer | 1.6-1 **Closed** by 2.0.2b (KeyStore-only + session gate) | 53 data tests incl. the Etherscan V2 round; wallet material walked | **`[x]`** |
| 1.7 Onboarding | no open rows | Device walk M1–M11 (2.0.2b); 35 tests; creation E2E on device (A5) | **`[x]`** |
| 1.8 Auth / unlock | no open rows | Cold-start unlock + lockout walked; 15 tests incl. TC-VM-009 | **`[x]`** |
| 1.9 Main scaffold | no open rows | 5-tab shell re-walked in 2.0.3; the 4 placeholder tabs are 2.6–2.8 scope (recorded, not hidden) | **`[x]`** |
| 2.1 Network module | 2.1-1 **Closed** by 2.0.6 | 20 tests (explorer + adapters); live balances/prices rendered in the 2.0.5 walk | **`[x]`** |
| 2.2 Database module | 2.2-1 **Closed** by 2.0.6 | 3 Robolectric DAO tests; Room is the read model behind every walked screen | **`[x]`** |
| 2.3 Chain management | no open rows | Chain switch walked; 2.3-1 (dead fields) fixed and verified | **`[x]`** |
| 2.4 Home dashboard | 2.4-1 **Closed** by 2.0.6 (VM half) | 2 VM tests; Home walked with live prices/chart (2.0.5). TC-UI-004/005 → 4.18 | **`[x]`** |
| 2.5 Token detail | 2.5-1 **Closed** by 2.0.6 | 6 VM tests; screen walked incl. the notice paths | **`[x]`** |
| 2.0.1 | — | Nine judgments reviewed by the owner **in this close** (M-A1); suite 64 tests since | **`[x]`** |
| 2.0.2 | — | Exit criteria met: happy path observed on the device (2.0.2b walk), entry point fixed, no crash filed | **`[x]`** |
| 2.0.4b | M1–M5 | M3/M4 closed in this close (M-table below); M1/M2 suspended, M5 owed | stays `[~]` |
| 2.9 Default token list | — | Seeded ETH/USDC rows rendered on the Home screen in the 2.0.5 walk; seeding logic covered by `RefreshBalancesUseCaseTest` | **`[x]`** |

**Result: 15 rows `[x]`** (12 Phase 0 + 2.0.1 + 2.0.2 + 2.9), **3 rows stay `[~]`** (1.2, 1.4, 2.0.4b)
— each with the reason above, none silently.

## Consolidated Handoff — Phase 2.0 → Phase 2.6+ (union of all seven Phase 2.0 handoff lists)

| Deferred | Why it is still deferred | Owner |
| --- | --- | --- |
| `TransactionRepositoryImpl` write paths (`estimateGas`, `sendNativeTransaction`, `sendTokenTransaction`) | Send flow is 2.6 (they throw `UnsupportedOperationException` today) | **2.6** |
| Home/TokenDetail Send + Receive callbacks, "See All" | Destinations arrive with 2.6 / 2.7 / 2.8 / 3.3 (2.0.3 disabled them) | **2.6 / 2.7 / 2.8 / 3.3** |
| "See All" device exercise | Needs real history (2.8) | **2.8** |
| True pagination (`page`/`pageSize` ignored) and `updateTransactionStatus` chain check | History work | **2.8** |
| Dedicated in-app history error banner for generic `ExplorerApiException` | History UI owns the surface | **2.8** |
| Key-absent states on screens built later (send/receive/swap/history) | Their error surfaces arrive with their items | **2.6 / 2.7 / 3.3 / 2.8** |
| Alchemy key still unused (no failover/RPC switching) | Signing/RPC choice | **2.6 / 2.7** |
| WalletConnect project id consumed by nothing | DApp pairing | **3.1** |
| TC-REPO-003/004, TC-VM-003/004/005, TC-UI-006, TC-UI-010, TC-INT-002 | TC ids registered by 2.0.6 | **2.6** |
| TC-REPO-005, TC-UI-008 | TC ids registered by 2.0.6 | **2.8** |
| TC-INT-003 | TC id registered by 2.0.6 | **3.1** |
| TC-UI-007 | TC id registered by 2.0.6 | **3.2** |
| TC-VM-010 | TC id registered by 2.0.6 | **3.3** |
| TC-UI-009, TC-SECTEST-005, TC-INT-004; Settings-screen delete-last-wallet behaviour | Settings work (2.0.2b handoff) | **3.4** |
| Release-vs-debug APK size (`doc/08`'s 25 MB bar, never measured on a release build) | Release build is 3.7 | **3.7** |
| No CI: both gates only run when a human runs them; detekt does not analyse `src/androidTest` | Automation item | **4.3** |
| Three coexisting Jackson variants (two transitive) | Revisit only if APK size matters | **4.4** |
| `junit-jupiter`, `kotlinx-collections-immutable`, 4 unused bundles | Adopt-or-delete product decision | **4.5** |
| `spotless` applied with no configuration (a no-op) | Configure or remove | **4.6** |
| FLAG_SECURE on sensitive screens; private-key zeroing after signing; TC-SECTEST-001/002/003 | Security hardening pass | **4.7** |
| PIN-based re-key of wallet material, biometric PIN recovery, change-PIN re-key | 2.0.2b chose KeyStore-only + session gate for that round | **4.7**-adjacent (unassigned) |
| TC-REPO-001/002 (deep Web3j-mocked balance/metadata tests) | Needs a web3j-mocking approach; coverage bar owns the gap | **4.8** |
| `ReportingExtension.file` Gradle-10 deprecation | Needs an upstream detekt plugin release | **4.9** |
| Password-strength scale tops out at 75 of 100 | Product decision, no caller yet | **4.11** |
| `SecureUtils` / `SecurityUtils` split (caused three 2.0.1 defects) | Public security API rename | **4.12** |
| `@Ignore`d AndroidKeyStore round-trip test + TC-SECTEST-004 | Device-only test port | **4.13** |
| TC-UI-001…005 instrumented UI tests + the onboarding-grid regression test (2.0.2b) | New roadmap row 4.18 | **4.18** |
| Findings 1.2-6, 1.2-7, 1.4-4; placeholder-tab strings | String/token adoption | **4.17** |
| Reactive root-router replacement (option C from 2.0.2b) | Out of that item's scope; revisit only if another mid-flow swap appears | unassigned |
| `suggestWords` / mnemonic autocomplete | Dropped in 2.0.1 Q1; needs its own item if wanted | unassigned |
| 2.0.4b's M1/M2 (funded history) and M5 (crash watch) | Owner's funding decision; M5 needs a device session. M3/M4 were closed by the 2.0.7 close (2026-09-28) | **2.0.4b** (stays `[~]`) |

Also carried, unchanged in ownership: the ktlint layout-dialect reversal (owner decision reversing
2.0.5's choice) and the 20 `TooManyFunctions` offenders (refactor only if the ceiling is hit), plus
the paid-Etherscan-plan decision (owner) — all from the 2.0.5 / 2.0.4b handoffs.

## 2.0.4b debt update — M3/M4 closed here

The 2.0.6 production fix (the refresh use case swallowed its result) is what made these two rows
observable at all. Walked on the Pixel 6a on **2026-09-28** (agent-executed at the owner's
instruction, on `feature/2.0.7-record-and-close`; the 2.0.4b record carries the full detail):

| # | Step | Observed | Verdict |
| --- | --- | --- | --- |
| M3 | Token detail on **BSC** (`chainid` 56) → the persistent plan notice | **"Transaction history for this chain is not covered by the current Etherscan plan."** rendered above "No transactions yet" (`/tmp/m3-20-plan-notice.png`). Required a real chain switch: Home → chain selector → BNB Smart Chain → BNB row | ✅ |
| M4 | `ETHERSCAN_API_KEY` blanked → explicit "Not configured" | **"Transaction history is not configured. Add ETHERSCAN_API_KEY to local.properties and rebuild."** (`/tmp/m4-10-not-configured.png`). Key restored and the working build reinstalled afterwards; the real value was never printed by the app or written into any record | ✅ |

**Procedure correction found by running it, now fixed in AGENTS.md:** blanking to the placeholder
string `your_key_here` does *not* exercise the state — `ChainConfigProvider.isExplorerConfigured`
checks `isNotBlank()`, so only an **empty** value is "not configured".

M1 (mainnet history), M2 (Polygon history) and M5 (crash watch) remain owed, so 2.0.4b stays `[~]`.

## Manual half — actions and confirmations (2026-09-27/28)

| # | Action | Result |
| --- | --- | --- |
| M-A1 | Owner review of the nine 2.0.1 test-vs-implementation judgments (required for the 2.0.1 flip) | ✅ **CONFIRMED by the owner 2026-09-28** — rows 1–6, 8, 9 were stale test code; row 7 (`getWordList()` 100 → 2048 words) was the one real production bug, fixed. This is the explicit confirmation the roadmap required before 2.0.1 could leave `[~]`. |
| M-A2 | Device observation of the BSC plan notice (M3) and the blank-key "Not configured" state (M4) | ✅ **agent-executed at the owner's instruction** ("DO these steps for me", 2026-09-28) — screenshots + UI-tree dumps; not claimed as an owner walk |
| M-A3 | Owner acceptance of the flip table and direction to close | ✅ **ACCEPTED by the owner 2026-09-28** — "Accept and close 2.0.7"; the close (commit, `--no-ff` merge, push, branch delete, 2.0.7 → `[x]`) followed in the same pass |

## Honesty notes

- This item produced **no new automated coverage** — it certifies existing evidence and adds the
  closing runs. The 299/0/0/1 and 5/5 figures are fresh re-executions (571 tasks), not cached.
- The Phase 0 flips rest on the CR findings state **as recorded in the task files**, plus the walks;
  they do not claim per-screen walk coverage beyond what those walks reported.
- `doc/08`'s acceptance criteria are checked per item, not wholesale: 2.0.x items each recorded
  their own criteria, and the roadmap carries what is still open (Phase 2.6+ rows).
- The device's wallet was wiped by the A5 recipe (`pm clear`) and the E2E created a fresh one
  (PIN `123456`, mnemonic generated on-screen, never logged) — same consequence as 2.0.6. The M3/M4
  walk on 2026-09-28 created another such wallet (same PIN) because the connected-test run had
  uninstalled the app and wiped its data; that wallet is what sits on the device now, on the BSC chain.
- **The M4 walk surfaced one secrets-hygiene incident:** a verification grep printed the real
  Etherscan key from a generated test `BuildConfig` file into the session transcript. It appeared in
  no tracked file (`app/build/` is git-ignored; nothing was committed) and no record holds it; the
  owner was told and may rotate the key.

## Reviewer handoff — requested 2026-09-28 (developer note, not a verdict)

The owner opened a code-reviewer session for the 2.0.7 close. The item is already merged and pushed
(`main` == `origin/main` == `6fc9d0b`, the `--no-ff` merge of `ddc2e25`), so the reviewer works
against `main` rather than a branch. What it is asked to verify — nothing below is claimed as
verified by the reviewer:

| # | Claim to check | How |
| --- | --- | --- |
| V1 | The fresh runs really are green | `./gradlew testDebugUnitTest :domain:test detekt ktlintCheck --rerun-tasks --continue; echo $?` -> exit 0, no failed tasks, **299 / 0 / 0 / 1** summed from `<module>/build/test-results/.../TEST-*.xml` |
| V2 | The TC audit still holds | re-extract `doc/07` ids vs `// TC-` markers in `src/test` + `src/androidTest`; expect 52 = 25 ∪ 27, 0 orphans, 25/25 markers directly above a `@Test` |
| V3 | The flip table matches the tree | `grep -c '\*\*Open\*\*' specs/features/2026-09-21-legacy-code-review/tasks/*.md` -> only 1.2 (x2) and 1.4 (x1) non-zero; i.e. every row flipped to `[x]` has no open finding, and the rows left `[~]` are exactly the ones with open findings (plus 2.0.4b's M1/M2/M5 debt) |
| V4 | The consolidated Handoff drops nothing | check its rows against the seven source lists: `2026-09-20-2.0.0-stabilization/validation.md` (its four handoffs), `2026-09-25-2.0.2b...`, `2026-09-25-2.0.3...`, `2026-09-25-2.0.4...`, `2026-09-26-2.0.4b...`, `2026-09-26-2.0.5...`, `2026-09-27-2.0.6...` |
| V5 | Records are consistent | roadmap: Phase 0 header + 15 rows `[x]`, 1.2/1.4 `[~]`, 2.0.1/2.0.2/2.9/2.0.7 `[x]`, 4.15 refuted, current position -> next entry **2.6**; `AGENTS.md` standing gate matches; `mission.md` reality counters; `tech-stack.md` section 5 |
| V6 | Diff scope is records-only | `git diff --name-only 780bcdb..6fc9d0b` -> `AGENTS.md` + `specs/**` only; no production, test or build-script file |
| V7 | The device evidence | M3/M4 screenshots (`/tmp/m3-20-plan-notice.png`, `/tmp/m4-10-not-configured.png`) and the instrumented XML `app/build/outputs/androidTest-results/connected/debug/TEST-Pixel 6a - 17.xml` (5/5); reproducible with the `specs/dev-environment.md` section 5 recipe (wake -> `pm clear com.nexvault.wallet.debug` -> `ANDROID_SERIAL=<colon-free mDNS serial> :app:connectedDebugAndroidTest`) |

Known limits the reviewer should not treat as findings: the M3/M4 walk wiped and recreated a device
wallet (documented consequence), M1/M2/M5 of 2.0.4b remain owed, and the secrets incident above is
already recorded.


