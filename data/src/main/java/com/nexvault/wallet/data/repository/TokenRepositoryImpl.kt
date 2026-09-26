package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.database.dao.TokenDao
import com.nexvault.wallet.core.database.entity.TokenEntity
import com.nexvault.wallet.core.network.api.CoinGeckoApi
import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.nexvault.wallet.core.network.web3.Web3jProvider
import com.nexvault.wallet.data.mapper.toDomain
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.token.PricePoint
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.repository.ChainRepository
import com.nexvault.wallet.domain.repository.TokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Utf8String
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.generated.Uint8
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TokenRepository] backed by Room, Web3j on-chain reads, and CoinGecko pricing.
 */
@Singleton
class TokenRepositoryImpl @Inject constructor(
    private val tokenDao: TokenDao,
    private val coinGeckoApi: CoinGeckoApi,
    private val web3jProvider: Web3jProvider,
    private val chainRepository: ChainRepository,
    private val chainConfigProvider: ChainConfigProvider,
) : TokenRepository {

    private val refreshMutex = Mutex()

    override fun getTokensWithBalances(chainId: Int, address: String): Flow<List<Token>> {
        return tokenDao.getTokensByChain(chainId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeToken(chainId: Int, contractAddress: String): Flow<Token?> {
        return tokenDao.observeToken(contractAddress, chainId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun refreshBalances(chainId: Int, address: String): DataResult<Unit> {
        if (!refreshMutex.tryLock()) return DataResult.Success(Unit)
        return try {
            withContext(Dispatchers.IO) {
                val chain = chainRepository.getChainById(chainId)
                    ?: return@withContext DataResult.Error(
                        IllegalArgumentException("Unknown chain"),
                        "Unknown chain: $chainId",
                    )
                chainConfigProvider.getConfig(chainId)
                    ?: return@withContext DataResult.Error(
                        IllegalArgumentException("No network config"),
                        "Unsupported chain: $chainId",
                    )
                // Roadmap 2.0.4: fail explicitly instead of calling an RPC endpoint that has no key.
                if (!chainConfigProvider.isRpcConfigured(chainId)) {
                    return@withContext DataResult.Error(
                        ApiKeyNotConfiguredException("Ethereum RPC is not configured"),
                    )
                }

                val web3j = web3jProvider.getWeb3j(chainId)
                val wei = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
                    .send()
                    .balance
                val nativeDecimals = nativeDecimalsForChain(chainId)
                val nativeBalance = BigDecimal(wei)
                    .divide(BigDecimal.TEN.pow(nativeDecimals), nativeDecimals, RoundingMode.DOWN)

                val tracked = tokenDao.getTokensByChainOnce(chainId)
                val erc20Balances = linkedMapOf<String, BigDecimal>()
                for (token in tracked) {
                    if (token.contractAddress == Token.NATIVE_TOKEN_ADDRESS) continue
                    try {
                        val raw = callBalanceOf(web3j, token.contractAddress, address)
                        val bal = BigDecimal(raw).divide(
                            BigDecimal.TEN.pow(token.decimals),
                            token.decimals,
                            RoundingMode.DOWN,
                        )
                        erc20Balances[token.contractAddress] = bal
                    } catch (_: Exception) {
                        // keep cached balance on failure
                    }
                }

                val idSet = linkedSetOf<String>()
                idSet.add(chain.nativeCoinCoinGeckoId)
                tracked.mapNotNull { it.coinGeckoId }.forEach { idSet.add(it) }

                val priceMap = try {
                    val ids = idSet.joinToString(",")
                    coinGeckoApi.getTokenPrices(
                        ids = ids,
                        vsCurrencies = "usd",
                        include24hChange = true,
                    )
                } catch (_: Exception) {
                    emptyMap()
                }

                val now = System.currentTimeMillis()
                val updated = mutableListOf<TokenEntity>()

                val nativePriceData = priceMap[chain.nativeCoinCoinGeckoId]
                val nativePrice = nativePriceData?.usd
                val nativeFiat = nativePrice?.let { p -> nativeBalance.toDouble() * p }

                updated.add(
                    TokenEntity(
                        contractAddress = Token.NATIVE_TOKEN_ADDRESS,
                        chainId = chainId,
                        symbol = chain.symbol,
                        name = chain.nativeCoinName,
                        decimals = nativeDecimals,
                        logoUrl = null,
                        balance = nativeBalance.stripTrailingZeros().toPlainString(),
                        fiatPrice = nativePrice,
                        fiatValue = nativeFiat,
                        priceChange24h = nativePriceData?.usd24hChange,
                        isCustom = false,
                        coinGeckoId = chain.nativeCoinCoinGeckoId,
                        sortOrder = 0,
                        lastUpdated = now,
                    ),
                )

                for (token in tracked) {
                    if (token.contractAddress == Token.NATIVE_TOKEN_ADDRESS) continue
                    val balance = erc20Balances[token.contractAddress]
                        ?: BigDecimal(token.balance)
                    val pd = token.coinGeckoId?.let { priceMap[it] }
                    val price = pd?.usd ?: token.fiatPrice
                    val fiat = price?.let { balance.toDouble() * it } ?: token.fiatValue
                    updated.add(
                        token.copy(
                            balance = balance.stripTrailingZeros().toPlainString(),
                            fiatPrice = price,
                            fiatValue = fiat,
                            priceChange24h = pd?.usd24hChange ?: token.priceChange24h,
                            lastUpdated = now,
                        ),
                    )
                }

                tokenDao.upsertTokens(updated)
                DataResult.Success(Unit)
            }
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        } finally {
            refreshMutex.unlock()
        }
    }

    override suspend fun addCustomToken(
        chainId: Int,
        contractAddress: String,
    ): DataResult<Token> = withContext(Dispatchers.IO) {
        try {
            chainRepository.getChainById(chainId)
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("Unknown chain"),
                    "Unknown chain: $chainId",
                )
            chainConfigProvider.getConfig(chainId)
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("Unsupported chain"),
                    "Unsupported chain: $chainId",
                )
            // Roadmap 2.0.4: reading ERC-20 metadata needs the RPC key.
            if (!chainConfigProvider.isRpcConfigured(chainId)) {
                return@withContext DataResult.Error(
                    ApiKeyNotConfiguredException("Ethereum RPC is not configured"),
                )
            }

            val normalized = contractAddress.lowercase()
            val web3j = web3jProvider.getWeb3j(chainId)
            val name = callErc20Name(web3j, normalized)
            val symbol = callErc20Symbol(web3j, normalized)
            val decimals = callErc20Decimals(web3j, normalized)

            val entity = TokenEntity(
                contractAddress = normalized,
                chainId = chainId,
                symbol = symbol,
                name = name,
                decimals = decimals,
                logoUrl = null,
                balance = "0",
                fiatPrice = null,
                fiatValue = null,
                priceChange24h = null,
                isCustom = true,
                coinGeckoId = null,
                sortOrder = 100,
                lastUpdated = System.currentTimeMillis(),
            )
            tokenDao.upsertTokens(listOf(entity))
            DataResult.Success(entity.toDomain())
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        }
    }

    override suspend fun removeToken(
        chainId: Int,
        contractAddress: String,
    ): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            tokenDao.deleteToken(contractAddress, chainId)
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        }
    }

    override fun getTotalFiatValue(chainId: Int, address: String): Flow<Double> {
        return tokenDao.getTotalFiatValue(chainId).map { it ?: 0.0 }
    }

    override suspend fun getPortfolioChartData(
        chainId: Int,
        days: Int,
    ): DataResult<List<PricePoint>> = withContext(Dispatchers.IO) {
        try {
            val chain = chainRepository.getChainById(chainId)
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("Unknown chain"),
                    "Unknown chain: $chainId",
                )
            val response = coinGeckoApi.getPriceHistory(
                coinId = chain.nativeCoinCoinGeckoId,
                vsCurrency = "usd",
                days = daysToCoinGeckoDays(days),
            )
            val points = response.prices.map { row ->
                PricePoint(
                    timestamp = row[0].toLong(),
                    value = row[1],
                )
            }
            DataResult.Success(points)
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        }
    }

    override suspend fun getTokenDetail(
        chainId: Int,
        contractAddress: String,
        address: String,
    ): DataResult<Token> = withContext(Dispatchers.IO) {
        try {
            val entity = tokenDao.getToken(contractAddress, chainId)
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("Token not found"),
                    "Token not found",
                )
            DataResult.Success(entity.toDomain())
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        }
    }

    override suspend fun getTokenPriceChart(
        chainId: Int,
        contractAddress: String,
        days: Int,
    ): DataResult<List<PricePoint>> = withContext(Dispatchers.IO) {
        try {
            val entity = tokenDao.getToken(contractAddress, chainId)
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("Token not found"),
                    "Token not found",
                )
            val coinId = entity.coinGeckoId
                ?: chainRepository.getChainById(chainId)?.nativeCoinCoinGeckoId
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("No CoinGecko id"),
                    "No price data for this token",
                )
            val response = coinGeckoApi.getPriceHistory(
                coinId = coinId,
                vsCurrency = "usd",
                days = daysToCoinGeckoDays(days),
            )
            val points = response.prices.map { row ->
                PricePoint(timestamp = row[0].toLong(), value = row[1])
            }
            DataResult.Success(points)
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        }
    }

    override suspend fun seedDefaultTokens(chainId: Int): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            if (tokenDao.getTokenCountByChain(chainId) > 0) {
                return@withContext DataResult.Success(Unit)
            }
            val defaults = defaultTokensForChain(chainId)
            if (defaults.isNotEmpty()) {
                tokenDao.upsertTokens(defaults)
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        }
    }

    private fun nativeDecimalsForChain(chainId: Int): Int = when (chainId) {
        else -> 18
    }

    private fun daysToCoinGeckoDays(days: Int): String = when {
        days >= 365 -> "max"
        else -> days.coerceAtLeast(1).toString()
    }

    private fun callBalanceOf(
        web3j: org.web3j.protocol.Web3j,
        contractAddress: String,
        walletAddress: String,
    ): BigInteger {
        val function = Function(
            "balanceOf",
            listOf(Address(walletAddress)),
            listOf(object : TypeReference<Uint256>() {}),
        )
        val encoded = FunctionEncoder.encode(function)
        val response = web3j.ethCall(
            Transaction.createEthCallTransaction(
                ZERO_ADDRESS,
                contractAddress,
                encoded,
            ),
            DefaultBlockParameterName.LATEST,
        ).send()
        val hex = response.value ?: return BigInteger.ZERO
        if (hex == "0x" || hex.isEmpty()) return BigInteger.ZERO
        val decoded = FunctionReturnDecoder.decode(hex, function.outputParameters)
        if (decoded.isEmpty()) return BigInteger.ZERO
        return (decoded[0] as Uint256).value
    }

    private fun callErc20Name(web3j: org.web3j.protocol.Web3j, contract: String): String {
        val function = Function(
            "name",
            emptyList(),
            listOf(object : TypeReference<Utf8String>() {}),
        )
        return decodeStringCall(web3j, contract, function) ?: "Unknown"
    }

    private fun callErc20Symbol(web3j: org.web3j.protocol.Web3j, contract: String): String {
        val function = Function(
            "symbol",
            emptyList(),
            listOf(object : TypeReference<Utf8String>() {}),
        )
        return decodeStringCall(web3j, contract, function) ?: "???"
    }

    private fun callErc20Decimals(web3j: org.web3j.protocol.Web3j, contract: String): Int {
        val function = Function(
            "decimals",
            emptyList(),
            listOf(object : TypeReference<Uint8>() {}),
        )
        val encoded = FunctionEncoder.encode(function)
        val response = web3j.ethCall(
            Transaction.createEthCallTransaction(ZERO_ADDRESS, contract, encoded),
            DefaultBlockParameterName.LATEST,
        ).send()
        val hex = response.value ?: return 18
        if (hex == "0x" || hex.isEmpty()) return 18
        val decoded = FunctionReturnDecoder.decode(hex, function.outputParameters)
        if (decoded.isEmpty()) return 18
        return (decoded[0] as Uint8).value.toInt()
    }

    private fun decodeStringCall(
        web3j: org.web3j.protocol.Web3j,
        contract: String,
        function: Function,
    ): String? {
        val encoded = FunctionEncoder.encode(function)
        val response = web3j.ethCall(
            Transaction.createEthCallTransaction(ZERO_ADDRESS, contract, encoded),
            DefaultBlockParameterName.LATEST,
        ).send()
        val hex = response.value ?: return null
        if (hex == "0x" || hex.isEmpty()) return null
        val decoded = FunctionReturnDecoder.decode(hex, function.outputParameters)
        if (decoded.isEmpty()) return null
        return (decoded[0] as Utf8String).value
    }

    private fun defaultTokensForChain(chainId: Int): List<TokenEntity> {
        val now = System.currentTimeMillis()
        return when (chainId) {
            1 -> listOf(
                makeDefaultToken(1, "native", "ETH", "Ethereum", 18, "ethereum", 0, now),
                makeDefaultToken(1, "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", "USDC", "USD Coin", 6, "usd-coin", 1, now),
                makeDefaultToken(1, "0xdac17f958d2ee523a2206206994597c13d831ec7", "USDT", "Tether USD", 6, "tether", 2, now),
                makeDefaultToken(1, "0x6b175474e89094c44da98b954eedeac495271d0f", "DAI", "Dai Stablecoin", 18, "dai", 3, now),
                makeDefaultToken(1, "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2", "WETH", "Wrapped Ether", 18, "weth", 4, now),
                makeDefaultToken(1, "0x1f9840a85d5af5bf1d1762f925bdaddc4201f984", "UNI", "Uniswap", 18, "uniswap", 5, now),
                makeDefaultToken(1, "0x514910771af9ca656af840dff83e8264ecf986ca", "LINK", "Chainlink", 18, "chainlink", 6, now),
            )
            11155111 -> listOf(
                makeDefaultToken(11155111, "native", "ETH", "Sepolia ETH", 18, "ethereum", 0, now),
            )
            56 -> listOf(
                makeDefaultToken(56, "native", "BNB", "BNB", 18, "binancecoin", 0, now),
                makeDefaultToken(56, "0xe9e7cea3dedca5984780bafc599bd69add087d56", "BUSD", "Binance USD", 18, "binance-usd", 1, now),
                makeDefaultToken(56, "0x0e09fabb73bd3ade0a17ecc321fd13a19e81ce82", "CAKE", "PancakeSwap", 18, "pancakeswap-token", 2, now),
                makeDefaultToken(56, "0x55d398326f99059ff775485246999027b3197955", "USDT", "Tether USD", 18, "tether", 3, now),
            )
            137 -> listOf(
                makeDefaultToken(137, "native", "MATIC", "Polygon", 18, "matic-network", 0, now),
                makeDefaultToken(137, "0x2791bca1f2de4661ed88a30c99a7a9449aa84174", "USDC", "USD Coin", 6, "usd-coin", 1, now),
                makeDefaultToken(137, "0x7ceb23fd6bc0add59e62ac25578270cff1b9f619", "WETH", "Wrapped Ether", 18, "weth", 2, now),
            )
            else -> emptyList()
        }
    }

    private fun makeDefaultToken(
        chainId: Int,
        contractAddress: String,
        symbol: String,
        name: String,
        decimals: Int,
        coinGeckoId: String,
        sortOrder: Int,
        timestamp: Long,
    ): TokenEntity = TokenEntity(
        contractAddress = contractAddress,
        chainId = chainId,
        symbol = symbol,
        name = name,
        decimals = decimals,
        logoUrl = null,
        balance = "0",
        fiatPrice = null,
        fiatValue = null,
        priceChange24h = null,
        isCustom = false,
        coinGeckoId = coinGeckoId,
        sortOrder = sortOrder,
        lastUpdated = timestamp,
    )

    companion object {
        private const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
    }
}
