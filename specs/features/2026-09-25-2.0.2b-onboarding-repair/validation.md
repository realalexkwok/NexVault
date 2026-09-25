# 2.0.2b — Repair the onboarding flow — Validation record

> Status: **BOTH HALVES EXECUTED 2026-09-25 — AWAITING OWNER CONFIRMATION.**
> Requirements: `requirements.md` (owner Q1–Q3, D1–D6). Plan: `plan.md`.
> Branch: `feature/2.0.2b-onboarding-repair`. Changes are **uncommitted** in the working tree
> per the owner-commit rule.
> Manual half: the owner reconnected the Pixel 6a and directed the walk; it ran on
> 2026-09-25 13:36–13:44 HKT and **passed** (M1–M9 below, plus two defects it found and that
> were fixed before the walk was repeated from clean data).

## What was wrong (confirmed in code, 2026-09-25)

1. `CreateWalletViewModel.init` called `createWalletUseCase`, which persisted the wallet **and**
   wrote `is_wallet_set_up = true` (`WalletRepositoryImpl.kt:92` before the fix).
2. `NexVaultApp` derived `routingKey` from that flag and wrapped the root `NavHost` in
   `key(routingKey)` (`NexVaultApp.kt:56,64` before the fix), so the onboarding graph was torn
   down and rebuilt at the auth graph mid-flow → Unlock with **no PIN ever set**.
3. The import path had the same defect (`WalletRepositoryImpl.kt:153`, `:211` before the fix).
4. **CR 1.6-1 (Critical):** `WalletStore.storeMnemonic(mnemonic, walletId)` used the wallet UUID —
   stored in plaintext in `wallet_metadata` — as the inner PBKDF2 password, so the inner layer held
   no secret, and `getMnemonicForBackup` / `addAccount` had no enforced authentication gate.

## Automatic half — commands and observed results

All commands from the repository root with
`JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` (Linux host recipe, `specs/dev-environment.md`).

| # | Check | Command | Result |
| --- | --- | --- | --- |
| A1 | Build | `./gradlew :app:assembleDebug` | **BUILD SUCCESSFUL**; APK `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk` (46,124,248 bytes, 2026-09-25 13:35) |
| A2 | Unit tests | `./gradlew testDebugUnitTest :domain:test --continue` | **BUILD SUCCESSFUL** — **249 tests, 0 failed, 1 skipped** (the `@Ignore`d AndroidKeyStore test) |
| A3 | Per-module counts | `**/build/test-results/**/TEST-*.xml` | `domain` 67 · `core-security` 61 (1 skipped) · `data` 42 · `feature-onboarding` 35 · `core-datastore` 29 · `feature-auth` 14 · `app` 1 |
| A4 | Gate delta | `./gradlew detekt ktlintCheck --continue`, run twice: once with the change set, once with it stashed (`git stash push -u`) | **detekt 71 → 65 issues; ktlint 1462 → 1454 violations**; **no file reports more detekt issues than at HEAD** (table below) |
| A5 | Stale-API sweep | `grep -rnE "storeMnemonic\(|storePrivateKey\(|retrieveMnemonic\(|retrievePrivateKey\(|doubleEncrypt|doubleDecrypt|DoubleEncryptedData"` | only the new no-password signatures and their call sites/tests remain |
| A6 | No test/build regressions | failed-task list of the A1/A2 run | **0** test or build tasks failed; the 49 failures are the pre-existing red gate tasks (2.0.5) |

Test-count reconciliation: baseline **223** tests / 222 pass / 1 skip (roadmap E2) → **249** / 248 / 1.
The +26 are this item's new/extended tests (`data` 27 → 42, onboarding 30 → 35, core-datastore
27 → 29) plus tests earlier CR rounds had already added in `domain` (63 → 67, untouched by this
item). Nothing was skipped or deleted to hide a failure; the single `@Ignore` is the keystore test
named in 2.0.1.

## Manual half — device walk (PASSED 2026-09-25)

Device: **Pixel 6a (`bluejay`), Android 16 / API 36**, adb over Wi-Fi (`10.42.0.139:35791`).
Method: `adb shell input tap` driven by `uiautomator dump` node bounds (the wallet screens set
`FLAG_SECURE`, so screenshots are black there — the UI dump plus the on-disk state carry the
evidence). Every step was executed twice: once to find the defects below, then again from
`pm clear` after the fixes; the table records the post-fix run.

| # | Step | Observed result | Verdict |
| --- | --- | --- | --- |
| M0 | `adb shell pm clear com.nexvault.wallet.debug`, `:app:installDebug`, launch | install + launch clean; Welcome renders (title, tagline, both buttons) | ✅ |
| M1 | Welcome → **Create New Wallet** | **"Your Recovery Phrase"** with the 12 numbered words in a 4×3 grid, checkbox, Continue — **not** the Unlock screen; no crash | ✅ |
| M2 | Acknowledge → **Continue** | wallet persisted; Verify screen loads its word list **from `mnemonic.enc`** (KeyStore-only decryption on real hardware); all 12 words accepted **in order** → Set PIN | ✅ |
| M3 | Set PIN screen | "Set Your PIN" (Step 3 of 3), 6-digit keypad, biometric toggle | ✅ |
| M4 | Enter + confirm `123456` | lands on **Home** ("Total Balance", Send/Receive/Swap, token list) — no Unlock detour, no crash | ✅ |
| M5 | State after M4 (`run-as`) | `files/wallet/mnemonic.enc` 231 B; `security_preferences` = `active_wallet_id`, `active_account_index`, `password_hash`, `password_salt`, `auth_method`, **`is_wallet_set_up`**; `user_preferences` = **`has_completed_onboarding`**; 1 wallet row, 1 account | ✅ |
| M6 | State **before** acknowledgment (M1 screen) and **after** Continue but before Set PIN | before: `files/wallet` **absent** (nothing persisted without consent); after Continue: `mnemonic.enc` present, security prefs hold **only** `active_wallet_id` + `active_account_index` — **no `password_hash`, no `is_wallet_set_up`** | ✅ |
| M7 | Force-stop → relaunch (cold start) | Unlock screen → `123456` → Home | ✅ |
| M8 | `pm clear` → **Import Wallet** → canonical 12-word vector `abandon ×11 about` → Set PIN → confirm | → Home; stored address **`0x9858EfFD232B4033E47d90003D41EC34EcaEda94`** — exactly the known BIP-44 address for that vector (`m/44'/60'/0'/0/0`); mid-import snapshot again showed no flag and no hash | ✅ |
| M8b | `pm clear` → **Import Wallet → Private Key** → `0x…01` → Set PIN → confirm | → Home; stored address **`0x7E5F4552091A69125d5DfCb7b8C2659029395Bdf`** (known address for key `1`); `private_keys.enc` present — the KeyStore-only private-key store works on hardware | ✅ |
| M9 | Abandoned attempt: create → Continue → force-stop at Verify → relaunch → create again | relaunch shows **Welcome** (not stuck); second attempt generates a **different** phrase and a **different** wallet id; `wallet_metadata` holds exactly **1 unique wallet id / 1 account / 1 derivation path** — the orphan guard wiped the abandoned attempt rather than stacking rows | ✅ |
| M10 | Invalid input | the non-checksum phrase `abandon about after again …` is rejected with **"Import failed. Please check your input."** — format validation alone does not let a bad phrase through | ✅ |
| M11 | Crash watch | `logcat` holds exactly **one** `FATAL EXCEPTION`, timestamped 13:31:03 — the pre-fix crash below. Every run after 13:32 is crash-free | ✅ |

## Two defects the walk found — and that were fixed in the same pass

Neither was visible from the code alone; both are consequences of the flow finally *reaching*
the screens it was supposed to reach.

### D1 — `CreateWalletScreen` crashed on the mnemonic grid (fixed)

```
FATAL EXCEPTION: main  (13:31:03)
java.lang.IllegalStateException: Vertically scrollable component was measured with an infinity
maximum height constraints …
  at androidx.compose.foundation.lazy.grid.LazyGridKt$rememberLazyGridMeasurePolicy$1$1.measure
```

`MnemonicGrid` was a `LazyVerticalGrid` nested inside the screen's `Column(verticalScroll(...))`.
Before this item the mnemonic never rendered (the graph was torn down first), so the bug had no
way to appear. **Fix:** `MnemonicGrid` now lays the words out with plain `Row`s
(`words.chunked(4)`), with `weight(1f)` cells and spacers keeping a short last row aligned —
no lazy container, no nested scrolling, and the phrase is at most 24 items anyway.

### D2 — onboarding completion was cancelled mid-write (fixed)

First post-fix run: the PIN was set, but the app landed on **Unlock** with `is_wallet_set_up`
written and **no `user_preferences` file at all**. Cause: `completeOnboarding()` wrote the flag
first; the router reacted by rebuilding the root graph at the auth graph, which destroyed the
Set PIN entry and **cancelled `viewModelScope`** — the completion record and the unlock never ran.
**Fix:** `completeOnboarding()` now unlocks **first** and runs the remaining writes inside
`withContext(NonCancellable)`; `SetPinViewModel` saves the biometric choice **before** calling it.
Both orderings are pinned by new tests (`completeOnboarding_unlocksBeforeWritingTheFlag`,
`biometricChoiceIsSavedBeforeOnboardingCompletes`) so the regression cannot come back silently.

## Deviations from the approved plan (and why)

| # | Plan said | Done instead | Reason |
| --- | --- | --- | --- |
| V1 | "`NexVaultApp` needs no change" | Routed on the session first (`isAuthed -> main`, then the flag) and made `MainActivity` pass `appStateManager.isUnlocked` instead of a local `_isAuthenticated`. | Completing onboarding writes two independent states. With the flag checked first, the flag-then-unlock ordering rebuilt the root graph at the auth graph — the bug class being fixed. |
| V2 | not mentioned | `deleteWallet`/`deleteAllWallets` lock the session when no wallet remains. | Necessary consequence of V1: otherwise "no wallet + unlocked session" would keep the user on `main`. Restores the pre-change routing outcome for wallet deletion. |
| V3 | not mentioned | `CreateWalletViewModel.onContinueClicked` guard split into two `if`s; `WalletStore` JSON helpers inlined; test classes `AppStateManagerSessionTest` / `SetPinViewModelCompletionTest` split out. | Each removed a **net-new detekt issue** instead of adding debt to the red baseline. |
| V4 | not mentioned | D1: `MnemonicGrid` rewritten from `LazyVerticalGrid` to plain `Row`s. | The screen crashed on the device the moment the mnemonic actually rendered; AC-1 is unreachable without it. |
| V5 | not mentioned | D2: completion unlocks first and is `NonCancellable`; biometric choice saved earlier. | The first device run showed the flag landing without the completion record or the unlock. |

## Test-vs-implementation judgments

Per `AGENTS.md`: decide which side is wrong, record it, never weaken a test to pass production code.

| # | Change | Judgment |
| --- | --- | --- |
| J1 | `EncryptionManagerTest.testDoubleEncryptedDataEquality` deleted | **The test covered a deleted model.** The inner wallet layer it belonged to *is* finding 1.6-1; owner decision Q2 removed the layer, so the model and its equality test go with it. Not a weakened assertion — no production path it exercised remains. |
| J2 | `WalletRepositoryImplTest.createWallet_*` rewritten for `(walletName, draft)` and the **absence** of `setWalletSetUp` | **The test encoded the defect** (`coVerify { setWalletSetUp(true) }` asserted the mid-onboarding flag write). The replacement asserts the opposite contract, plus a new test that the abandoned-attempt guard wipes when the flag is false and does **not** wipe an established wallet. |
| J3 | `SetPinViewModelTest` extended/split; `AppStateManagerTest` split | Additive only — every pre-existing assertion is retained verbatim; new tests cover completion, its failure path, the session flag and the ordering. |
| J4 | `WalletRepositoryImplTest` mocks `AppStateManager` | The gate is now part of the repository contract; three tests pin it (locked + onboarded → auth error, onboarding → allowed, unlocked → allowed), one pins `addAccount` locked. |
| J5 | D1/D2 regression tests added rather than a screen-level UI test | The screen change is layout-only and the walk covers it; a Compose UI test for it belongs to the TC-UI work (2.0.6), which has no home for instrumented tests yet. |

## Gate delta (the evidence behind A4)

| Metric | HEAD (`a080b41`) | With this change set | Delta |
| --- | --- | --- | --- |
| detekt issues | 71 | **65** | −6 |
| ktlint violations | 1462 | **1454** | −8 |
| Files with more detekt issues than HEAD | — | **none** | ✅ |

detekt per rule: `TooManyFunctions` 21 → 20, `NewLineAtEndOfFile` 11 → 9, `MaxLineLength` 10 → 8,
`CommentOverPrivateFunction` 5 → 5, `ComplexCondition` 1 → 1.

The three new production files (`WalletDraft.kt`, `GenerateWalletDraftUseCase.kt`,
`CompleteOnboardingUseCase.kt`) report **0** ktlint violations. The two new test classes carry
instances of the same rules the suite already violates everywhere
(`multiline-expression-wrapping`, `function-expression-body`, trailing-comma/annotation siblings);
they add no new violation *category*, and the repo-wide total still fell. The repo-wide ktlint
decision is **2.0.5** and was deliberately not taken here.

## Not verified by this item

- **The KeyStore-backed JVM test** — `EncryptionManagerTest.testEncryptWithKeystore` stays
  `@Ignore`d (port is item 4.13). On-device it is now covered: M2 and M8/M8b decrypt material
  written by `encryptWithKeystore` on the real AndroidKeyStore.
- **Migration of an existing install's `mnemonic.enc`** — the file format changed from the
  double-encrypted JSON to `{ciphertext}`. No migration is provided: `pm clear` was part of the
  walk and the only pre-existing wallet was the never-backed-up 2026-09-21 one, whose phrase is
  gone. Any other install must be cleared too. Recorded deliberately rather than discovered later.
- **Owner confirmation.** The walk passed and the evidence is above, but the owner has not yet
  confirmed the result, so the roadmap item stays `[~]`.
- **Device left as found, except for the test wallet**: the last walk step imported private key
  `0x…01` (a publicly known test key) with PIN `123456`. Harmless, and convenient for the 2.0.3
  walk; `pm clear` restores a virgin install.

## Handoff

| Deferred | Why | Owner |
| --- | --- | --- |
| Porting the keystore round-trip test to `app/src/androidTest` | Needs the device established by this walk | **4.13** |
| Repo-wide ktlint reformat / `.editorconfig` decision | Style-vocabulary conflict, untouched on purpose | **2.0.5** |
| detekt `TooManyFunctions` on `WalletRepository` (now 14/11) and `WalletRepositoryImpl` (19/11) | Threshold decision belongs to the same gate item; the ratios grew because this item added the `generateWallet`/`completeOnboarding` contract | **2.0.5** |
| Instrumented UI tests for the onboarding screens (incl. the grid layout that D1 crashed on) | TC-UI has no home yet | **2.0.6** |
| PIN-based re-key of wallet material, biometric PIN recovery, change-PIN re-key | Owner decision Q2 chose KeyStore-only + session gate for this round | Unassigned — security hardening (**4.7**-adjacent) |
| Replacing the reactive root router (roadmap option C) | Out of scope per Q1; the session-first ordering plus `NonCancellable` completion removes the mid-flow class for onboarding | Unassigned — revisit if another mid-flow swap appears |
| Settings-screen behaviour after deleting the last wallet (`V2`) | No production caller yet; 3.4 will exercise it | **3.4** |
