package com.nexvault.wallet.domain.usecase.wallet

import com.nexvault.wallet.domain.model.auth.WalletCreationResult
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.wallet.WalletDraft
import com.nexvault.wallet.domain.repository.WalletRepository
import javax.inject.Inject

/**
 * Persists an already-generated [WalletDraft].
 *
 * Flow: encrypt & store the mnemonic → write wallet/account metadata → set the active
 * wallet → seed default tokens.
 *
 * Does **not** write `is_wallet_set_up`: onboarding completes only once the PIN is set
 * ([CompleteOnboardingUseCase]), so the root router cannot swap graphs mid-flow.
 */
class CreateWalletUseCase @Inject constructor(
    private val walletRepository: WalletRepository,
) {
    suspend operator fun invoke(
        walletName: String,
        draft: WalletDraft,
    ): DataResult<WalletCreationResult> {
        return walletRepository.createWallet(walletName, draft)
    }
}
