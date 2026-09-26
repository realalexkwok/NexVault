package com.nexvault.wallet.core.network.di

import android.content.Context
import com.nexvault.wallet.core.network.adapter.BigDecimalAdapter
import com.nexvault.wallet.core.network.adapter.BigIntegerAdapter
import com.nexvault.wallet.core.network.adapter.EtherscanEnvelopeAdapterFactory
import com.nexvault.wallet.core.network.api.BlockExplorerApiFactory
import com.nexvault.wallet.core.network.api.CoinGeckoApi
import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.nexvault.wallet.core.network.interceptor.CacheControlInterceptor
import com.nexvault.wallet.core.network.interceptor.CoinGeckoApiKeyInterceptor
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module providing all networking dependencies.
 *
 * This module provides:
 * - OkHttpClient with logging, caching, and interceptors
 * - Moshi instance with custom adapters
 * - CoinGeckoApi Retrofit service
 * - BlockExplorerApiFactory for per-chain explorer APIs
 * - ChainConfigProvider for chain network configuration
 *
 * API keys are injected via @Named qualifiers from AppNetworkModule.
 *
 * Ref: doc/02-ARCHITECTURE-AND-TECH-STACK.md Section 3 - DI Graph
 * Ref: doc/05-IMPLEMENTATION-PLAN-PHASE2.md Task 2.1.5
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ================================================================
    // Moshi
    // ================================================================

    /**
     * Provides Moshi instance with BigDecimal, BigInteger and explorer-envelope adapters.
     *
     * BigDecimalAdapter handles both JSON numbers and strings to prevent
     * precision loss for large decimal values like token amounts.
     *
     * BigIntegerAdapter handles both decimal and hex strings (0x prefix)
     * commonly used in Ethereum APIs.
     *
     * EtherscanEnvelopeAdapterFactory reads the V2 list envelopes leniently, so a rejection body
     * (a String in `result`) parses instead of blowing up the whole response (roadmap 2.0.4b).
     */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(BigDecimalAdapter())
            .add(BigIntegerAdapter())
            .add(EtherscanEnvelopeAdapterFactory())
            .build()
    }

    // ================================================================
    // OkHttp
    // ================================================================

    /**
     * Provides HTTP cache for OkHttpClient.
     *
     * Uses 10MB cache in the app's cache directory.
     */
    @Provides
    @Singleton
    fun provideHttpCache(@ApplicationContext context: Context): Cache {
        val cacheDir = File(context.cacheDir, "http_cache")
        return Cache(cacheDir, 10L * 1024L * 1024L) // 10MB
    }

    /**
     * Provides HTTP logging interceptor.
     *
     * Uses BASIC level by default. The app module can override this
     * if debug-specific logging is needed.
     */
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    /**
     * Provides the base OkHttpClient.
     *
     * Configured with:
     * - 30 second timeouts
     * - 10MB HTTP cache
     * - Logging interceptor
     * - Cache control interceptor for network responses
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        cache: Cache,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor(CacheControlInterceptor())
            .build()
    }

    // ================================================================
    // Chain Configuration
    // ================================================================

    /**
     * Provides ChainConfigProvider with API keys injected from app module.
     */
    @Provides
    @Singleton
    fun provideChainConfigProvider(
        @Named("infura_api_key") infuraApiKey: String,
        @Named("alchemy_api_key") alchemyApiKey: String,
        @Named("etherscan_api_key") etherscanApiKey: String,
    ): ChainConfigProvider {
        return ChainConfigProvider(
            infuraApiKey = infuraApiKey,
            alchemyApiKey = alchemyApiKey,
            etherscanApiKey = etherscanApiKey,
        )
    }

    // ================================================================
    // CoinGecko API
    // ================================================================

    /**
     * Provides CoinGeckoApi Retrofit service.
     *
     * Uses a separate OkHttpClient with the API key interceptor added.
     */
    @Provides
    @Singleton
    fun provideCoinGeckoApi(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        @Named("coingecko_api_key") coinGeckoApiKey: String,
    ): CoinGeckoApi {
        val client = okHttpClient.newBuilder()
            .addInterceptor(CoinGeckoApiKeyInterceptor(coinGeckoApiKey))
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CoinGeckoApi::class.java)
    }

    // ================================================================
    // Block Explorer API Factory
    // ================================================================

    /**
     * Provides BlockExplorerApiFactory for creating per-chain explorer APIs.
     *
     * Each chain has a different base URL (Etherscan, BscScan, PolygonScan),
     * so this factory creates and caches separate Retrofit instances.
     */
    @Provides
    @Singleton
    fun provideBlockExplorerApiFactory(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        chainConfigProvider: ChainConfigProvider,
    ): BlockExplorerApiFactory {
        return BlockExplorerApiFactory(
            okHttpClient = okHttpClient,
            moshi = moshi,
            chainConfigProvider = chainConfigProvider,
        )
    }
}
