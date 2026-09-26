package com.nexvault.wallet.feature.onboarding.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.usecase.wallet.GetMnemonicForBackupUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VerifyMnemonicViewModelTest {
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var getMnemonicForBackupUseCase: GetMnemonicForBackupUseCase

    private val testMnemonic = listOf(
        "apple", "brave", "crane", "delta", "eagle", "frost",
        "grape", "house", "ivory", "jump", "king", "lamp",
    )

    @Before
    fun setup() {
        Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher())
        savedStateHandle = mockk(relaxed = true)
        every { savedStateHandle.get<String>("walletId") } returns "test-wallet-id"
        getMnemonicForBackupUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun correctOrder() = runTest {
        coEvery { getMnemonicForBackupUseCase.invoke(any()) } returns DataResult.Success(testMnemonic)

        val viewModel = VerifyMnemonicViewModel(savedStateHandle, getMnemonicForBackupUseCase)

        advanceUntilIdle()

        // Tap words in correct order
        for (word in testMnemonic) {
            viewModel.onWordSelected(word)
        }

        assertTrue(viewModel.uiState.value.isVerified)
        assertEquals(12, viewModel.uiState.value.selectedWords.size)
        assertTrue(viewModel.uiState.value.availableWords.isEmpty())
    }

    @Test
    fun incorrectOrder() = runTest {
        coEvery { getMnemonicForBackupUseCase.invoke(any()) } returns DataResult.Success(testMnemonic)

        val viewModel = VerifyMnemonicViewModel(savedStateHandle, getMnemonicForBackupUseCase)

        advanceUntilIdle()

        // Tap wrong word first (should be "apple")
        viewModel.onWordSelected("brave")

        assertTrue(viewModel.uiState.value.isError)
    }

    @Test
    fun deselectLastWord() = runTest {
        coEvery { getMnemonicForBackupUseCase.invoke(any()) } returns DataResult.Success(testMnemonic)

        val viewModel = VerifyMnemonicViewModel(savedStateHandle, getMnemonicForBackupUseCase)

        advanceUntilIdle()

        // Select "apple", "brave"
        viewModel.onWordSelected("apple")
        viewModel.onWordSelected("brave")

        assertEquals(2, viewModel.uiState.value.selectedWords.size)

        // Remove last word "brave"
        viewModel.onSelectedWordRemoved("brave")

        assertEquals(1, viewModel.uiState.value.selectedWords.size)
        assertEquals("apple", viewModel.uiState.value.selectedWords.first())
    }

    @Test
    fun cannotDeselectNonLastWord() = runTest {
        coEvery { getMnemonicForBackupUseCase.invoke(any()) } returns DataResult.Success(testMnemonic)

        val viewModel = VerifyMnemonicViewModel(savedStateHandle, getMnemonicForBackupUseCase)

        advanceUntilIdle()

        // Select "apple", "brave"
        viewModel.onWordSelected("apple")
        viewModel.onWordSelected("brave")

        // Try to remove first word "apple" (not last)
        viewModel.onSelectedWordRemoved("apple")

        // Should still have both words
        assertEquals(2, viewModel.uiState.value.selectedWords.size)
    }

    @Test
    fun confirmEmitsNavigationWhenVerified() = runTest {
        coEvery { getMnemonicForBackupUseCase.invoke(any()) } returns DataResult.Success(testMnemonic)

        val viewModel = VerifyMnemonicViewModel(savedStateHandle, getMnemonicForBackupUseCase)

        advanceUntilIdle()

        // Select all words in correct order
        for (word in testMnemonic) {
            viewModel.onWordSelected(word)
        }

        viewModel.navigationEvent.test {
            viewModel.onConfirmClicked()
            val event = awaitItem()
            assertTrue(event is VerifyMnemonicViewModel.NavigationEvent.NavigateToSetPin)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmDoesNothingWhenNotVerified() = runTest {
        coEvery { getMnemonicForBackupUseCase.invoke(any()) } returns DataResult.Success(testMnemonic)

        val viewModel = VerifyMnemonicViewModel(savedStateHandle, getMnemonicForBackupUseCase)

        advanceUntilIdle()

        // Only select one word (not fully verified)
        viewModel.onWordSelected("apple")

        viewModel.navigationEvent.test {
            viewModel.onConfirmClicked()
            expectNoEvents()
        }
    }
}
