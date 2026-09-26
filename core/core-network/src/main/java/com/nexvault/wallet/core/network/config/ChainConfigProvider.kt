package com.nexvault.wallet.core.network.config

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides network configurations for all supported blockchain chains.
 *
 * API keys are injected via Hilt from the app module's BuildConfig.
 * Each chain has its own RPC URL, explorer API endpoint, and CoinGecko ID.
 *
 * Supported chains:
 * - Ethereum Mainnet (chainId=1)
 * - Ethereum Sepolia testnet (chainId=11155111)
 * - BNB Smart Chain (chainId=56)
 * - Polygon (chainId=137)
 *
 * Roadmap 2.0.4b: every explorer call goes to Etherscan API **V2**
 * (`https://api.etherscan.io/v2/`) and carries a mandatory `chainid`; the per-chain
 * BscScan/PolygonScan hosts of V1 are gone. V2 uses one key for every chain, so all four
 * chains share [etherscanApiKey]. Plan coverage is a separate axis: with the free plan the
 * key is configured but chain 56 answers NOTOK (see `ExplorerPlanUnsupportedException`).
 *
 * @param infuraApiKey Infura API key for Ethereum RPC
 * @param alchemyApiKey Alchemy API key for Ethereum RPC (alternative to Infura)
 * @param etherscanApiKey Etherscan API V2 key for the block explorer API (all chains)
 */
@Singleton
class ChainConfigProvider @Inject constructor(
    private val infuraApiKey: String,
    private val alchemyApiKey: String,
    private val etherscanApiKey: String,
) {
    companion object {
        /** Chains whose RPC endpoint requires an injected API key (explorer keys are separate). */
        private val KEYED_RPC_CHAIN_IDS = setOf(1, 11155111)

        /** Etherscan API V2 serves every supported chain from this single host. */
        private const val ETHERSCAN_V2_BASE_URL = "https://api.etherscan.io/v2/"
    }

    /**
     * Returns the network config for a given chain ID.
     *
     * @param chainId The chain ID to look up
     * @return The chain configuration, or null if the chain is not supported
     */
    fun getConfig(chainId: Int): ChainNetworkConfig? = configs[chainId]

    /**
     * Returns all supported chain configurations.
     */
    fun getAllConfigs(): List<ChainNetworkConfig> = configs.values.toList()

    /**
     * Whether the RPC endpoint for [chainId] is usable in this build.
     *
     * Ethereum mainnet and Sepolia are served by Infura, so they are unusable without
     * [infuraApiKey]; BNB Smart Chain and Polygon use public endpoints and never need a key.
     *
     * Used by the repositories to fail with an explicit "not configured" error instead of
     * attempting a call that cannot succeed (roadmap 2.0.4).
     */
    fun isRpcConfigured(chainId: Int): Boolean {
        if (!configs.containsKey(chainId)) return false
        return chainId !in KEYED_RPC_CHAIN_IDS || infuraApiKey.isNotBlank()
    }

    /**
     * Whether the block-explorer API for [chainId] is usable in this build.
     *
     * Under Etherscan V2 (roadmap 2.0.4b) every explorer call is keyed, so the single
     * [etherscanApiKey] gates all supported chains — BSC and Polygon included, which were
     * keyless under V1. This is a pure key check: whether the *plan* covers a chain is a
     * runtime answer from the explorer (chain 56 is plan-gated on the free tier) and is not
     * encoded here.
     */
    fun isExplorerConfigured(chainId: Int): Boolean {
        if (!configs.containsKey(chainId)) return false
        return etherscanApiKey.isNotBlank()
    }

    private val configs: Map<Int, ChainNetworkConfig> = mapOf(
        1 to ChainNetworkConfig(
            chainId = 1,
            rpcUrl = "https://mainnet.infura.io/v3/$infuraApiKey",
            explorerApiBaseUrl = ETHERSCAN_V2_BASE_URL,
            explorerApiKey = etherscanApiKey,
            coinGeckoNativeCoinId = "ethereum",
            isTestnet = false,
        ),
        11155111 to ChainNetworkConfig(
            chainId = 11155111,
            rpcUrl = "https://sepolia.infura.io/v3/$infuraApiKey",
            explorerApiBaseUrl = ETHERSCAN_V2_BASE_URL,
            explorerApiKey = etherscanApiKey,
            coinGeckoNativeCoinId = "ethereum",
            isTestnet = true,
        ),
        56 to ChainNetworkConfig(
            chainId = 56,
            rpcUrl = "https://bsc-dataseed.binance.org/",
            explorerApiBaseUrl = ETHERSCAN_V2_BASE_URL,
            explorerApiKey = etherscanApiKey,
            coinGeckoNativeCoinId = "binancecoin",
            isTestnet = false,
        ),
        137 to ChainNetworkConfig(
            chainId = 137,
            rpcUrl = "https://polygon-rpc.com/",
            explorerApiBaseUrl = ETHERSCAN_V2_BASE_URL,
            explorerApiKey = etherscanApiKey,
            coinGeckoNativeCoinId = "matic-network",
            isTestnet = false,
        ),
    )
}
