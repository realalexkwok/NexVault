package com.nexvault.wallet.domain.usecase.token

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.WalletNotFoundException
import com.nexvault.wallet.domain.repository.TransactionRepository
import com.nexvault.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Refreshes cached explorer transactions for the active wallet on a chain.
 */
class RefreshTransactionHistoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
) {
    /**
     * @param chainId Chain to refresh.
     * @return the repository's result, so callers can surface the standing conditions
     *         (missing API key, explorer plan gate) as persistent notices.
     */
    suspend operator fun invoke(chainId: Int): DataResult<Unit> {
        val wallet = walletRepository.getActiveWallet().first() ?: return DataResult.Error(WalletNotFoundException())
        val address =
            wallet.accounts.find { it.isActive }?.address ?: return DataResult.Error(WalletNotFoundException())
        return transactionRepository.refreshTransactionHistory(chainId, address)
    }
}
