package com.nexvault.wallet.core.network.api

import com.nexvault.wallet.core.network.dto.PriceHistoryResponse
import com.nexvault.wallet.core.network.dto.TokenPriceResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * CoinGecko API service for cryptocurrency prices and market data.
 *
 * Base URL: https://api.coingecko.com/api/v3/
 *
 * Free tier rate limit: ~10-30 requests/minute.
 * Ref: doc/05-IMPLEMENTATION-PLAN-PHASE2.md Task 2.1.3
 */
interface CoinGeckoApi {
    /**
     * Fetches current prices for multiple tokens in the specified currency.
     *
     * @param ids Comma-separated CoinGecko coin IDs (e.g., "ethereum,uniswap,usd-coin")
     * @param vsCurrencies Target currency (e.g., "usd")
     * @param include24hChange Whether to include 24h price change percentage
     * @return Map of coin ID to price data
     *
     * Example: getTokenPrices("ethereum,uniswap", "usd", true)
     * Response: {"ethereum": {"usd": 1650.42, "usd_24h_change": 2.34}, ...}
     */
    @GET("simple/price")
    suspend fun getTokenPrices(
        @Query("ids") ids: String,
        @Query("vs_currencies") vsCurrencies: String = "usd",
        @Query("include_24hr_change") include24hChange: Boolean = true,
    ): Map<String, TokenPriceResponse>

    /**
     * Fetches historical price data for charting.
     *
     * @param coinId CoinGecko coin ID (e.g., "ethereum")
     * @param vsCurrency Target currency (e.g., "usd")
     * @param days Number of days (1, 7, 30, 90, 365, "max")
     * @return Price history with timestamp-value pairs
     */
    @GET("coins/{id}/market_chart")
    suspend fun getPriceHistory(
        @Path("id") coinId: String,
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("days") days: String,
    ): PriceHistoryResponse
}
