package com.nexvault.wallet.domain.usecase.wallet

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.repository.WalletRepository
import javax.inject.Inject

/**
 * Marks onboarding as finished: a wallet exists **and** the user has set a PIN.
 *
 * Writes the `is_wallet_set_up` flag (which the root router reads), records the
 * onboarding completion preference, and unlocks the authentication session so the
 * post-onboarding screens may retrieve wallet material.
 *
 * Called by the Set PIN screen after the PIN has been stored (roadmap 2.0.2b, option A).
 */
class CompleteOnboardingUseCase
    @Inject
    constructor(
        private val walletRepository: WalletRepository,
    ) {
        suspend operator fun invoke(): DataResult<Unit> = walletRepository.completeOnboarding()
    }
