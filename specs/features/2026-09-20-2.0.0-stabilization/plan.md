# 2.0.1 — Plan (repair the `core-security` test suite)

> Requirements: `requirements.md` (Q1–Q4 answered 2026-09-21, defects #1–#8 traced).
> Validation: `validation.md`. Parent roadmap item: Phase 2.0 / 2.0.1.
> Changes stay uncommitted until the owner's commit word.

## §0 Inputs

Four files are touched, three of them test-only:

| File | Change class | Defects addressed |
| --- | --- | --- |
| `core/core-security/src/test/.../util/SecurityUtilsTest.kt` | test | #1 #2 #3 #4 |
| `core/core-security/src/test/.../mnemonic/MnemonicManagerTest.kt` | test | #5 #8, plus Q1 deletions |
| `core/core-security/src/test/.../encryption/EncryptionManagerTest.kt` | test | #6, plus Q2 `@Ignore` |
| `core/core-security/src/main/.../mnemonic/MnemonicManager.kt` | **production** | #7 |

No dependency changes. No build-file changes. `libs.mockk` is already declared in
`core/core-security/build.gradle.kts`, so the Mockito port needs nothing new — which is
itself the evidence that Mockito was never a sanctioned choice.

## §1 The production fix (finding #7 — the only main-source edit)

`MnemonicManager.getWordList()` currently returns a 100-entry hardcoded literal with the
comment *"first 100 words as example"*. Replace the literal and the comment with a
delegation to web3j:

```kotlin
fun getWordList(): List<String> = MnemonicUtils.getWords()
```

Rationale for using the library rather than pasting 2048 words:

- `org.web3j.crypto.MnemonicUtils.getWords()` is **public static**, and
  `MnemonicUtils` is already imported by this very file for `generateMnemonic`,
  `validateMnemonic` and `generateSeed`.
- Its backing resource `en-mnemonic-word-list.txt` ships inside `org.web3j:crypto:5.0.2`
  (verified: 2048 lines, 2048 unique entries, first `abandon`, last `zoo`).
- A pasted literal would be a second source of truth for a consensus-critical list, and
  would silently drift if the library's list were ever corrected.
- The `private companion object` and `BIP39_WORDLIST` property are deleted with it; the
  file has no other member of that companion, so the companion goes too.

Invariant to preserve: the returned list must be the same list `generateMnemonic`
produces words from, so that a generated mnemonic always validates against
`getWordList()`. Delegating to the same library guarantees this; a literal does not.

## §2 Test repairs

### 2.1 `SecurityUtilsTest.kt`

Three imports are added; the file currently imports only JUnit:

```kotlin
import com.nexvault.wallet.core.security.util.SecureUtils.secureWipe
import com.nexvault.wallet.core.security.util.SecurityUtils.hexToByteArray
import com.nexvault.wallet.core.security.util.SecurityUtils.toHex
```

Importing `secureWipe` by name brings both the `ByteArray` and `CharArray` overloads,
which is exactly what the two call sites need.

- line 42: `byteArrayOf(1, 2, 3, 4, 5, 255.toByte(), 0, 128.toByte())`. The round trip
  still holds: `255.toByte()` is `-1` and `toHex` masks with `and 0x0f` after a signed
  shift, producing `ff`, which `hexToByteArray` maps back to `-1`.
- line 112: `SecureUtils.secureWipe(chars)` → `chars.secureWipe()`.

No assertions change; the tested behaviour was always right.

### 2.2 `MnemonicManagerTest.kt`

- lines 61, 68, 69: pass `""` as the passphrase, matching all three production call sites
  and the two call sites at lines 76–77 that were already correct.
- lines 86–87: replace the false assertions. `"wallet"` and `"bitcoin"` are not BIP-39
  words; assert on `"walk"` and `"bitter"` instead, and add a boundary check on the last
  word `"zoo"` so the assertion covers both ends of the real list.
- lines 90–107: delete `testSuggestWords`, `testSuggestWordsEmptyPrefix` and
  `testSuggestWordsLimit` (Q1).

### 2.3 `EncryptionManagerTest.kt`

Mechanical Mockito → MockK port:

| Before | After |
| --- | --- |
| `import org.mockito.Mockito` | `import io.mockk.every`, `import io.mockk.mockk` |
| `Mockito.mock(KeyStoreManager::class.java)` | `mockk<KeyStoreManager>()` |
| `Mockito.mock(SecretKey::class.java)` | `mockk<SecretKey>()` |
| ``Mockito.`when`(x).thenReturn(y)`` | `every { x } returns y` |

`mockk<KeyStoreManager>()` without `relaxed = true` is deliberate: the password-derived
tests never call the keystore, so a strict mock turns any accidental keystore call into a
failure rather than a silent pass-through. That is the behaviour we want from these tests.

The keystore path gets an explicit ignore rather than a fake:

```kotlin
@Ignore("Requires a real AndroidKeyStore — see specs/features/2026-09-20-2.0.0-stabilization/requirements.md (Handoff)")
@Test
fun testEncryptWithKeystore() { ... }
```

Note for the record: `testEncryptWithKeystore` as written asserts nothing at all — it
stubs a mock key and returns. It was never a test; `@Ignore` makes that honest instead of
leaving a green dot that proves nothing. Its body is left intact so the follow-up
instrumented port has a starting point.

## §3 Order of work

1. Fix `getWordList()` (§1) — the only main-source change, done first so the test that
   asserts 2048 words can pass.
2. Repair `SecurityUtilsTest` (§2.1) — imports and casts only.
3. Repair `MnemonicManagerTest` (§2.2) — arguments, corrected assertions, deletions.
4. Port `EncryptionManagerTest` (§2.3).
5. Run `:core:core-security:testDebugUnitTest` until green.
6. Run the full `testDebugUnitTest --continue` to prove nothing regressed elsewhere —
   `data` mocks `MnemonicManager`, so a `getWordList` change is safe, but `data`'s
   `WalletRepositoryImplTest` exercises the same class and must be re-proven.
7. Only then touch `detekt`/`ktlint` — those are 2.0.5 and out of scope here.

## §4 Risks

| Risk | Mitigation |
| --- | --- |
| `getWordList()` has no production caller, so a wrong fix would go unnoticed | AC-3 asserts size 2048, first word `abandon`, last word `zoo`, and no duplicates — enough to catch a truncated or reordered list. |
| PBKDF2 at 100 000 iterations runs in 4 tests and is slow | Accept it; correctness over speed. If the suite becomes painful, the iteration count is a production constant and not ours to lower for tests. |
| Porting to MockK could mask a real difference from Mockito's defaults | Only `mockk<T>()` and `every{} returns` are used — no behaviour that differs between the two frameworks. |
| `@Ignore` hides a real gap | The gap is stated in the ignore reason, in `validation.md`, and in the Handoff section; it is tracked, not hidden. |
| Deleting the `suggestWords` tests could look like hiding a failure | `requirements.md` Q1 records the owner decision; the deletion is a scope decision, not a workaround. |
