package com.nexvault.wallet.data.repository

import com.nexvault.wallet.core.database.dao.TransactionDao
import com.nexvault.wallet.core.network.api.BlockExplorerApiFactory
import com.nexvault.wallet.core.network.config.ChainConfigProvider
import com.nexvault.wallet.domain.model.common.ApiKeyNotConfiguredException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.repository.WalletRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Roadmap 2.0.4: with the explorer key absent, the history refresh must fail with an explicit
 * [ApiKeyNotConfiguredException] and must never touch the network.
 */
class TransactionRepositoryImplTest {

    private lateinit var transactionDao: TransactionDao
    private lateinit var blockExplorerApiFactory: BlockExplorerApiFactory
    private lateinit var chainConfigProvider: ChainConfigProvider
    private lateinit var walletRepository: WalletRepository
    private lateinit var repository: TransactionRepositoryImpl

    @Before
    fun setup() {
        transactionDao = mockk(relaxed = true)
        blockExplorerApiFactory = mockk(relaxed = true)
        chainConfigProvider = mockk(relaxed = true)
        walletRepository = mockk(relaxed = true)

        repository = TransactionRepositoryImpl(
            transactionDao,
            blockExplorerApiFactory,
            chainConfigProvider,
            walletRepository,
        )
    }

    @Test
    fun refreshTransactionHistory_withoutExplorerKey_failsWithoutCallingTheNetwork() = runTest {
        every { chainConfigProvider.isExplorerConfigured(1) } returns false

        val result = repository.refreshTransactionHistory(1, "0xabc")

        assertTrue(result is DataResult.Error)
        assertTrue((result as DataResult.Error).exception is ApiKeyNotConfiguredException)
        coVerify(exactly = 0) { blockExplorerApiFactory.getApi(any()) }
        coVerify(exactly = 0) { blockExplorerApiFactory.getApiKey(any()) }
    }

    @Test
    fun refreshTransactionHistory_withExplorerKey_callsTheExplorer() = runTest {
        every { chainConfigProvider.isExplorerConfigured(1) } returns true
        every { blockExplorerApiFactory.getApi(1) } returns mockk(relaxed = true)
        every { blockExplorerApiFactory.getApiKey(1) } returns "test-key"

        repository.refreshTransactionHistory(1, "0xabc")

        coVerify(exactly = 1) { blockExplorerApiFactory.getApi(1) }
    }
}
