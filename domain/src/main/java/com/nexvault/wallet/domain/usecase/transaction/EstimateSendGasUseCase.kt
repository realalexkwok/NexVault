package com.nexvault.wallet.domain.usecase.transaction

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.transaction.GasEstimate
import com.nexvault.wallet.domain.repository.TransactionRepository
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Estimates the gas options (slow / normal / fast) for a prospective send.
 *
 * Roadmap 2.6: features talk to the repository through this use case, so the send form never
 * reaches into `data` and never touches ABI encoding — the repository resolves the token's
 * decimals and calldata itself.
 */
class EstimateSendGasUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(
        fromAddress: String,
        toAddress: String,
        amount: BigDecimal,
        tokenAddress: String?,
        chainId: Int,
    ): DataResult<GasEstimate> {
        return transactionRepository.estimateSendGas(fromAddress, toAddress, amount, tokenAddress, chainId)
    }
}
