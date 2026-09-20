# 2.0.1 — Repair the `core-security` test suite

> Feature spec (requirements). Planning round **2026-09-21**: four ask-round questions
> answered by the owner (Q1–Q4 below). Branch: `feature/2.0.1-core-security-tests` —
> **not created yet**; work currently sits uncommitted on `main` per the owner-commit
> rule.
>
> Parent roadmap item: Phase 2.0 (stabilization), sub-item 2.0.1. Sibling evidence
> record: `validation.md` in this directory.

## Goal

`:core:core-security:compileDebugUnitTestKotlin` fails with 16 compile errors, so the
module's **64 tests have never executed once** since they were written. This item makes
the suite compile, makes every surviving test express a true statement about the
production code, and fixes the one genuine production defect the tests were pointing at.

The security module is the wrong place to carry a dead test suite: mnemonic handling and
key encryption are the highest-consequence code in the repository, and they are currently
the *only* significant module with zero executed tests.

## Owner decisions (2026-09-21)

| Q | Question | Decision |
| --- | --- | --- |
| Q1 | `getWordList()` returns 100 of 2048 BIP-39 words (a real bug); `suggestWords()` does not exist at all. How much do we do? | **Fix `getWordList()` to the full 2048 words; delete the 3 `suggestWords` tests.** Implementing an unused autocomplete API is feature work and does not belong in a stabilization pass — if it is ever wanted it gets its own roadmap item. |
| Q2 | Part of `EncryptionManagerTest` needs a real AndroidKeyStore. | **Port the whole file to MockK. Keep the password-derived path on the JVM; mark the keystore path `@Ignore` with a reason.** Moving it to `androidTest` would block this item on 2.0.2 (no emulator), and the ignored cases are recorded for later. |
| Q3 | 2.0.2 needs the emulator package installed. | **Do not install it — the owner will use a physical device.** Consequence: 2.0.2 stays blocked until a device is attached; see the Handoff section. |
| Q4 | The stray `Users/superguo/Projects/NexVault/...` tree at the repo root. | **Delete it.** It was a 123-byte accidental fragment of `TokenRepositoryImpl.kt` (the real file is 19001 bytes). Done — the working tree is clean. |

## Findings — test vs implementation, judged case by case

The rule from `AGENTS.md` applies: *"Do not fix a failing test by weakening the production
code it tests; decide whether the test or the implementation is wrong and record the
decision."* Each defect below was traced to its root cause before choosing a side.

### Test-side defects (production code is correct)

| # | Location | Error | Root cause | Verdict |
| --- | --- | --- | --- | --- |
| 1 | `SecurityUtilsTest.kt:15` | `Unresolved reference 'secureWipe'` | `secureWipe` is an extension on the **`SecureUtils`** object, which shares `SecurityUtils.kt` with the unrelated `SecurityUtils` object. The test declares no imports. | Test wrong — add `import ...SecureUtils.secureWipe` |
| 2 | `SecurityUtilsTest.kt:112` | `SecureUtils.secureWipe(chars)` does not resolve | It is an *extension member*, so it cannot be invoked as `Object.fn(receiver)`. | Test wrong — call `chars.secureWipe()` |
| 3 | `SecurityUtilsTest.kt:42` | `Argument type mismatch: Int, but Byte was expected` ×2 | `byteArrayOf(1, 2, 3, 4, 5, 255, 0, 128)` — `255` and `128` exceed `Byte` range. Columns 51 and 59 in the compiler output land exactly on those two literals. | Test wrong — `255.toByte()`, `128.toByte()` |
| 4 | `SecurityUtilsTest.kt:43–44` | `Unresolved reference 'toHex'` | `toHex` / `hexToByteArray` are extensions on `SecurityUtils`; same missing-import cause as #1. | Test wrong — add the two imports |
| 5 | `MnemonicManagerTest.kt:61,68,69` | `No value passed for parameter 'passphrase'` | `mnemonicToSeed(mnemonic, passphrase)` genuinely takes two parameters. All three production call sites (`WalletRepositoryImpl.kt:57,119,289`) pass `""` explicitly. | Test wrong — pass `""` |
| 6 | `EncryptionManagerTest.kt:10,23,111,112` | `Unresolved reference 'mockito'` | Mockito was never a dependency. The module already declares `testImplementation(libs.mockk)`, and `data`'s suite mocks the equally-final `MnemonicManager` with MockK successfully. | Test wrong — port to MockK |

### Implementation-side defects (the test was right)

| # | Location | Problem | Verdict |
| --- | --- | --- | --- |
| 7 | `MnemonicManager.getWordList()` | Returns a hardcoded list of **100** words (`MnemonicManager.kt:55-66`), while `MnemonicManagerTest.kt:84` asserts 2048. The in-code comment admits it: *"first 100 words as example"*. | **Production is wrong.** Delegate to `org.web3j.crypto.MnemonicUtils.getWords()`, which is public static and backed by the `en-mnemonic-word-list.txt` resource (verified: 2048 lines, 2048 unique, `abandon` → `zoo`) already on the classpath via `org.web3j:crypto:5.0.2`. |
| 8 | `MnemonicManagerTest.kt:86,87` | Asserts the word list contains `"wallet"` and `"bitcoin"`. | **Neither word is in BIP-39** (verified against the shipped list: the `wal` prefix yields only `walk`, `wall`, `walnut`; `bit` yields only `bitter`). This is a *test* bug that would have surfaced the moment the file compiled. Replace with words that exist. |

Finding 8 is worth stating plainly: it means the suite contains assertions that were
never executed and are simply false. Anything else in these files that *looks* verified is
unverified until it runs.

## Functional requirements

- **FR1** `:core:core-security:compileDebugUnitTestKotlin` completes with zero errors.
- **FR2** Every test in the module either passes or is explicitly `@Ignore`d with a
  stated reason. No test is silently deleted except the three named in Q1.
- **FR3** `getWordList()` returns the canonical 2048-word BIP-39 English list.
- **FR4** `suggestWords` tests are removed; `MnemonicManager` gains no new public API.
- **FR5** No production behaviour changes other than FR3. In particular the crypto paths
  in `EncryptionManager`, `KeyStoreManager` and `WalletStore` are untouched.
- **FR6** Every test-side edit is justified in `validation.md` by the root cause above,
  not by "make it compile".

## Out of scope

- Implementing `suggestWords` / mnemonic autocomplete (Q1).
- Keystore-path tests against a real AndroidKeyStore (Q2 — deferred, see Handoff).
- The `SecureUtils` / `SecurityUtils` two-objects-in-one-file naming confusion that
  caused defects #1 and #4. It is a real readability trap, but renaming a public security
  API is not stabilization work — logged for Phase 4.
- Wiring `getWordList()` into the import screen. It still has no production caller.

## Acceptance criteria

- **AC-1** `./gradlew :core:core-security:testDebugUnitTest` → BUILD SUCCESSFUL.
- **AC-2** Test count is reported and reconciled: 64 `@Test` annotations existed; the
  surviving count is recorded with the exact number skipped and why.
- **AC-3** `getWordList()` assertion on size 2048 passes; the list contains `abandon`
  (first) and `zoo` (last) and no duplicates.
- **AC-4** `git diff` shows zero changes under `core/core-security/src/main/` except
  `MnemonicManager.kt`'s word-list body.
- **AC-5** The full `./gradlew testDebugUnitTest --continue` run stays green for all
  previously-green modules (162 tests) plus the newly-enabled security tests.

## Handoff

- **Keystore-path tests** (currently `testEncryptWithKeystore`, plus whatever
  `KeyStoreManagerTest`/`WalletStoreTest` need once they run) must eventually execute
  against a real AndroidKeyStore. Owner: **2.0.2** establishes the device; a follow-up
  item owns porting them to `app/src/androidTest`. They cannot be verified in 2.0.1.
- **`SecureUtils` vs `SecurityUtils` split** — two security utility objects in one file,
  with extension functions on one and top-level helpers on the other, directly caused two
  of the six test defects. Owner: **Phase 4** (API cleanup).
- **`suggestWords` feature** — deliberately dropped. If mnemonic autocomplete is wanted
  for the import screen, it needs its own roadmap item and a feature spec; the deleted
  tests document the intended contract if anyone revisits it.
