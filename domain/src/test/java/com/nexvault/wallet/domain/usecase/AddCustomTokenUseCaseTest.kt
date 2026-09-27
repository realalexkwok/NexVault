package com.nexvault.wallet.domain.usecase

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.common.InvalidAddressException
import com.nexvault.wallet.domain.model.token.Token
import com.nexvault.wallet.domain.repository.TokenRepository
import com.nexvault.wallet.domain.usecase.token.AddCustomTokenUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class AddCustomTokenUseCaseTest {
    private lateinit var tokenRepository: TokenRepository
    private lateinit var useCase: AddCustomTokenUseCase

    @Before
    fun setup() {
        tokenRepository = mockk()
        useCase = AddCustomTokenUseCase(tokenRepository)
    }

    @Test
    fun testWithValid0xAddressCallsRepository() = runTest {
        val contractAddress = "0x1234567890123456789012345678901234567890"
        coEvery {
            tokenRepository.addCustomToken(any(), any())
        } returns DataResult.Success(
            Token(
                contractAddress = contractAddress,
                chainId = 1,
                symbol = "TEST",
                name = "Test Token",
                decimals = 18,
                logoUrl = null,
                balance = BigDecimal.ZERO,
                fiatPrice = null,
                fiatValue = null,
                priceChange24h = null,
            ),
        )

        val result = useCase(1, contractAddress)

        assertTrue(result is DataResult.Success)
        coVerify { tokenRepository.addCustomToken(1, contractAddress) }
    }

    @Test
    fun testWithAddressMissing0xPrefixReturnsError() = runTest {
        val contractAddress = "1234567890123456789012345678901234567890"

        val result = useCase(1, contractAddress)

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is InvalidAddressException)
    }

    @Test
    fun testWithAddressWrongLengthReturnsError() = runTest {
        val contractAddress = "0x123456789012345678901234567890123456"

        val result = useCase(1, contractAddress)

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is InvalidAddressException)
    }

    @Test
    fun testWithNonHexCharactersReturnsError() = runTest {
        val contractAddress = "0x123456789012345678901234567890123456789g"

        val result = useCase(1, contractAddress)

        assertTrue(result is DataResult.Error)
        val error = result as DataResult.Error
        assertTrue(error.exception is InvalidAddressException)
    }
}
