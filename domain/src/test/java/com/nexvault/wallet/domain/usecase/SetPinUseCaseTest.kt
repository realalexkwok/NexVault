package com.nexvault.wallet.domain.usecase

import com.nexvault.wallet.domain.model.common.AuthenticationException
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.repository.AuthRepository
import com.nexvault.wallet.domain.usecase.auth.SetPinUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SetPinUseCaseTest {
    private lateinit var authRepository: AuthRepository
    private lateinit var useCase: SetPinUseCase

    @Before
    fun setup() {
        authRepository = mockk()
        useCase = SetPinUseCase(authRepository)
    }

    @Test
    fun testWithValid6DigitPinCallsRepository() = runTest {
        coEvery { authRepository.setPin(any()) } returns DataResult.Success(Unit)

        val result = useCase("123456")

        assertTrue(result is DataResult.Success)
        coVerify { authRepository.setPin("123456") }
    }

    @Test
    fun testWith5DigitPinReturnsError() = runTest {
        val result = useCase("12345")

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is AuthenticationException)
    }

    @Test
    fun testWith7DigitPinReturnsError() = runTest {
        val result = useCase("1234567")

        assertTrue(result is DataResult.Error)
    }

    @Test
    fun testWithAlphabeticPinReturnsError() = runTest {
        val result = useCase("abcdef")

        assertTrue(result is DataResult.Error)
    }
}
