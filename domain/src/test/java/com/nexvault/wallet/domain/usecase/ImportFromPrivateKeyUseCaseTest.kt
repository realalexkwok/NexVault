package com.nexvault.wallet.domain.usecase

import com.nexvault.wallet.domain.model.auth.WalletCreationResult
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.InvalidPrivateKeyException
import com.nexvault.wallet.domain.repository.WalletRepository
import com.nexvault.wallet.domain.usecase.wallet.ImportFromPrivateKeyUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportFromPrivateKeyUseCaseTest {
    private lateinit var walletRepository: WalletRepository
    private lateinit var useCase: ImportFromPrivateKeyUseCase

    @Before
    fun setup() {
        walletRepository = mockk()
        useCase = ImportFromPrivateKeyUseCase(walletRepository)
    }

    private fun successResult() = DataResult.Success(
        WalletCreationResult(
            walletId = "id",
            address = "0x123",
            mnemonicWords = emptyList(),
        ),
    )

    @Test
    fun testWithValid64HexCallsRepository() = runTest {
        val privateKey = "a".repeat(64)
        coEvery { walletRepository.importFromPrivateKey(any(), any()) } returns successResult()

        val result = useCase(privateKey)

        assertTrue(result is DataResult.Success)
        coVerify { walletRepository.importFromPrivateKey(privateKey, "Imported Wallet") }
    }

    @Test
    fun testWith0xPrefixStripsPrefix() = runTest {
        coEvery { walletRepository.importFromPrivateKey(any(), any()) } returns successResult()

        val result = useCase("0x" + "a".repeat(64))

        assertTrue(result is DataResult.Success)
        coVerify { walletRepository.importFromPrivateKey("a".repeat(64), "Imported Wallet") }
    }

    @Test
    fun testWithInvalidLengthReturnsError() = runTest {
        val result = useCase("a".repeat(63))

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is InvalidPrivateKeyException)
    }

    @Test
    fun testWithNonHexCharsReturnsError() = runTest {
        val result = useCase("g".repeat(64))

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is InvalidPrivateKeyException)
    }
}
