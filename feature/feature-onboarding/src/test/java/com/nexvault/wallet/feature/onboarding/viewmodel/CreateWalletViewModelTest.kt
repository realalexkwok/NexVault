package com.nexvault.wallet.feature.onboarding.viewmodel

import app.cash.turbine.test
import com.nexvault.wallet.domain.model.auth.WalletCreationResult
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.usecase.wallet.CreateWalletUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateWalletViewModelTest {

    private lateinit var createWalletUseCase: CreateWalletUseCase
    private val testDispatcher = kotlinx.coroutines.test.StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        createWalletUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val testMnemonic = listOf(
        "apple", "brave", "crane", "delta", "eagle", "frost",
        "grape", "house", "ivory", "jump", "king", "lamp"
    )

    @Test
    fun mnemonicLoadedOnInit() = runTest {
        coEvery { createWalletUseCase.invoke(any()) } returns DataResult.Success(
            WalletCreationResult(
                walletId = "abc",
                address = "0x1234...abcd",
                mnemonicWords = testMnemonic,
            )
        )

        val viewModel = CreateWalletViewModel(createWalletUseCase)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(12, state.mnemonicWords.size)
        assertEquals("abc", state.walletId)
        assertFalse(state.isLoading)
        assertNull(state.errorRes)
        assertNull(state.errorMessage)
    }

    @Test
    fun creationError() = runTest {
        coEvery { createWalletUseCase.invoke(any()) } returns DataResult.Error(
            exception = Exception("Encryption failed"),
            message = "Encryption failed"
        )

        val viewModel = CreateWalletViewModel(createWalletUseCase)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage?.contains("Encryption failed") == true)
        assertFalse(state.isLoading)
        assertTrue(state.mnemonicWords.isEmpty())
    }

    @Test
    fun acknowledgeTogglesState() = runTest {
        coEvery { createWalletUseCase.invoke(any()) } returns DataResult.Success(
            WalletCreationResult(
                walletId = "abc",
                address = "0x1234",
                mnemonicWords = testMnemonic,
            )
        )

        val viewModel = CreateWalletViewModel(createWalletUseCase)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAcknowledged)

        viewModel.onAcknowledgeToggled(true)
        assertTrue(viewModel.uiState.value.isAcknowledged)

        viewModel.onAcknowledgeToggled(false)
        assertFalse(viewModel.uiState.value.isAcknowledged)
    }

    @Test
    fun continueEmitsNavigationEvent() = runTest {
        coEvery { createWalletUseCase.invoke(any()) } returns DataResult.Success(
            WalletCreationResult(
                walletId = "abc",
                address = "0x1234",
                mnemonicWords = testMnemonic,
            )
        )

        val viewModel = CreateWalletViewModel(createWalletUseCase)

        advanceUntilIdle()

        viewModel.navigationEvent.test {
            viewModel.onContinueClicked()
            val event = awaitItem()
            assertTrue(event is CreateWalletViewModel.NavigationEvent.NavigateToVerifyMnemonic)
            assertEquals("abc", (event as CreateWalletViewModel.NavigationEvent.NavigateToVerifyMnemonic).walletId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retryAfterErrorCreatesNewWallet() = runTest {
        var callCount = 0
        coEvery { createWalletUseCase.invoke(any()) } answers {
            callCount++
            if (callCount == 1) {
                DataResult.Error(exception = Exception("First failure"), message = "First failure")
            } else {
                DataResult.Success(
                    WalletCreationResult(
                        walletId = "retry-id",
                        address = "0x5678",
                        mnemonicWords = testMnemonic,
                    )
                )
            }
        }

        val viewModel = CreateWalletViewModel(createWalletUseCase)

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage != null)

        viewModel.onRetryClicked()

        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorRes)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, callCount)
    }
}