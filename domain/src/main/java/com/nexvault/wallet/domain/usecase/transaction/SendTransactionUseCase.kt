package com.nexvault.wallet.domain.usecase.transaction

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.transaction.SendTransactionParams
import com.nexvault.wallet.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Submits a signed transaction: native coin when [SendTransactionParams.tokenAddress] is null,
 * an ERC-20 transfer otherwise. Returns the transaction hash.
 *
 * Roadmap 2.6.
 */
class SendTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        params: SendTransactionParams,
        walletId: String,
        accountIndex: Int,
    ): DataResult<String> {
        return if (params.tokenAddress == null) {
            transactionRepository.sendNativeTransaction(params, walletId, accountIndex)
        } else {
            transactionRepository.sendTokenTransaction(params, walletId, accountIndex)
        }
    }
}
