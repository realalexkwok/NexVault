package com.nexvault.wallet.domain.repository

import com.nexvault.wallet.domain.model.auth.WalletCreationResult
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.wallet.Account
import com.nexvault.wallet.domain.model.wallet.Wallet
import com.nexvault.wallet.domain.model.wallet.WalletDraft
import kotlinx.coroutines.flow.Flow

/**
 * Repository for wallet lifecycle operations.
 * Implementation lives in the `data` module.
 */
interface WalletRepository {
    /**
     * Generate a new HD wallet draft: a fresh BIP-39 mnemonic and the first derived
     * account address. **Nothing is persisted** — pass the draft to [createWallet]
     * once the user has acknowledged the recovery phrase (roadmap 2.0.2b, option B).
     */
    suspend fun generateWallet(): DataResult<WalletDraft>

    /**
     * Persist a previously generated [draft] as a new HD wallet.
     *
     * Returns the mnemonic words (for user backup) and derived address. The mnemonic is
     * encrypted and stored securely. Does **not** mark onboarding complete — that is
     * [completeOnboarding], called once the PIN is set.
     */
    suspend fun createWallet(
        walletName: String,
        draft: WalletDraft,
    ): DataResult<WalletCreationResult>

    /**
     * Import a wallet from a BIP-39 mnemonic phrase (12 or 24 words).
     *
     * Does not mark onboarding complete — see [completeOnboarding].
     */
    suspend fun importFromMnemonic(
        mnemonic: String,
        walletName: String,
    ): DataResult<WalletCreationResult>

    /**
     * Import a wallet from a raw private key (64 hex chars, with or without 0x prefix).
     *
     * Does not mark onboarding complete — see [completeOnboarding].
     */
    suspend fun importFromPrivateKey(
        privateKey: String,
        walletName: String,
    ): DataResult<WalletCreationResult>

    /**
     * Mark onboarding as finished: a wallet exists and the user has set a PIN.
     *
     * Writes the `is_wallet_set_up` flag the root router reads, records onboarding
     * completion, and unlocks the authentication session.
     */
    suspend fun completeOnboarding(): DataResult<Unit>

    /**
     * Get all wallets with their accounts.
     */
    fun getWallets(): Flow<List<Wallet>>

    /**
     * Get the currently active wallet with its accounts.
     */
    fun getActiveWallet(): Flow<Wallet?>

    /**
     * Set a wallet as the active wallet.
     */
    suspend fun setActiveWallet(walletId: String): DataResult<Unit>

    /**
     * Add a new derived account to an existing HD wallet.
     * Returns the new account with its address.
     *
     * **Authentication gate:** fails with an authentication error while the session is
     * locked — deriving the account requires decrypting the mnemonic.
     */
    suspend fun addAccount(walletId: String, accountName: String): DataResult<Account>

    /**
     * Get the active account's Ethereum address.
     */
    fun getActiveAddress(): Flow<String?>

    /**
     * Retrieve the mnemonic for backup display.
     *
     * **Authentication gate:** the caller must be in an unlocked session. The single
     * exception is onboarding, where the Verify screen has to read back the phrase it
     * just stored, so the call is allowed while `is_wallet_set_up` is still false.
     */
    suspend fun getMnemonicForBackup(walletId: String): DataResult<List<String>>

    /**
     * Delete a wallet and all associated data.
     */
    suspend fun deleteWallet(walletId: String): DataResult<Unit>

    /**
     * Delete all wallets and reset to clean state.
     */
    suspend fun deleteAllWallets(): DataResult<Unit>

    /**
     * Check if any wallet exists.
     */
    fun hasWallet(): Flow<Boolean>
}
