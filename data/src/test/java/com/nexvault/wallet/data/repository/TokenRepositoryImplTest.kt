package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.database.dao.TokenDao
import com.nexvault.wallet.core.network.api.CoinGeckoApi
import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.nexvault.wallet.core.network.config.ChainNetworkConfig
import com.nexvault.wallet.core.network.web3.Web3jProvider
import com.nexvault.wallet.domain.model.chain.SupportedChains
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.repository.ChainRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Roadmap 2.0.4: with the RPC key absent, the balance/metadata paths must fail with an explicit
 * [ApiKeyNotConfiguredException] and must never touch the network; with a key present they behave
 * as before.
 */
class TokenRepositoryImplTest {

    private lateinit var tokenDao: TokenDao
    private lateinit var coinGeckoApi: CoinGeckoApi
    private lateinit var web3jProvider: Web3jProvider
    private lateinit var chainRepository: ChainRepository
    private lateinit var chainConfigProvider: ChainConfigProvider
    private lateinit var repository: TokenRepositoryImpl

    private val mainnetConfig = ChainNetworkConfig(
        chainId = 1,
        rpcUrl = "https://mainnet.infura.io/v3/test-key",
        explorerApiBaseUrl = "https://api.etherscan.io/",
        explorerApiKey = "test-key",
        coinGeckoNativeCoinId = "ethereum",
        isTestnet = false,
    )

    @Before
    fun setup() {
        tokenDao = mockk(relaxed = true)
        coinGeckoApi = mockk(relaxed = true)
        web3jProvider = mockk(relaxed = true)
        chainRepository = mockk(relaxed = true)
        chainConfigProvider = mockk(relaxed = true)

        repository = TokenRepositoryImpl(
            tokenDao,
            coinGeckoApi,
            web3jProvider,
            chainRepository,
            chainConfigProvider,
        )
    }

    @Test
    fun refreshBalances_withoutRpcKey_failsWithoutCallingTheNetwork() = runTest {
        coEvery { chainRepository.getChainById(1) } returns SupportedChains.ETHEREUM_MAINNET
        every { chainConfigProvider.getConfig(1) } returns mainnetConfig
        every { chainConfigProvider.isRpcConfigured(1) } returns false

        val result = repository.refreshBalances(1, "0xabc")

        assertTrue(result is DataResult.Error)
        assertTrue((result as DataResult.Error).exception is ApiKeyNotConfiguredException)
        coVerify(exactly = 0) { web3jProvider.getWeb3j(any()) }
    }

    @Test
    fun refreshBalances_withRpcKey_reachesTheNetwork() = runTest {
        coEvery { chainRepository.getChainById(1) } returns SupportedChains.ETHEREUM_MAINNET
        every { chainConfigProvider.getConfig(1) } returns mainnetConfig
        every { chainConfigProvider.isRpcConfigured(1) } returns true

        repository.refreshBalances(1, "0xabc")

        coVerify(exactly = 1) { web3jProvider.getWeb3j(1) }
    }

    @Test
    fun refreshBalances_withoutRpcKey_onPublicRpcChain_isAllowed() = runTest {
        // Chains 56/137 use public endpoints: no key required, so the gate must not trip.
        coEvery { chainRepository.getChainById(56) } returns SupportedChains.BSC_MAINNET
        every { chainConfigProvider.getConfig(56) } returns mainnetConfig.copy(chainId = 56)
        every { chainConfigProvider.isRpcConfigured(56) } returns true

        repository.refreshBalances(56, "0xabc")

        coVerify(exactly = 1) { web3jProvider.getWeb3j(56) }
    }

    @Test
    fun addCustomToken_withoutRpcKey_failsWithoutCallingTheNetwork() = runTest {
        coEvery { chainRepository.getChainById(1) } returns SupportedChains.ETHEREUM_MAINNET
        every { chainConfigProvider.getConfig(1) } returns mainnetConfig
        every { chainConfigProvider.isRpcConfigured(1) } returns false

        val result = repository.addCustomToken(1, "0x0000000000000000000000000000000000000001")

        assertTrue(result is DataResult.Error)
        assertTrue((result as DataResult.Error).exception is ApiKeyNotConfiguredException)
        coVerify(exactly = 0) { web3jProvider.getWeb3j(any()) }
    }
}
