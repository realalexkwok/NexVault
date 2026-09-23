package com.nexvault.wallet.domain.usecase.token

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.WalletNotFoundException
import com.nexvault.wallet.domain.repository.ChainRepository
import com.nexvault.wallet.domain.repository.TokenRepository
import com.nexvault.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Refreshes on-chain balances and fiat prices for the current chain and active address.
 */
class RefreshBalancesUseCase @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val chainRepository: ChainRepository,
    private val walletRepository: WalletRepository,
) {
    suspend operator fun invoke(): DataResult<Unit> {
        val chain = chainRepository.getSelectedChain().first()
        val address = walletRepository.getActiveAddress().first()
            ?: return DataResult.Error(WalletNotFoundException())

        // Default tokens must exist per chain: seed the selected chain before refreshing so a
        // chain switch (or the first refresh on a newly selected chain) has tokens to refresh.
        // Seeding is idempotent and best-effort — a seeding failure must not turn a successful
        // balance refresh into a failure; the next refresh retries it.
        tokenRepository.seedDefaultTokens(chain.chainId)

        return tokenRepository.refreshBalances(chain.chainId, address)
    }
}
