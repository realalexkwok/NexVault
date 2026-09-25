package com.nexvault.wallet.domain.usecase.wallet

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.wallet.WalletDraft
import com.nexvault.wallet.domain.repository.WalletRepository
import javax.inject.Inject

/**
 * Generates a new HD wallet draft: a fresh BIP-39 mnemonic plus the first derived
 * account address.
 *
 * **Nothing is persisted.** The draft must be handed to [CreateWalletUseCase] once the
 * user has acknowledged the recovery phrase (roadmap 2.0.2b, option B).
 */
class GenerateWalletDraftUseCase
    @Inject
    constructor(
        private val walletRepository: WalletRepository,
    ) {
        suspend operator fun invoke(): DataResult<WalletDraft> = walletRepository.generateWallet()
    }
