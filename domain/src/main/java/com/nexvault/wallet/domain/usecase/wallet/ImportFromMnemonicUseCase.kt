package com.nexvault.wallet.domain.usecase.wallet

import com.nexvault.wallet.domain.model.auth.WalletCreationResult
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.InvalidMnemonicException
import com.nexvault.wallet.domain.repository.WalletRepository
import javax.inject.Inject

/**
 * Imports a wallet from a BIP-39 mnemonic phrase (12 or 24 words).
 */
class ImportFromMnemonicUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
) {
    suspend operator fun invoke(
        mnemonic: String,
        walletName: String = "Imported Wallet",
    ): DataResult<WalletCreationResult> {
        val trimmed = mnemonic.trim().lowercase()
        val words = trimmed.split("\\s+".toRegex())
        if (words.size != 12 && words.size != 24) {
            return DataResult.Error(
                InvalidMnemonicException("Mnemonic must be 12 or 24 words, got ${words.size}"),
            )
        }
        return walletRepository.importFromMnemonic(trimmed, walletName)
    }
}
