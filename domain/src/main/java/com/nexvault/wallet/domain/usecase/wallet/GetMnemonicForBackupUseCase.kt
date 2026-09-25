package com.nexvault.wallet.domain.usecase.wallet

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.repository.WalletRepository
import javax.inject.Inject

/**
 * Retrieves the mnemonic for backup display.
 *
 * The repository enforces the authentication gate: the session must be unlocked, except
 * during onboarding (while `is_wallet_set_up` is still false), where the Verify screen
 * reads back the phrase it has just stored.
 */
class GetMnemonicForBackupUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
) {
    suspend operator fun invoke(walletId: String): DataResult<List<String>> {
        return walletRepository.getMnemonicForBackup(walletId)
    }
}
