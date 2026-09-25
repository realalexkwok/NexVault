package com.nexvault.wallet.feature.onboarding.viewmodel

import app.cash.turbine.test
import com.nexvault.wallet.domain.model.auth.WalletCreationResult
import com.nexvault.wallet.domain.model.common.DataResult
import com.nexvault.wallet.domain.model.wallet.WalletDraft
import com.nexvault.wallet.domain.usecase.wallet.CreateWalletUseCase
import com.nexvault.wallet.domain.usecase.wallet.GenerateWalletDraftUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
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

/**
 * Roadmap 2.0.2b, option B: generation must not persist anything, and persistence happens
 * only after the user acknowledges the mnemonic and taps Continue.
 */
class CreateWalletViewModelTest {

    private lateinit var generateWalletDraftUseCase: GenerateWalletDraftUseCase
    private lateinit var createWalletUseCase: CreateWalletUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        generateWalletDraftUseCase = mockk()
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

    private val testDraft = WalletDraft(
        mnemonic = testMnemonic.joinToString(" "),
        mnemonicWords = testMnemonic,
        address = "0x1234...abcd",
    )

    private fun stubGenerateSuccess() {
        coEvery { generateWalletDraftUseCase.invoke() } returns DataResult.Success(testDraft)
    }

    private fun stubPersistSuccess(walletId: String = "abc") {
        coEvery { createWalletUseCase.invoke(any(), any()) } returns DataResult.Success(
            WalletCreationResult(
                walletId = walletId,
                address = testDraft.address,
                mnemonicWords = testMnemonic,
            )
        )
    }

    @Test
    fun draftLoadedOnInitWithoutPersisting() = runTest {
        stubGenerateSuccess()

        val viewModel = CreateWalletViewModel(generateWalletDraftUseCase, createWalletUseCase)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(12, state.mnemonicWords.size)
        assertEquals("0x1234...abcd", state.address)
        assertEquals("", state.walletId)
        assertFalse(state.isLoading)
        assertNull(state.errorRes)
        assertNull(state.errorMessage)

        // Option B: nothing may be written before the user acknowledges the phrase.
        coVerify(exactly = 0) { createWalletUseCase.invoke(any(), any()) }
    }

    @Test
    fun continueWithoutAcknowledgmentDoesNotPersist() = runTest {
        stubGenerateSuccess()
        stubPersistSuccess()

        val viewModel = CreateWalletViewModel(generateWalletDraftUseCase, createWalletUseCase)
        advanceUntilIdle()

        viewModel.onContinueClicked()
        advanceUntilIdle()

        coVerify(exactly = 0) { createWalletUseCase.invoke(any(), any()) }
    }

    @Test
    fun continueAfterAcknowledgmentPersistsDraftAndNavigates() = runTest {
        stubGenerateSuccess()
        stubPersistSuccess()

        val viewModel = CreateWalletViewModel(generateWalletDraftUseCase, createWalletUseCase)
        advanceUntilIdle()

        viewModel.onAcknowledgeToggled(true)

        viewModel.navigationEvent.test {
            viewModel.onContinueClicked()

            val event = awaitItem()
            assertTrue(event is CreateWalletViewModel.NavigationEvent.NavigateToVerifyMnemonic)
            assertEquals(
                "abc",
                (event as CreateWalletViewModel.NavigationEvent.NavigateToVerifyMnemonic).walletId,
            )
            cancelAndIgnoreRemainingEvents()
        }

        val draftSlot = slot<WalletDraft>()
        coVerify { createWalletUseCase.invoke("Main Wallet", capture(draftSlot)) }
        assertEquals(testDraft, draftSlot.captured)
    }

    @Test
    fun generationErrorShowsErrorAndRetryRegenerates() = runTest {
        var generateCalls = 0
        coEvery { generateWalletDraftUseCase.invoke() } answers {
            generateCalls++
            if (generateCalls == 1) {
                DataResult.Error(exception = Exception("No entropy"), message = "No entropy")
            } else {
                DataResult.Success(testDraft)
            }
        }

        val viewModel = CreateWalletViewModel(generateWalletDraftUseCase, createWalletUseCase)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage?.contains("No entropy") == true)
        assertTrue(viewModel.uiState.value.mnemonicWords.isEmpty())

        viewModel.onRetryClicked()
        advanceUntilIdle()

        assertEquals(2, generateCalls)
        assertEquals(12, viewModel.uiState.value.mnemonicWords.size)
        assertNull(viewModel.uiState.value.errorRes)
        coVerify(exactly = 0) { createWalletUseCase.invoke(any(), any()) }
    }

    @Test
    fun persistErrorRetryReusesTheSameDraft() = runTest {
        stubGenerateSuccess()
        var persistCalls = 0
        coEvery { createWalletUseCase.invoke(any(), any()) } answers {
            persistCalls++
            if (persistCalls == 1) {
                DataResult.Error(exception = Exception("Disk full"), message = "Disk full")
            } else {
                DataResult.Success(
                    WalletCreationResult(
                        walletId = "retry-id",
                        address = testDraft.address,
                        mnemonicWords = testMnemonic,
                    )
                )
            }
        }

        val viewModel = CreateWalletViewModel(generateWalletDraftUseCase, createWalletUseCase)
        advanceUntilIdle()
        viewModel.onAcknowledgeToggled(true)
        viewModel.onContinueClicked()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage?.contains("Disk full") == true)

        viewModel.onRetryClicked()
        advanceUntilIdle()

        assertEquals(2, persistCalls)
        // The mnemonic shown to the user must never be silently replaced by a regeneration.
        coVerify(exactly = 1) { generateWalletDraftUseCase.invoke() }

        val drafts = mutableListOf<WalletDraft>()
        coVerify { createWalletUseCase.invoke(any(), capture(drafts)) }
        assertEquals(2, drafts.size)
        assertEquals(testDraft, drafts[0])
        assertEquals(testDraft, drafts[1])
    }

    @Test
    fun acknowledgeTogglesState() = runTest {
        stubGenerateSuccess()

        val viewModel = CreateWalletViewModel(generateWalletDraftUseCase, createWalletUseCase)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAcknowledged)

        viewModel.onAcknowledgeToggled(true)
        assertTrue(viewModel.uiState.value.isAcknowledged)

        viewModel.onAcknowledgeToggled(false)
        assertFalse(viewModel.uiState.value.isAcknowledged)
    }
}
