package com.nexvault.wallet.domain.usecase

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.NexVaultException
import com.nexvault.wallet.domain.repository.ChainRepository
import com.nexvault.wallet.domain.usecase.chain.SetSelectedChainUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SetSelectedChainUseCaseTest {
    private lateinit var chainRepository: ChainRepository
    private lateinit var useCase: SetSelectedChainUseCase

    @Before
    fun setup() {
        chainRepository = mockk()
        useCase = SetSelectedChainUseCase(chainRepository)
    }

    @Test
    fun testWithValidChainIdCallsRepository() = runTest {
        coEvery { chainRepository.setSelectedChain(any()) } returns DataResult.Success(Unit)

        val result = useCase(1)

        assertTrue(result is DataResult.Success)
        coVerify { chainRepository.setSelectedChain(1) }
    }

    @Test
    fun testWithUnsupportedChainIdReturnsError() = runTest {
        val result = useCase(999)

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is NexVaultException)
        assertTrue(error.exception.message?.contains("Unsupported") == true)
    }

    @Test
    fun testWithEthereumSepoliaChainId() = runTest {
        coEvery { chainRepository.setSelectedChain(any()) } returns DataResult.Success(Unit)

        val result = useCase(11155111)

        assertTrue(result is DataResult.Success)
    }

    @Test
    fun testWithBscMainnetChainId() = runTest {
        coEvery { chainRepository.setSelectedChain(any()) } returns DataResult.Success(Unit)

        val result = useCase(56)

        assertTrue(result is DataResult.Success)
    }

    @Test
    fun testWithPolygonChainId() = runTest {
        coEvery { chainRepository.setSelectedChain(any()) } returns DataResult.Success(Unit)

        val result = useCase(137)

        assertTrue(result is DataResult.Success)
    }
}
