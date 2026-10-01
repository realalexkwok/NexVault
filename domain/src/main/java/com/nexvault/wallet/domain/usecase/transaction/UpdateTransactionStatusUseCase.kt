package com.nexvault.wallet.domain.usecase.transaction

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Re-checks a pending transaction against the chain (roadmap 2.8).
 *
 * This is the caller for the repository's previously stubbed status refresh: until 2.8 it returned
 * the cached row without asking the explorer, so a mined transaction stayed pending forever.
 */
class UpdateTransactionStatusUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(txHash: String, chainId: Int): DataResult<Transaction> =
        transactionRepository.updateTransactionStatus(txHash, chainId)
}
