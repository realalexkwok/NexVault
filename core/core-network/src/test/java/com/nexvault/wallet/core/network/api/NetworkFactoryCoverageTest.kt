package com.nexvault.wallet.core.network.api

import com.google.common.truth.Truth.assertThat
import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.nexvault.wallet.core.network.config.ChainNetworkConfig
import com.nexvault.wallet.core.network.web3.Web3jProvider
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.web3j.protocol.Web3j

/**
 * Roadmap 2.6 coverage: the per-chain caching factories.
 */
class NetworkFactoryCoverageTest {
    private lateinit var chainConfigProvider: ChainConfigProvider
    private lateinit var web3jProvider: Web3jProvider
    private lateinit var blockExplorerApiFactory: BlockExplorerApiFactory

    @Before
    fun setup() {
        chainConfigProvider = mockk()
        web3jProvider = Web3jProvider(chainConfigProvider)
        blockExplorerApiFactory = BlockExplorerApiFactory(OkHttpClient(), mockk(), chainConfigProvider)
        every { chainConfigProvider.getConfig(1) } returns config("https://rpc.example.com", "https://api.example.com", "key-1")
    }

    @Test
    fun web3jProvider_cachesPerChain() {
        val first = web3jProvider.getWeb3j(1)
        val second = web3jProvider.getWeb3j(1)

        assertThat(first).isSameInstanceAs(second)
    }

    @Test
    fun web3jProvider_unsupportedChainThrows() {
        every { chainConfigProvider.getConfig(999) } returns null

        assertThrows(IllegalArgumentException::class.java) { web3jProvider.getWeb3j(999) }
    }

    @Test
    fun web3jProvider_shutdownClearsTheCache() {
        val instance = web3jProvider.getWeb3j(1)

        web3jProvider.shutdown(1)
        val afterShutdown = web3jProvider.getWeb3j(1)

        assertThat(afterShutdown).isNotSameInstanceAs(instance)
        web3jProvider.shutdownAll()
    }

    @Test
    fun explorerFactory_cachesAndThrowsForUnknownChains() {
        val first = blockExplorerApiFactory.getApi(1)
        val second = blockExplorerApiFactory.getApi(1)

        assertThat(first).isSameInstanceAs(second)
        every { chainConfigProvider.getConfig(999) } returns null
        assertThrows(IllegalArgumentException::class.java) { blockExplorerApiFactory.getApi(999) }
    }

    @Test
    fun explorerFactory_returnsTheConfiguredApiKey() {
        assertThat(blockExplorerApiFactory.getApiKey(1)).isEqualTo("key-1")
        every { chainConfigProvider.getConfig(999) } returns null
        assertThat(blockExplorerApiFactory.getApiKey(999)).isEmpty()
    }

    private fun config(rpcUrl: String, apiUrl: String, apiKey: String) =
        ChainNetworkConfig(
            chainId = 1,
            rpcUrl = rpcUrl,
            explorerApiBaseUrl = apiUrl,
            explorerApiKey = apiKey,
            coinGeckoNativeCoinId = "ethereum",
            isTestnet = false,
        )
}
