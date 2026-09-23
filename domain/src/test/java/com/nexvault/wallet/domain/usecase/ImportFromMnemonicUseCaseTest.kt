package com.nexvault.wallet.domain.usecase

import com.nexvault.wallet.domain.model.auth.WalletCreationResult
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.InvalidMnemonicException
import com.nexvault.wallet.domain.repository.WalletRepository
import com.nexvault.wallet.domain.usecase.wallet.ImportFromMnemonicUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportFromMnemonicUseCaseTest {
    private lateinit var walletRepository: WalletRepository
    private lateinit var useCase: ImportFromMnemonicUseCase

    @Before
    fun setup() {
        walletRepository = mockk()
        useCase = ImportFromMnemonicUseCase(walletRepository)
    }

    private fun successResult() = DataResult.Success(
        WalletCreationResult(
            walletId = "id",
            address = "0x123",
            mnemonicWords = emptyList(),
        ),
    )

    @Test
    fun testWithValid12WordsCallsRepository() = runTest {
        val mnemonic = "abandon " + "about " + "absent " + "abstract " + "absent " +
            "abstract " + "absent " + "abstract " + "absent " + "abstract " +
            "absent " + "absent"
        coEvery { walletRepository.importFromMnemonic(any(), any()) } returns successResult()

        val result = useCase(mnemonic, "Test Wallet")

        assertTrue(result is DataResult.Success)
        coVerify { walletRepository.importFromMnemonic(mnemonic.lowercase(), "Test Wallet") }
    }

    @Test
    fun testWith10WordsReturnsError() = runTest {
        val mnemonic = "abandon " + "about " + "absent " + "abstract " + "absent " +
            "abstract " + "absent " + "abstract " + "absent " + "about"

        val result = useCase(mnemonic)

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is InvalidMnemonicException)
    }

    @Test
    fun testWith24WordsCallsRepository() = runTest {
        val mnemonic = List(24) { "word$it" }.joinToString(" ")
        coEvery { walletRepository.importFromMnemonic(any(), any()) } returns successResult()

        val result = useCase(mnemonic)

        assertTrue(result is DataResult.Success)
    }
}
