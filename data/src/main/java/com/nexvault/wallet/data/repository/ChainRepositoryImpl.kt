package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.datastore.preferences.UserPreferencesDataStore
import com.nexvault.wallet.data.mapper.ChainMapper
import com.nexvault.wallet.domain.model.chain.Chain
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.NexVaultException
import com.nexvault.wallet.domain.repository.ChainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChainRepositoryImpl @Inject constructor(
    private val userPreferences: UserPreferencesDataStore,
) : ChainRepository {
    private val hiddenChainIds = MutableStateFlow<Set<Int>>(emptySet())

    override fun getSupportedChains(): Flow<List<Chain>> {
        return MutableStateFlow(SupportedChains.all())
    }

    override fun getVisibleChains(): Flow<List<Chain>> {
        return hiddenChainIds.map { hidden ->
            SupportedChains.all().filter { it.chainId !in hidden }
        }
    }

    override fun getSelectedChain(): Flow<Chain> {
        return userPreferences.selectedNetwork.map { networkType ->
            val chainId = ChainMapper.networkTypeToChainId(networkType)
            SupportedChains.getByChainId(chainId) ?: SupportedChains.ETHEREUM_MAINNET
        }
    }

    override suspend fun setSelectedChain(chainId: Int): DataResult<Unit> {
        val chain = SupportedChains.getByChainId(chainId)
            ?: return DataResult.Error(NexVaultException("Unsupported chain ID: $chainId"))

        return try {
            val networkType = ChainMapper.chainIdToNetworkType(chainId)
            userPreferences.setSelectedNetwork(networkType)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e, "Failed to set selected chain")
        }
    }

    override fun getChainById(chainId: Int): Chain? {
        return SupportedChains.getByChainId(chainId)
    }

    override suspend fun setChainVisible(chainId: Int, visible: Boolean): DataResult<Unit> {
        return try {
            hiddenChainIds.value = if (visible) {
                hiddenChainIds.value - chainId
            } else {
                hiddenChainIds.value + chainId
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e, "Failed to update chain visibility")
        }
    }
}
