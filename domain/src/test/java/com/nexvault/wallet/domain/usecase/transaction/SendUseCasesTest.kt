package com.nexvault.wallet.domain.usecase.transaction

import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.transaction.GasEstimate
import com.nexvault.wallet.domain.model.transaction.GasOption
import com.nexvault.wallet.domain.model.transaction.SendTransactionParams
import com.nexvault.wallet.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Roadmap 2.6: the two send use cases are thin delegation seams between the feature layer and the
 * repository. These tests lock the dispatch rules: estimate passthrough, native vs token routing.
 */
class SendUseCasesTest {
    private val repository = mockk<TransactionRepository>()

    private val estimateUseCase = EstimateSendGasUseCase(repository)
    private val sendUseCase = SendTransactionUseCase(repository)

    @Test
    fun estimateGas_delegatesToTheRepository() = runTest {
        val expected =
            GasEstimate(
                slow = GasOption(BigInteger.ONE, null, null, 600, "~10 min", null),
                normal = GasOption(BigInteger.TWO, null, null, 180, "~3 min", null),
                fast = GasOption(BigInteger.TEN, null, null, 30, "~30 sec", null),
                gasLimit = BigInteger.valueOf(21000),
            )
        coEvery { repository.estimateSendGas("0xfrom", "0xto", BigDecimal.ONE, null, 1) } returns
            DataResult.Success(expected)

        val result = estimateUseCase("0xfrom", "0xto", BigDecimal.ONE, null, 1)

        assertTrue(result is DataResult.Success)
        assertEquals(expected, (result as DataResult.Success).data)
    }

    @Test
    fun send_withNullTokenAddress_routesToNativeSend() = runTest {
        val params = sendParams(tokenAddress = null)
        coEvery { repository.sendNativeTransaction(params, "w1", 0) } returns DataResult.Success("0xhash")

        val result = sendUseCase(params, "w1", 0)

        assertEquals("0xhash", (result as DataResult.Success).data)
        coVerify(exactly = 1) { repository.sendNativeTransaction(params, "w1", 0) }
        coVerify(exactly = 0) { repository.sendTokenTransaction(any(), any(), any()) }
    }

    @Test
    fun send_withTokenAddress_routesToTokenSend() = runTest {
        val params = sendParams(tokenAddress = "0xtoken")
        coEvery { repository.sendTokenTransaction(params, "w1", 0) } returns DataResult.Success("0xhash")

        val result = sendUseCase(params, "w1", 0)

        assertEquals("0xhash", (result as DataResult.Success).data)
        coVerify(exactly = 1) { repository.sendTokenTransaction(params, "w1", 0) }
        coVerify(exactly = 0) { repository.sendNativeTransaction(any(), any(), any()) }
    }

    private fun sendParams(tokenAddress: String?) =
        SendTransactionParams(
            toAddress = "0x1234567890abcdef1234567890abcdef12345678",
            amount = BigDecimal.ONE,
            tokenAddress = tokenAddress,
            gasOption = GasOption(BigInteger.TWO, null, null, 180, "~3 min", null),
            chainId = 1,
            data = null,
        )
}
