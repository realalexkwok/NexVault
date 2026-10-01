package com.nexvault.wallet.domain.usecase.transaction

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Reads one stored transaction with its full details (roadmap 2.8, history detail screen).
 */
class GetTransactionDetailUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(txHash: String, chainId: Int): DataResult<Transaction> =
        transactionRepository.getTransactionDetail(txHash, chainId)
}
