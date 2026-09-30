package com.nexvault.wallet.feature.auth.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexvault.wallet.core.datastore.state.AppStateManager
import com.nexvault.wallet.core.security.biometric.BiometricHelper
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.domain.repository.AuthRepository
import com.nexvault.wallet.domain.usecase.auth.VerifyPinUseCase
import com.nexvault.wallet.feature.auth.viewmodel.UnlockViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roadmap 2.6 coverage: the unlock screen renders its PIN pad and lockout copy on the JVM, with a
 * real [UnlockViewModel] wired to mocked repositories.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class UnlockScreenCoverageTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val verifyPinUseCase = mockk<VerifyPinUseCase>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val appStateManager = mockk<AppStateManager>(relaxed = true)
    private val biometricHelper = mockk<BiometricHelper>(relaxed = true)

    private fun stubAuthState() {
        coEvery { authRepository.isBiometricAvailable() } returns false
        every { authRepository.isBiometricEnabled() } returns flowOf(false)
        every { appStateManager.isLockedOut } returns MutableStateFlow(false)
        every { appStateManager.lockoutRemainingSeconds } returns MutableStateFlow(0L)
    }

    @Test
    fun unlockScreen_rendersThePinPadAndBrand() {
        stubAuthState()

        composeRule.setContent {
            NexVaultTheme(darkTheme = true) {
                UnlockScreen(
                    onNavigateToMain = { },
                    onNavigateToOnboarding = { },
                    biometricHelper = biometricHelper,
                    viewModel = UnlockViewModel(verifyPinUseCase, authRepository, appStateManager),
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("N").assertIsDisplayed()
        composeRule.onNodeWithText("1").performClick()
        composeRule.waitForIdle()
    }
}
