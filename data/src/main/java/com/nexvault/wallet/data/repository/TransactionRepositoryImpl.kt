package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.database.dao.TransactionDao
import com.nexvault.wallet.core.network.api.BlockExplorerApiFactory
import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.nexvault.wallet.data.mapper.toDomain
import com.nexvault.wallet.data.mapper.toEntity
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.transaction.GasEstimate
import com.nexvault.wallet.domain.model.transaction.SendTransactionParams
import com.nexvault.wallet.domain.model.transaction.Transaction
import com.nexvault.wallet.domain.repository.TransactionRepository
import com.nexvault.wallet.domain.repository.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

private val notImplementedSend =
    UnsupportedOperationException("Send flow is not implemented yet.")

private val notImplementedGas =
    UnsupportedOperationException("Gas estimation is not implemented yet.")

/**
 * [TransactionRepository] backed by Room and Etherscan-compatible explorers.
 *
 * Token detail and history refresh are implemented; send and gas estimation return explicit errors until later prompts.
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val blockExplorerApiFactory: BlockExplorerApiFactory,
    private val chainConfigProvider: ChainConfigProvider,
    private val walletRepository: WalletRepository,
) : TransactionRepository {

    override suspend fun estimateGas(
        fromAddress: String,
        toAddress: String,
        value: BigInteger,
        data: String?,
        chainId: Int,
    ): DataResult<GasEstimate> {
        return DataResult.Error(notImplementedGas, notImplementedGas.message)
    }

    override suspend fun sendNativeTransaction(
        params: SendTransactionParams,
        walletId: String,
        accountIndex: Int,
    ): DataResult<String> {
        return DataResult.Error(notImplementedSend, notImplementedSend.message)
    }

    override suspend fun sendTokenTransaction(
        params: SendTransactionParams,
        walletId: String,
        accountIndex: Int,
    ): DataResult<String> {
        return DataResult.Error(notImplementedSend, notImplementedSend.message)
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
            val native = api.getTransactions(address = address, apiKey = apiKey, offset = 100)
            val tokenTx = api.getTokenTransfers(address = address, apiKey = apiKey, offset = 100)
            val merged = buildList {
                if (native.isSuccess) {
                    addAll(native.result.map { it.toEntity(chainId, address) })
                }
                if (tokenTx.isSuccess) {
                    addAll(tokenTx.result.map { it.toEntity(chainId, address) })
                }
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
}
