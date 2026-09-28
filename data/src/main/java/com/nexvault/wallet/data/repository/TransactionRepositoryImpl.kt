package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.database.dao.TokenDao
import com.nexvault.wallet.core.database.dao.TransactionDao
import com.nexvault.wallet.core.database.entity.TransactionEntity
import com.nexvault.wallet.core.network.api.BlockExplorerApiFactory
import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.nexvault.wallet.core.network.rpc.ChainRpcClient
import com.nexvault.wallet.core.security.mnemonic.MnemonicManager
import com.nexvault.wallet.core.security.util.SecureUtils.secureWipe
import com.nexvault.wallet.core.security.wallet.HDKeyManager
import com.nexvault.wallet.core.security.wallet.WalletStore
import com.nexvault.wallet.data.mapper.toDomain
import com.nexvault.wallet.data.mapper.toEntity
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.AuthenticationException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.InsufficientBalanceException
import com.nexvault.wallet.domain.model.common.InvalidAddressException
import com.nexvault.wallet.domain.model.common.WalletNotFoundException
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.model.transaction.GasEstimate
import com.nexvault.wallet.domain.model.transaction.GasOption
import com.nexvault.wallet.domain.model.transaction.SendTransactionParams
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.model.wallet.Account
import com.nexvault.wallet.domain.model.wallet.Wallet
import com.nexvault.wallet.domain.model.wallet.WalletType
import com.nexvault.wallet.domain.repository.TransactionRepository
import com.nexvault.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [TransactionRepository] backed by Room, Etherscan-compatible explorers and the chain RPC seam.
 *
 * Roadmap 2.6: the read paths were already real; `estimateGas` and the two send paths are now
 * implemented against [ChainRpcClient] (mockable in tests — web3j itself is final). Both send paths
 * validate the recipient and the balance, sign with the account's key, submit the raw transaction,
 * persist the pending row, and wipe the derived key bytes immediately after signing (the full
 * zeroing verification is 4.7's TC-SECTEST-002).
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val tokenDao: TokenDao,
    private val blockExplorerApiFactory: BlockExplorerApiFactory,
    private val chainConfigProvider: ChainConfigProvider,
    private val walletRepository: WalletRepository,
    private val rpcClient: ChainRpcClient,
    private val walletStore: WalletStore,
    private val mnemonicManager: MnemonicManager,
    private val hdKeyManager: HDKeyManager,
) : TransactionRepository {
    override suspend fun estimateSendGas(
        fromAddress: String,
        toAddress: String,
        amount: BigDecimal,
        tokenAddress: String?,
        chainId: Int,
    ): DataResult<GasEstimate> = withContext(Dispatchers.IO) {
        try {
            // Roadmap 2.0.4: fail explicitly instead of calling an RPC endpoint that has no key.
            if (!chainConfigProvider.isRpcConfigured(chainId)) {
                return@withContext DataResult.Error(
                    ApiKeyNotConfiguredException("Ethereum RPC is not configured"),
                )
            }
            val token = tokenAddress?.let { address ->
                tokenDao.getToken(address, chainId)
                    ?: return@withContext DataResult.Error(
                        IllegalArgumentException("Token not found on this chain"),
                    )
            }
            val decimals = token?.decimals ?: NATIVE_DECIMALS
            val valueWei = amount.movePointRight(decimals).toBigInteger()
            val data = token?.let { encodeTransfer(toAddress, valueWei) }
            val gasPrice = rpcClient.getGasPrice(chainId)
            val gasLimit =
                rpcClient.estimateGas(
                    from = fromAddress,
                    to = token?.contractAddress ?: toAddress,
                    value = if (token == null) valueWei else BigInteger.ZERO,
                    data = data,
                    chainId = chainId,
                )
            val nativeFiatPrice = tokenDao.getToken(Token.NATIVE_TOKEN_ADDRESS, chainId)?.fiatPrice
            DataResult.Success(
                GasEstimate(
                    slow = gasOption(gasPrice, gasLimit, nativeFiatPrice, "~10 min", 600, 8),
                    normal = gasOption(gasPrice, gasLimit, nativeFiatPrice, "~3 min", 180, 10),
                    fast = gasOption(gasPrice, gasLimit, nativeFiatPrice, "~30 sec", 30, 13),
                    gasLimit = gasLimit,
                ),
            )
        } catch (e: Exception) {
            DataResult.Error(e, "Gas estimation failed: ${e.message}")
        }
    }

    override suspend fun sendNativeTransaction(
        params: SendTransactionParams,
        walletId: String,
        accountIndex: Int,
    ): DataResult<String> = sendTransaction(params, walletId, accountIndex, tokenContractAddress = null)

    override suspend fun sendTokenTransaction(
        params: SendTransactionParams,
        walletId: String,
        accountIndex: Int,
    ): DataResult<String> {
        val tokenAddress = params.tokenAddress
            ?: return DataResult.Error(
                IllegalArgumentException("Token address is required for a token transfer"),
            )
        return sendTransaction(params, walletId, accountIndex, tokenContractAddress = tokenAddress)
    }

    private suspend fun sendTransaction(
        params: SendTransactionParams,
        walletId: String,
        accountIndex: Int,
        tokenContractAddress: String?,
    ): DataResult<String> = withContext(Dispatchers.IO) {
        try {
            if (!chainConfigProvider.isRpcConfigured(params.chainId)) {
                return@withContext DataResult.Error(
                    ApiKeyNotConfiguredException("Ethereum RPC is not configured"),
                )
            }
            if (!isValidAddress(params.toAddress)) {
                return@withContext DataResult.Error(InvalidAddressException())
            }
            val wallet = walletRepository.getWallets().first().firstOrNull { it.id == walletId }
                ?: return@withContext DataResult.Error(WalletNotFoundException())
            val account = wallet.accounts.getOrNull(accountIndex)
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("Unknown account index: $accountIndex"),
                )
            val token = tokenContractAddress?.let { address ->
                tokenDao.getToken(address, params.chainId)
                    ?: return@withContext DataResult.Error(
                        IllegalArgumentException("Token not found on this chain"),
                    )
            }
            val decimals = token?.decimals ?: NATIVE_DECIMALS
            val valueWei = params.amount.movePointRight(decimals).toBigInteger()

            val data = token?.let { encodeTransfer(params.toAddress, valueWei) }
            val gasPrice = params.gasOption.gasPrice
            val gasLimit =
                rpcClient.estimateGas(
                    from = account.address,
                    to = token?.contractAddress ?: params.toAddress,
                    value = if (token == null) valueWei else BigInteger.ZERO,
                    data = data,
                    chainId = params.chainId,
                )

            // Roadmap 2.6 / TC-REPO-004: validate the balance before building the transaction.
            val balance =
                if (token == null) {
                    rpcClient.getBalance(account.address, params.chainId)
                } else {
                    rpcClient.getTokenBalance(token.contractAddress, account.address, params.chainId)
                }
            val required = if (token == null) valueWei.add(gasLimit.multiply(gasPrice)) else valueWei
            if (balance < required) {
                return@withContext DataResult.Error(
                    InsufficientBalanceException(
                        available = balance.toString(),
                        required = required.toString(),
                    ),
                )
            }
            // The fee is always paid in the native coin — for ERC-20 sends the native balance
            // must cover it separately.
            if (token != null && rpcClient.getBalance(account.address, params.chainId) < gasLimit.multiply(gasPrice)) {
                return@withContext DataResult.Error(
                    InsufficientBalanceException(
                        available = "native fee check failed",
                        required = gasLimit.multiply(gasPrice).toString(),
                    ),
                )
            }

            val nonce = rpcClient.getTransactionCount(account.address, params.chainId)
            val credentials = signingCredentials(wallet, account)
            val rawTransaction =
                if (token == null) {
                    RawTransaction.createEtherTransaction(
                        nonce,
                        gasPrice,
                        gasLimit,
                        params.toAddress,
                        valueWei,
                    )
                } else {
                    RawTransaction.createTransaction(
                        nonce,
                        gasPrice,
                        gasLimit,
                        token.contractAddress,
                        BigInteger.ZERO,
                        data,
                    )
                }
            val signed = TransactionEncoder.signMessage(rawTransaction, params.chainId.toLong(), credentials)
            val hash = rpcClient.sendRawTransaction(Numeric.toHexString(signed), params.chainId)

            transactionDao.upsertTransactions(
                listOf(
                    TransactionEntity(
                        txHash = hash,
                        chainId = params.chainId,
                        fromAddress = account.address,
                        toAddress = params.toAddress,
                        value = params.amount.toPlainString(),
                        gasUsed = null,
                        gasPrice = gasPrice.toString(),
                        tokenSymbol = token?.symbol,
                        tokenContractAddress = token?.contractAddress,
                        tokenDecimals = token?.decimals,
                        blockNumber = 0L,
                        timestamp = System.currentTimeMillis() / 1000,
                        status = 0,
                        type = "send",
                    ),
                ),
            )
            DataResult.Success(hash)
        } catch (e: AuthenticationException) {
            DataResult.Error(e, e.message)
        } catch (e: Exception) {
            DataResult.Error(e, e.message ?: "Send failed")
        }
    }

    private suspend fun signingCredentials(wallet: Wallet, account: Account): Credentials {
        val keyPair =
            if (wallet.type == WalletType.HD) {
                val words =
                    when (val result = walletRepository.getMnemonicForBackup(wallet.id)) {
                        is DataResult.Success -> result.data
                        is DataResult.Error -> throw result.exception
                    }
                val seed = mnemonicManager.mnemonicToSeed(words.joinToString(" "), "")
                hdKeyManager.deriveEthereumKeyPair(seed, account.index, 0)
            } else {
                val keyBytes = walletStore.retrievePrivateKey(account.address)
                ECKeyPair.create(keyBytes).also { keyBytes.secureWipe() }
            }
        return Credentials.create(keyPair).also {
            // Best-effort hygiene: the derived key bytes must not linger (verification: 4.7).
            hdKeyManager.getPrivateKeyBytes(keyPair).secureWipe()
        }
    }

    private fun gasOption(
        gasPrice: BigInteger,
        gasLimit: BigInteger,
        nativeFiatPrice: Double?,
        timeName: String,
        timeSeconds: Int,
        multiplierTenths: Int,
    ): GasOption {
        val tierPrice = gasPrice.multiply(BigInteger.valueOf(multiplierTenths.toLong())).divide(BigInteger.TEN)
        return GasOption(
            gasPrice = tierPrice,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,
            estimatedTimeSeconds = timeSeconds,
            estimatedTimeName = timeName,
            fiatCost = fiatCost(gasLimit, tierPrice, nativeFiatPrice),
        )
    }

    private fun fiatCost(gasLimit: BigInteger, gasPrice: BigInteger, nativeFiatPrice: Double?): Double? {
        if (nativeFiatPrice == null) return null
        return BigDecimal(gasLimit.multiply(gasPrice))
            .movePointLeft(NATIVE_DECIMALS)
            .multiply(BigDecimal.valueOf(nativeFiatPrice))
            .toDouble()
    }

    private fun encodeTransfer(to: String, valueWei: BigInteger): String {
        val function =
            Function(
                "transfer",
                listOf(Address(to), Uint256(valueWei)),
                listOf(object : TypeReference<Address>() {}, object : TypeReference<Uint256>() {}),
            )
        return FunctionEncoder.encode(function)
    }

    private fun isValidAddress(address: String): Boolean {
        return address.startsWith("0x") &&
            address.length == 42 &&
            address.substring(2).all { it in "0123456789abcdefABCDEF" }
    }

    /**
     * Emits the full observed list for [address] whenever Room updates.
     * [page] and [pageSize] are reserved for future true pagination; callers receive the full list for now.
     */
    override fun getTransactionHistory(
        chainId: Int,
        address: String,
        page: Int,
        pageSize: Int,
    ): Flow<List<Transaction>> {
        return transactionDao.observeTransactions(chainId, address).map { entities ->
            entities.map { it.toDomain(address) }
        }
    }

    override suspend fun refreshTransactionHistory(
        chainId: Int,
        address: String,
    ): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Roadmap 2.0.4: fail explicitly instead of calling an explorer API with no key.
            if (!chainConfigProvider.isExplorerConfigured(chainId)) {
                return@withContext DataResult.Error(
                    ApiKeyNotConfiguredException("Block explorer API is not configured"),
                )
            }
            val api = blockExplorerApiFactory.getApi(chainId)
            val apiKey = blockExplorerApiFactory.getApiKey(chainId)
            // Roadmap 2.0.4b: Etherscan V2 requires the chain id on every call.
            val native = api.getTransactions(chainId = chainId, address = address, apiKey = apiKey, offset = 100)
            // Roadmap 2.0.4b: a rejected envelope (bad key, plan gate, …) is an error with the
            // explorer's own message — never a silently empty history.
            native.errorOrNull()?.let { return@withContext DataResult.Error(it, it.message) }
            val tokenTx = api.getTokenTransfers(chainId = chainId, address = address, apiKey = apiKey, offset = 100)
            tokenTx.errorOrNull()?.let { return@withContext DataResult.Error(it, it.message) }
            val merged = buildList {
                addAll(native.result.orEmpty().map { it.toEntity(chainId, address) })
                addAll(tokenTx.result.orEmpty().map { it.toEntity(chainId, address) })
            }
            if (merged.isNotEmpty()) {
                transactionDao.upsertTransactions(merged)
            }
            DataResult.Success(Unit)
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        }
    }

    override suspend fun getRecentTransactionsForToken(
        chainId: Int,
        address: String,
        tokenContractAddress: String?,
        limit: Int,
    ): List<Transaction> {
        val entities = transactionDao.getRecentTransactionsForToken(
            chainId = chainId,
            address = address,
            tokenContractAddress = tokenContractAddress,
            limit = limit,
        )
        return entities.map { it.toDomain(address) }
    }

    override suspend fun getTransactionDetail(
        txHash: String,
        chainId: Int,
    ): DataResult<Transaction> = withContext(Dispatchers.IO) {
        try {
            val address = walletRepository.getActiveAddress().first()
                ?: return@withContext DataResult.Error(
                    IllegalStateException("No active wallet"),
                    "No active wallet",
                )
            val entity = transactionDao.getTransaction(txHash, chainId)
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("Transaction not found"),
                    "Transaction not found",
                )
            DataResult.Success(entity.toDomain(address))
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        }
    }

    override fun getPendingTransactions(chainId: Int, address: String): Flow<List<Transaction>> {
        return transactionDao.observeTransactions(chainId, address).map { entities ->
            entities.filter { it.status == 0 }.map { it.toDomain(address) }
        }
    }

    override suspend fun updateTransactionStatus(
        txHash: String,
        chainId: Int,
    ): DataResult<Transaction> = withContext(Dispatchers.IO) {
        try {
            val address = walletRepository.getActiveAddress().first()
                ?: return@withContext DataResult.Error(
                    IllegalStateException("No active wallet"),
                    "No active wallet",
                )
            val entity = transactionDao.getTransaction(txHash, chainId)
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("Transaction not found"),
                    "Transaction not found",
                )
            DataResult.Success(entity.toDomain(address))
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        }
    }

    private companion object {
        const val NATIVE_DECIMALS = 18
    }
}
