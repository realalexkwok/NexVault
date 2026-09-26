package com.nexvault.wallet.di

import com.nexvault.wallet.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

/**
 * Provides API key strings from BuildConfig to the NetworkModule.
 *
 * This module bridges the gap between BuildConfig (which is module-scoped)
 * and the NetworkModule (in the core-network module) which needs these
 * values via Hilt @Named qualifiers.
 *
 * API keys are read from local.properties and exposed via BuildConfig.
 * They are injected into NetworkModule which uses them to construct
 * RPC URLs and authenticate with block explorer APIs.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppNetworkModule {
    /**
     * Provides Infura API key for Ethereum RPC.
     */
    @Provides
    @Named("infura_api_key")
    fun provideInfuraApiKey(): String = BuildConfig.INFURA_API_KEY

    /**
     * Provides Alchemy API key for Ethereum RPC.
     */
    @Provides
    @Named("alchemy_api_key")
    fun provideAlchemyApiKey(): String = BuildConfig.ALCHEMY_API_KEY

    /**
     * Provides Etherscan API key for block explorer API.
     */
    @Provides
    @Named("etherscan_api_key")
    fun provideEtherscanApiKey(): String = BuildConfig.ETHERSCAN_API_KEY

    /**
     * Provides CoinGecko API key for price API.
     *
     * The free tier doesn't require a key. If not configured,
     * an empty string is returned which is handled by CoinGeckoApiKeyInterceptor.
     */
    @Provides
    @Named("coingecko_api_key")
    fun provideCoinGeckoApiKey(): String = BuildConfig.COINGECKO_API_KEY
}
