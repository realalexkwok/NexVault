# 2.0.2b — Repair the onboarding flow — Requirements

> Roadmap item: **2.0.2b — Repair the onboarding flow** (Phase 2.0, blocker `[!]`).
> Parent evidence: `../2026-09-20-2.0.0-stabilization/validation.md` §2.0.2b.
> Branch: `feature/2.0.2b-onboarding-repair`. Plan: `plan.md`. Evidence: `validation.md`.
> Owner decisions recorded 2026-09-25 (ask round before implementation).

## Problem

Tapping **Create New Wallet** creates the wallet and lands on **Unlock** instead of the
mnemonic, leaving the app unusable (no PIN was ever set) and the wallet unbacked-up.
Root cause (measured 2026-09-21, confirmed in code 2026-09-25):

1. `CreateWalletViewModel.init` calls `createWalletUseCase`, which persists the wallet
   **and** writes `is_wallet_set_up = true` (`WalletRepositoryImpl.kt:92`).
2. `NexVaultApp` derives `routingKey` from `isWalletSetUp` and wraps the root `NavHost`
   in `key(routingKey)` (`NexVaultApp.kt:56,64`) — the onboarding graph is torn down and
   rebuilt at the auth graph mid-flow.
3. The import path has the same defect (`WalletRepositoryImpl.kt:153`, `:211`).
4. **CR finding 1.6-1 (Critical, deferred here 2026-09-23):** the mnemonic's and private
   key's inner encryption "password" is the wallet UUID, which is stored in plaintext in
   `wallet_metadata` — the inner PBKDF2 layer holds zero secret — and retrieval
   (`getMnemonicForBackup`, `addAccount`) has no enforced authentication gate.

## Owner decisions (Q/A, 2026-09-25)

| # | Question | Answer |
| --- | --- | --- |
| Q1 | Remediation shape for the mid-flow graph swap (roadmap options A/B/C) | **B + A.** Wallet creation leaves the ViewModel `init`: the wallet is persisted only when the user acknowledges the mnemonic and taps Continue. `is_wallet_set_up` is written only when onboarding actually completes (Set PIN success). The reactive root router (option C) is out of scope this round. |
| Q2 | Remediation of CR 1.6-1 (mnemonic/private key encrypted with the plaintext wallet UUID; no auth gate) | **KeyStore-only + session gate.** Drop the wallet-specific inner PBKDF2 layer; wallet files are protected by the KeyStore-wrapped AES-GCM layer alone. Enforce an **unlocked-session gate** in the repository before mnemonic/private-key retrieval (`getMnemonicForBackup`, `addAccount`), with an exemption while onboarding is in progress (the Verify screen must decrypt the file it just wrote). Generic password-crypto primitives (`encryptWithPassword` / `decryptWithPassword` / `deriveKeyFromPassword`, `PasswordEncryptedData`, `DerivedKeyResult`) stay as tested API; the wallet-specific `doubleEncrypt` / `doubleDecrypt` / `DoubleEncryptedData` and their equality test are deleted with the layer. |
| Q3 | Single item or grouped with 2.0.3 | **2.0.2b alone.** 2.0.3 (navigation dead ends) is planned separately. |

### Derived decisions

| # | Decision | Why |
| --- | --- | --- |
| D1 | `is_wallet_set_up` changes meaning to "onboarding complete", not "a wallet row exists". | The flag drove the root router; overloading it was half the defect. |
| D2 | No PIN re-key and no biometric-PIN plumbing this round. | Q2 choice; `storeBiometricEncryptedPassword` has zero callers and reviving it is a separate security change. |
| D3 | Abandoned onboarding attempts are cleaned up before a new wallet is persisted (orphan guard). | `mnemonic.enc` is a single-file store; without the guard a second attempt would overwrite the first silently and stack metadata rows. |
| D4 | `userPreferences.hasCompletedOnboarding` is written at completion. | It is read by `AppStateManager.isFirstRun` but had no writer. |
| D5 | The gate lives in `data` (`WalletRepositoryImpl`) backed by `AppStateManager.isUnlocked`. | Single injectable seam; `MainActivity`'s local `_isAuthenticated` was not reachable from `data`. |
| D6 | No new dependencies; no changes to lockout/wipe behaviour. | Tech-stack compliance; 1.4's wipe-at-20 work is already verified. |

## Acceptance criteria

| AC | Criterion |
| --- | --- |
| AC-1 | With cleared app data, onboarding runs Welcome → Create → **mnemonic shown** → Verify → Set PIN → main, with no mid-flow graph swap. |
| AC-2 | The mnemonic displayed is the one that decrypts `files/wallet/mnemonic.enc` (proved by the Verify step, which reads its word list from that file, plus unit round-trip tests). |
| AC-3 | The import path (mnemonic and private key) completes the same way; `is_wallet_set_up` is not written before Set PIN. |
| AC-4 | No wallet is persisted before the user acknowledges the mnemonic and taps Continue. |
| AC-5 | `WalletStore` encrypts/decrypts the mnemonic and private keys with the KeyStore-wrapped layer only; no wallet UUID is used as a password anywhere. |
| AC-6 | `getMnemonicForBackup` and `addAccount` fail with an authentication error while the session is locked (onboarding-in-progress exempted), and succeed once unlocked. |
| AC-7 | Abandoned onboarding cannot leave a stale wallet that a later attempt silently overwrites (orphan guard). |
| AC-8 | Full build + unit tests green; `detekt`/`ktlintCheck` show **no new violations in touched files** (repo-wide gates remain 2.0.5's). |

## Non-goals

- Option C (replace the reactive root router) — Q1.
- PIN-based re-encryption of wallet material, biometric PIN recovery, change-PIN re-key — Q2/D2.
- 2.0.3 dead navigation callbacks, 2.0.4 API-key strategy, 2.0.5 quality gates, 2.0.6 TC traceability.
- Any change under `doc/` or the legacy CR records; the CR 1.6-1 row stays in its task file
  until the **reviewer** verifies the fix and deletes it.
