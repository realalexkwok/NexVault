package com.nexvault.wallet.core.network.api

import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory that creates and caches BlockExplorerApi Retrofit instances per chain.
 *
 * Roadmap 2.0.4b: every supported chain now shares the single Etherscan V2 base URL, so the cache
 * is no longer about different hosts — it keeps one Retrofit instance per chain id and keeps the
 * per-chain base URL lookup in one place.
 *
 * Example:
 * ```
 * val api = blockExplorerApiFactory.getApi(chainId = 1)
 * val txs = api.getTransactions(chainId = 1, address = "0x...", apiKey = "...")
 * ```
 */
@Singleton
class BlockExplorerApiFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val chainConfigProvider: ChainConfigProvider,
) {
    private val cache = ConcurrentHashMap<Int, BlockExplorerApi>()

    /**
     * Returns a BlockExplorerApi instance for the given chain.
     *
     * Caches instances to avoid recreating Retrofit for the same chain.
     *
     * @param chainId The chain ID
     * @return BlockExplorerApi configured for that chain's explorer
     * @throws IllegalArgumentException if the chain is not supported
     */
    fun getApi(chainId: Int): BlockExplorerApi {
        return cache.getOrPut(chainId) {
            val config = chainConfigProvider.getConfig(chainId)
                ?: throw IllegalArgumentException("Unsupported chain ID: $chainId")

            Retrofit.Builder()
                .baseUrl(config.explorerApiBaseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(BlockExplorerApi::class.java)
        }
    }

    /**
     * Convenience method that returns the explorer API key for a chain.
     *
     * Call this alongside getApi() when making requests that need the key.
     */
    fun getApiKey(chainId: Int): String {
        return chainConfigProvider.getConfig(chainId)?.explorerApiKey ?: ""
    }
}
