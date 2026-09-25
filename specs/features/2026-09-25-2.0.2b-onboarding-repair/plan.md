# 2.0.2b — Repair the onboarding flow — Technical plan

> Approved 2026-09-25 (plan mode). Requirements: `requirements.md` (Q1–Q3, D1–D6).
> Evidence: `validation.md`. Branch: `feature/2.0.2b-onboarding-repair` — changes stay
> **uncommitted** for owner review; merging/committing is an owner action.

## Target flow

```
Welcome ──tap Create──▶ CreateWalletScreen
                        ├─ VM init: GenerateWalletDraftUseCase   (no persistence)
                        ├─ shows 12 words, acknowledge checkbox
                        └─ tap Continue ─▶ CreateWalletUseCase(draft)  ─ persists wallet,
                                                                          flag still false
VerifyMnemonicScreen ── reads mnemonic.enc via getMnemonicForBackup (onboarding exemption)
                        └─ verified ─▶ SetPinScreen
SetPinScreen ── tap confirm ─▶ SetPinUseCase (hash) ─▶ CompleteOnboardingUseCase
                                                       ├─ is_wallet_set_up = true
                                                       ├─ hasCompletedOnboarding = true
                                                       └─ AppStateManager.unlock()
                                                       └─ onAuthSuccess() ─▶ routingKey = "main"
```

`NexVaultApp` is **not** modified: `routingKey` can no longer change mid-onboarding because
neither the flag nor the session flips before Set PIN completes.

## Task groups

### TG1 — domain (`domain/`)
- New `model/wallet/WalletDraft.kt`: `data class WalletDraft(mnemonic, mnemonicWords, address)`.
- New `usecase/wallet/GenerateWalletDraftUseCase.kt` → `WalletRepository.generateWallet()`.
- `usecase/wallet/CreateWalletUseCase.kt`: `invoke(walletName, draft)` — persists a draft.
- New `usecase/wallet/CompleteOnboardingUseCase.kt` → `WalletRepository.completeOnboarding()`.
- `repository/WalletRepository.kt`: add `generateWallet()`, `completeOnboarding()`; change
  `createWallet`; KDoc the authentication gate on `getMnemonicForBackup`/`addAccount`.
- `usecase/wallet/GetMnemonicForBackupUseCase.kt`: KDoc states the enforced gate.

### TG2 — core-security (`core/core-security/`)
- `wallet/WalletStore.kt`: `storeMnemonic(mnemonic)` / `retrieveMnemonic()` and
  `storePrivateKey(address, privateKey)` / `retrievePrivateKey(address)` use
  `encryptWithKeystore` / `decryptWithKeystore`; JSON becomes `{ciphertext, iv}`.
- `encryption/EncryptionManager.kt`: delete `doubleEncrypt` / `doubleDecrypt`.
- `encryption/EncryptionModels.kt`: delete `DoubleEncryptedData`.
- `EncryptionManagerTest`: delete `testDoubleEncryptedDataEquality` (the equality test of a
  deleted model). Password-path tests stay.

### TG3 — core-datastore (`core/core-datastore/`)
- `state/AppStateManager.kt`: `_isUnlocked = MutableStateFlow(false)`, `isUnlocked`,
  `suspend fun unlock()`, `fun lock()`; `onAuthenticationSuccess()` unlocks; `resetApp()` locks.

### TG4 — data (`data/`)
- `repository/WalletRepositoryImpl.kt`:
  - inject `AppStateManager`;
  - `generateWallet()` (mnemonic + derived address, no persistence);
  - `createWallet(walletName, draft)`; `importFromMnemonic` / `importFromPrivateKey` — all
    three persist **without** `setWalletSetUp(true)` and start with the orphan guard
    (`if (!isWalletSetUp.first()) { walletStore.wipeAll(); walletMetadataStore.clearAll() }`);
  - `completeOnboarding()` — flag + `hasCompletedOnboarding` + `unlock()`;
  - `getMnemonicForBackup` gate: `isUnlocked || !isWalletSetUp`; `addAccount` gate: `isUnlocked`;
  - new `WalletStore` signatures.

### TG5 — feature-onboarding (`feature/feature-onboarding/`)
- `viewmodel/CreateWalletViewModel.kt`: no `init` side effect beyond draft generation;
  two phases (generate / persist) with distinct retry semantics — a persist failure retries
  the **same** draft, a generation failure regenerates; `onContinueClicked` persists then
  emits `NavigateToVerifyMnemonic`.
- `viewmodel/SetPinViewModel.kt`: inject `CompleteOnboardingUseCase`; call it after a
  successful `SetPinUseCase` and before emitting `NavigateToMain`.

### TG6 — app (`app/`)
- `MainActivity.kt`: inject `AppStateManager`; `onAuthSuccess` → unlock; `onAuthRequired`
  and the `onStart` auto-lock branch → lock.

### TG7 — tests
- `data/WalletRepositoryImplTest`: no-flag assertions on create/import; `completeOnboarding`;
  orphan guard; both gates (locked / unlocked / onboarding-exempt); new store signatures.
- `core-datastore/AppStateManagerTest`: unlock/lock transitions, success unlocks, reset locks.
- `core-security/EncryptionManagerTest`: delete the double-encryption equality test.
- `feature-onboarding/CreateWalletViewModelTest`: rewrite for generate→persist.
- `feature-onboarding/SetPinViewModelTest`: completion use case called only on success.
- `feature-auth/UnlockViewModelTest`, `VerifyMnemonicViewModelTest`, `ImportWalletViewModelTest`:
  re-run unchanged.

## Validation plan

**Automatic:** `./gradlew :domain:test testDebugUnitTest --continue :app:assembleDebug`; per-module
detekt/ktlint counts before/after to prove no new violations in touched files.

**Manual (Pixel 6a):** `pm clear` → create path walk → state snapshots (`is_wallet_set_up`,
`password_hash`, `mnemonic.enc`) before/after Set PIN → kill/relaunch → unlock → import path.
Recorded in `validation.md`; the owner confirms.

## Post-plan additions (2026-09-25, during implementation and the device walk)

The plan did not foresee these; each is recorded in `validation.md` with its evidence.

| # | Addition | Trigger |
| --- | --- | --- |
| V1 | Routing checks the session first; `MainActivity` passes `AppStateManager.isUnlocked` instead of a local `_isAuthenticated`. | Completing onboarding writes two states; with the flag first the graph still hopped through Unlock. |
| V2 | `deleteWallet`/`deleteAllWallets` lock the session when no wallet remains. | Keeps "no wallet ⇒ locked" true under V1. |
| V3 | Small lint-driven refactors (guard split, `WalletStore` helper inlining, two test classes split out). | Each removed a net-new detekt issue. |
| V4 | `MnemonicGrid` rewritten from `LazyVerticalGrid` to plain `Row`s. | The screen crashed on the device the moment the mnemonic rendered (`IllegalStateException`: lazy grid inside a scrolling column). |
| V5 | `completeOnboarding()` unlocks first and is `NonCancellable`; the biometric choice is saved before it. | The first device run showed the flag written without the completion record or the unlock — the router tear-down cancelled `viewModelScope` mid-write. |

## Risks & mitigations

| Risk | Mitigation |
| --- | --- |
| Persist failure after the mnemonic is shown | Retry persists the same draft; the mnemonic is never silently regenerated. |
| Abandoned attempt leaves a stale wallet | Orphan guard wipes wallet file + metadata when the flag is false before persisting (verified on device, M9). |
| Gate blocks the legitimate Verify-step read | Explicit onboarding exemption (`!isWalletSetUp`); covered by a unit test and by the device walk (M2). |
| Existing install on the test device holds an unrecoverable wallet | `pm clear` is part of the validation walk; the stranded wallet was never backed up. |
| Router tear-down cancelling the completion writes | Completion unlocks first and runs inside `NonCancellable`; ordering pinned by two regression tests (V5). |

## Handoff
Filled in `validation.md` when the item closes (Kotlin 2.4/KSP, biometric-PIN revival, option C,
2.0.3 dead callbacks — whichever remain deferred).
