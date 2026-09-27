package com.nexvault.wallet.domain.usecase.chain

import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.repository.ChainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the list of supported blockchain networks.
 *
 * Used by the chain selector dropdown in the Home screen top bar.
 */
class GetSupportedChainsUseCase @Inject constructor(
    private val chainRepository: ChainRepository,
) {
    operator fun invoke(): Flow<List<Chain>> {
        return chainRepository.getSupportedChains()
    }
}
