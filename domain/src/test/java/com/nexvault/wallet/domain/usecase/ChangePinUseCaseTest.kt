package com.nexvault.wallet.domain.usecase

import com.nexvault.wallet.domain.model.common.AuthenticationException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.repository.AuthRepository
import com.nexvault.wallet.domain.usecase.auth.ChangePinUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChangePinUseCaseTest {
    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: ChangePinUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        useCase = ChangePinUseCase(authRepository)
    }

    @Test
    fun testWithValidCurrentAndNewPinsCallsRepository() = runTest {
        coEvery { authRepository.changePin(any(), any()) } returns DataResult.Success(Unit)

        val result = useCase("123456", "654321")

        assertTrue(result is DataResult.Success)
        coVerify { authRepository.changePin("123456", "654321") }
    }

    @Test
    fun testWithSameCurrentAndNewPinReturnsError() = runTest {
        val result = useCase("123456", "123456")

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is AuthenticationException)
        assertTrue(error.exception.message?.contains("different") == true)
    }

    @Test
    fun testWithInvalidNewPinFormatReturnsError() = runTest {
        val result = useCase("123456", "12345")

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is AuthenticationException)
    }

    @Test
    fun testWithNonDigitNewPinReturnsError() = runTest {
        val result = useCase("123456", "abcdef")

        assertTrue(result is DataResult.Error)
    }
}
