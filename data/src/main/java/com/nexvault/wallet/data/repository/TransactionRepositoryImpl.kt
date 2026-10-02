package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.database.dao.TokenDao
import com.nexvault.wallet.core.database.dao.TransactionDao
import com.nexvault.wallet.core.database.entity.TokenEntity
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
            val inputError = validateSendInputs(params, tokenContractAddress)
            if (inputError != null) return@withContext inputError

            val wallet = walletRepository.getWallets().first().firstOrNull { it.id == walletId }
                ?: return@withContext DataResult.Error(WalletNotFoundException())
            val account = wallet.accounts.getOrNull(accountIndex)
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException("Unknown account index: $accountIndex"),
                )
            val token = tokenContractAddress?.let { tokenDao.getToken(it, params.chainId) }

            val payload = buildSendPayload(params, account.address, token)
            val balanceError = validateSendBalances(params, account.address, token, payload)
            if (balanceError != null) return@withContext balanceError

            val hash = submitSignedTransaction(params, wallet, account, token, payload)
            persistPendingTransaction(params, account.address, token, payload, hash)
            DataResult.Success(hash)
        } catch (e: AuthenticationException) {
            DataResult.Error(e, e.message)
        } catch (e: Exception) {
            DataResult.Error(e, e.message ?: "Send failed")
        }
    }

    private suspend fun validateSendInputs(
        params: SendTransactionParams,
        tokenContractAddress: String?,
    ): DataResult<String>? {
        if (!chainConfigProvider.isRpcConfigured(params.chainId)) {
            return DataResult.Error(ApiKeyNotConfiguredException("Ethereum RPC is not configured"))
        }
        if (!isValidAddress(params.toAddress)) {
            return DataResult.Error(InvalidAddressException())
        }
        if (tokenContractAddress != null && tokenDao.getToken(tokenContractAddress, params.chainId) == null) {
            return DataResult.Error(IllegalArgumentException("Token not found on this chain"))
        }
        return null
    }

    private suspend fun buildSendPayload(
        params: SendTransactionParams,
        fromAddress: String,
        token: TokenEntity?,
    ): SendPayload {
        val decimals = token?.decimals ?: NATIVE_DECIMALS
        val valueWei = params.amount.movePointRight(decimals).toBigInteger()
        val data = token?.let { encodeTransfer(params.toAddress, valueWei) }
        val gasPrice = params.gasOption.gasPrice
        val gasLimit =
            rpcClient.estimateGas(
                from = fromAddress,
                to = token?.contractAddress ?: params.toAddress,
                value = if (token == null) valueWei else BigInteger.ZERO,
                data = data,
                chainId = params.chainId,
            )
        return SendPayload(valueWei, data, gasPrice, gasLimit)
    }

    private suspend fun validateSendBalances(
        params: SendTransactionParams,
        fromAddress: String,
        token: TokenEntity?,
        payload: SendPayload,
    ): DataResult<String>? {
        // Roadmap 2.6 / TC-REPO-004: validate the balance before building the transaction.
        val balance =
            if (token == null) {
                rpcClient.getBalance(fromAddress, params.chainId)
            } else {
                rpcClient.getTokenBalance(token.contractAddress, fromAddress, params.chainId)
            }
        val fee = payload.gasLimit.multiply(payload.gasPrice)
        val required = if (token == null) payload.valueWei.add(fee) else payload.valueWei
        if (balance < required) {
            return DataResult.Error(InsufficientBalanceException(balance.toString(), required.toString()))
        }
        // The fee is always paid in the native coin — for ERC-20 sends the native balance must
        // cover it separately.
        if (token != null && rpcClient.getBalance(fromAddress, params.chainId) < fee) {
            return DataResult.Error(InsufficientBalanceException("native fee check failed", fee.toString()))
        }
        return null
    }

    private suspend fun submitSignedTransaction(
        params: SendTransactionParams,
        wallet: Wallet,
        account: Account,
        token: TokenEntity?,
        payload: SendPayload,
    ): String {
        val nonce = rpcClient.getTransactionCount(account.address, params.chainId)
        val credentials = signingCredentials(wallet, account)
        val rawTransaction =
            if (token == null) {
                RawTransaction.createEtherTransaction(
                    nonce,
                    payload.gasPrice,
                    payload.gasLimit,
                    params.toAddress,
                    payload.valueWei,
                )
            } else {
                RawTransaction.createTransaction(
                    nonce,
                    payload.gasPrice,
                    payload.gasLimit,
                    token.contractAddress,
                    BigInteger.ZERO,
                    payload.data,
                )
            }
        val signed = TransactionEncoder.signMessage(rawTransaction, params.chainId.toLong(), credentials)
        return rpcClient.sendRawTransaction(Numeric.toHexString(signed), params.chainId)
    }

    private suspend fun persistPendingTransaction(
        params: SendTransactionParams,
        fromAddress: String,
        token: TokenEntity?,
        payload: SendPayload,
        hash: String,
    ) {
        transactionDao.upsertTransactions(
            listOf(
                TransactionEntity(
                    txHash = hash,
                    chainId = params.chainId,
                    fromAddress = fromAddress,
                    toAddress = params.toAddress,
                    value = params.amount.toPlainString(),
                    gasUsed = null,
                    gasPrice = payload.gasPrice.toString(),
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
    }

    private data class SendPayload(
        val valueWei: BigInteger,
        val data: String?,
        val gasPrice: BigInteger,
        val gasLimit: BigInteger,
    )

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
     * One page of the stored history, newest first (roadmap 2.8 shortcut fix).
     *
     * The page arguments used to be ignored; they now map onto the DAO's limit/offset query so the
     * screen can grow the list as the user scrolls without re-reading every stored row.
     */
    override suspend fun getTransactionHistory(
        chainId: Int,
        address: String,
        page: Int,
        pageSize: Int,
    ): DataResult<List<Transaction>> = withContext(Dispatchers.IO) {
        try {
            val safePageSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)
            // Clamped before the multiplication: an absurd page would otherwise overflow the Int
            // and hand the DAO a negative offset.
            val safePage = page.coerceIn(1, MAX_PAGE_NUMBER)
            val entities =
                transactionDao.getTransactions(
                    chainId = chainId,
                    address = address,
                    limit = safePageSize,
                    offset = (safePage - 1) * safePageSize,
                )
            DataResult.Success(entities.map { it.toDomain(address) })
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        }
    }

    override suspend fun refreshTransactionHistory(
        chainId: Int,
        address: String,
        page: Int,
        pageSize: Int,
    ): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            // Roadmap 2.0.4: fail explicitly instead of calling an explorer API with no key.
            if (!chainConfigProvider.isExplorerConfigured(chainId)) {
                return@withContext DataResult.Error(
                    ApiKeyNotConfiguredException(EXPLORER_NOT_CONFIGURED_MESSAGE),
                )
            }
            val api = blockExplorerApiFactory.getApi(chainId)
            val apiKey = blockExplorerApiFactory.getApiKey(chainId)
            // Roadmap 2.0.4b: Etherscan V2 requires the chain id on every call.
            val native =
                api.getTransactions(
                    chainId = chainId,
                    address = address,
                    apiKey = apiKey,
                    page = page.coerceAtLeast(1),
                    offset = pageSize.coerceIn(1, MAX_PAGE_SIZE),
                )
            // Roadmap 2.0.4b: a rejected envelope (bad key, plan gate, …) is an error with the
            // explorer's own message — never a silently empty history.
            native.errorOrNull()?.let { return@withContext DataResult.Error(it, it.message) }
            val tokenTx =
                api.getTokenTransfers(
                    chainId = chainId,
                    address = address,
                    apiKey = apiKey,
                    page = page.coerceAtLeast(1),
                    offset = pageSize.coerceIn(1, MAX_PAGE_SIZE),
                )
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
    ): DataResult<Transaction> = resolveTransactionStatus(txHash, chainId)

    override fun getPendingTransactions(chainId: Int, address: String): Flow<List<Transaction>> {
        return transactionDao.observeTransactions(chainId, address).map { entities ->
            entities.filter { it.status == 0 }.map { it.toDomain(address) }
        }
    }

    override suspend fun updateTransactionStatus(
        txHash: String,
        chainId: Int,
    ): DataResult<Transaction> = resolveTransactionStatus(txHash, chainId)

    /**
     * Loads a stored transaction and, when it is still pending, reconciles its status with the
     * chain before returning the row (roadmap 2.8 shortcut fix). Shared by the detail query and
     * the explicit status refresh so the chain-check logic cannot drift between the two callers.
     */
    private suspend fun resolveTransactionStatus(
        txHash: String,
        chainId: Int,
    ): DataResult<Transaction> = withContext(Dispatchers.IO) {
        try {
            val address = walletRepository.getActiveAddress().first()
                ?: return@withContext DataResult.Error(
                    IllegalStateException(NO_ACTIVE_WALLET_MESSAGE),
                    NO_ACTIVE_WALLET_MESSAGE,
                )
            val entity = transactionDao.getTransaction(txHash, chainId)
                ?: return@withContext DataResult.Error(
                    IllegalArgumentException(TRANSACTION_NOT_FOUND_MESSAGE),
                    TRANSACTION_NOT_FOUND_MESSAGE,
                )
            // Roadmap 2.8 shortcut fix: a settled row is returned as-is, a pending one is checked
            // against the chain instead of trusting the cached status.
            if (entity.status != STATUS_PENDING) {
                return@withContext DataResult.Success(entity.toDomain(address))
            }
            if (!chainConfigProvider.isExplorerConfigured(chainId)) {
                return@withContext DataResult.Error(
                    ApiKeyNotConfiguredException(EXPLORER_NOT_CONFIGURED_MESSAGE),
                )
            }
            val api = blockExplorerApiFactory.getApi(chainId)
            val receipt =
                api.getTxReceiptStatus(
                    chainId = chainId,
                    txHash = txHash,
                    apiKey = blockExplorerApiFactory.getApiKey(chainId),
                )
            receipt.errorOrNull()?.let { return@withContext DataResult.Error(it, it.message) }
            val confirmed = receipt.receiptSucceeded
            // No receipt yet: the transaction is still in the mempool, so it stays pending.
            if (confirmed != null) {
                transactionDao.updateTransactionStatus(
                    txHash = txHash,
                    chainId = chainId,
                    status = if (confirmed) STATUS_CONFIRMED else STATUS_FAILED,
                    gasUsed = entity.gasUsed,
                )
            }
            val updated = transactionDao.getTransaction(txHash, chainId) ?: entity
            DataResult.Success(updated.toDomain(address))
        } catch (e: Exception) {
            DataResult.Error(e, e.message)
        }
    }

    private companion object {
        const val NATIVE_DECIMALS = 18
        const val MAX_PAGE_SIZE = 100
        const val MAX_PAGE_NUMBER = 10_000
        const val EXPLORER_NOT_CONFIGURED_MESSAGE = "Block explorer API is not configured"
        const val STATUS_PENDING = 0
        const val STATUS_CONFIRMED = 1
        const val STATUS_FAILED = 2
        const val NO_ACTIVE_WALLET_MESSAGE = "No active wallet"
        const val TRANSACTION_NOT_FOUND_MESSAGE = "Transaction not found"
    }
}
