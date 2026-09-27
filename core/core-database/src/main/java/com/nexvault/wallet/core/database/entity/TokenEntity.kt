/*
 * Copyright (c) 2025 NexVault
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexvault.wallet.core.database.entity

import androidx.room.Entity

/**
 * Room entity for cryptocurrency tokens tracked by the user.
 *
 * Each token is uniquely identified by its contract address + chain ID.
 * Native coins (ETH, BNB, MATIC) use the special contract address "native".
 *
 * Ref: doc/05-IMPLEMENTATION-PLAN-PHASE2.md Task 2.2.1
 *
 * @property contractAddress The token's contract address, or "native" for native coins
 * @property chainId The blockchain chain ID (1 = Ethereum, 56 = BSC, etc.)
 * @property symbol Token ticker symbol (e.g., "ETH", "USDC")
 * @property name Full token name (e.g., "Ethereum", "USD Coin")
 * @property decimals Number of decimal places (usually 18 for ETH, 6 for USDC)
 * @property logoUrl URL for the token's icon image (nullable for custom tokens)
 * @property balance Token balance as a String to preserve BigDecimal precision
 * @property fiatPrice Current price in user's fiat currency
 * @property fiatValue Total fiat value (balance x fiatPrice)
 * @property priceChange24h 24-hour price change percentage
 * @property isCustom Whether this token was manually added by the user
 * @property coinGeckoId CoinGecko API identifier for price lookups
 * @property sortOrder Display sort order (lower = higher in list)
 * @property lastUpdated Timestamp of the last data refresh (epoch millis)
 */
@Entity(
    tableName = "tokens",
    primaryKeys = ["contractAddress", "chainId"],
)
data class TokenEntity(
    val contractAddress: String,
    val chainId: Int,
    val symbol: String,
    val name: String,
    val decimals: Int,
    val logoUrl: String?,
    val balance: String,
    val fiatPrice: Double?,
    val fiatValue: Double?,
    val priceChange24h: Double?,
    val isCustom: Boolean,
    val coinGeckoId: String?,
    val sortOrder: Int,
    val lastUpdated: Long,
)
