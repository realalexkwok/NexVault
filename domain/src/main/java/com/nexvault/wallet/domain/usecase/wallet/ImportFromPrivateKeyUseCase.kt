package com.nexvault.wallet.domain.usecase.wallet

import com.nexvault.wallet.domain.model.auth.WalletCreationResult
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.InvalidPrivateKeyException
import com.nexvault.wallet.domain.repository.WalletRepository
import javax.inject.Inject

/**
 * Imports a wallet from a raw private key (64 hex characters, optional 0x prefix).
 */
class ImportFromPrivateKeyUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
) {
    suspend operator fun invoke(
        privateKey: String,
        walletName: String = "Imported Wallet",
    ): DataResult<WalletCreationResult> {
        val cleaned = privateKey.trim().removePrefix("0x").removePrefix("0X")
        if (cleaned.length != 64 || !cleaned.all { it in "0123456789abcdefABCDEF" }) {
            return DataResult.Error(
                InvalidPrivateKeyException("Private key must be 64 hex characters"),
            )
        }
        return walletRepository.importFromPrivateKey(cleaned, walletName)
    }
}
