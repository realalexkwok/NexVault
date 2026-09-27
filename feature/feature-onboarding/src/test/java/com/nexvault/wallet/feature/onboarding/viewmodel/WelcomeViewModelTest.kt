package com.nexvault.wallet.feature.onboarding.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class WelcomeViewModelTest {
    @Test
    fun onCreateWalletClickedEmitsNavigateToCreateWallet() = runTest {
        val viewModel = WelcomeViewModel()

        viewModel.navigationEvent.test {
            viewModel.onCreateWalletClicked()
            val event = awaitItem()
            assertTrue(event is WelcomeViewModel.NavigationEvent.NavigateToCreateWallet)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onImportWalletClickedEmitsNavigateToImportWallet() = runTest {
        val viewModel = WelcomeViewModel()

        viewModel.navigationEvent.test {
            viewModel.onImportWalletClicked()
            val event = awaitItem()
            assertTrue(event is WelcomeViewModel.NavigationEvent.NavigateToImportWallet)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
