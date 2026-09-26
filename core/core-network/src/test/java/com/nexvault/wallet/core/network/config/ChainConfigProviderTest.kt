package com.nexvault.wallet.core.network.config

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Roadmap 2.0.4b: every supported chain talks to Etherscan **V2** with the same key, and
 * `isExplorerConfigured` requires that key for all four chains — BSC and Polygon included, which
 * were keyless under V1. `isRpcConfigured` keeps the 2.0.4 rule (Infura for 1/11155111, public
 * endpoints for 56/137).
 */
class ChainConfigProviderTest {
    private val keyed =
        ChainConfigProvider(
            infuraApiKey = "infura-key",
            alchemyApiKey = "alchemy-key",
            etherscanApiKey = "etherscan-key",
        )

    private val keyless =
        ChainConfigProvider(
            infuraApiKey = "",
            alchemyApiKey = "",
            etherscanApiKey = "",
        )

    @Test
    fun everySupportedChainUsesTheSingleV2Host() {
        assertThat(keyed.getAllConfigs()).hasSize(4)

        keyed.getAllConfigs().forEach { config ->
            assertThat(config.explorerApiBaseUrl).isEqualTo("https://api.etherscan.io/v2/")
        }
    }

    @Test
    fun everySupportedChainCarriesTheSameExplorerKey() {
        keyed.getAllConfigs().forEach { config ->
            assertThat(config.explorerApiKey).isEqualTo("etherscan-key")
        }
    }

    @Test
    fun explorerIsConfiguredForAllChainsOnlyWithAKey() {
        SUPPORTED_CHAIN_IDS.forEach { chainId ->
            assertThat(keyed.isExplorerConfigured(chainId)).isTrue()
            assertThat(keyless.isExplorerConfigured(chainId)).isFalse()
        }
    }

    @Test
    fun rpcConfigurationKeepsTheKeylessBscAndPolygonRule() {
        assertThat(keyless.isRpcConfigured(1)).isFalse()
        assertThat(keyless.isRpcConfigured(11155111)).isFalse()
        assertThat(keyless.isRpcConfigured(56)).isTrue()
        assertThat(keyless.isRpcConfigured(137)).isTrue()
        assertThat(keyed.isRpcConfigured(1)).isTrue()
        assertThat(keyed.isRpcConfigured(11155111)).isTrue()
    }

    @Test
    fun unknownChainIsNotReportedAsConfigured() {
        assertThat(keyed.getConfig(10)).isNull()
        assertThat(keyed.isExplorerConfigured(10)).isFalse()
        assertThat(keyed.isRpcConfigured(10)).isFalse()
    }

    private companion object {
        val SUPPORTED_CHAIN_IDS = listOf(1, 11155111, 56, 137)
    }
}
