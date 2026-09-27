package com.nexvault.wallet.domain.usecase

import com.nexvault.wallet.domain.model.auth.AuthResult
import com.nexvault.wallet.domain.repository.AuthRepository
import com.nexvault.wallet.domain.usecase.auth.VerifyPinUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VerifyPinUseCaseTest {
    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: VerifyPinUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        useCase = VerifyPinUseCase(authRepository)
    }

    @Test
    fun testWithValidFormatDelegatesToRepository() = runTest {
        coEvery { authRepository.verifyPin(any()) } returns AuthResult.Success

        val result = useCase("123456")

        assertTrue(result is AuthResult.Success)
        coVerify { authRepository.verifyPin("123456") }
    }

    @Test
    fun testWithInvalidFormatReturnsFailedWithoutCallingRepository() = runTest {
        val result = useCase("12345")

        assertTrue(result is AuthResult.Failed)
        assertEquals("Invalid PIN format", (result as AuthResult.Failed).message)
        coVerify(exactly = 0) { authRepository.verifyPin(any()) }
    }

    @Test
    fun testWithNonDigitCharactersReturnsFailed() = runTest {
        val result = useCase("12345a")

        assertTrue(result is AuthResult.Failed)
        assertEquals("Invalid PIN format", (result as AuthResult.Failed).message)
    }
}
