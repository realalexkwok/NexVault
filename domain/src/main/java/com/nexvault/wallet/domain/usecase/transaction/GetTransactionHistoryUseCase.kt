package com.nexvault.wallet.domain.usecase.transaction

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.WalletNotFoundException
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.repository.TransactionRepository
import com.nexvault.wallet.domain.repository.WalletRepository
import com.nexvault.wallet.domain.usecase.token.RefreshTransactionHistoryUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Reads one page of the stored transaction history for the active wallet (roadmap 2.8).
 *
 * The page arguments travel to the repository, which honours them — before 2.8 every call returned
 * the full stored list.
 */
class GetTransactionHistoryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val walletRepository: WalletRepository,
) {
    /**
     * @param chainId Chain to read.
     * @param page 1-based page number.
     * @param pageSize Rows per page.
     */
    suspend operator fun invoke(
        chainId: Int,
        page: Int = 1,
        pageSize: Int = RefreshTransactionHistoryUseCase.HISTORY_PAGE_SIZE,
    ): DataResult<List<Transaction>> {
        val address = activeAddress() ?: return DataResult.Error(WalletNotFoundException())
        return transactionRepository.getTransactionHistory(chainId, address, page, pageSize)
    }

    private suspend fun activeAddress(): String? {
        val wallet = walletRepository.getActiveWallet().first() ?: return null
        return wallet.accounts.find { it.isActive }?.address
    }
}
