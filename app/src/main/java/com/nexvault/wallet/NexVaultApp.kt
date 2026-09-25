package com.nexvault.wallet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nexvault.wallet.core.datastore.security.SecurityPreferencesDataStore
import com.nexvault.wallet.core.security.biometric.BiometricHelper
import com.nexvault.wallet.feature.auth.navigation.AuthRoutes
import com.nexvault.wallet.feature.auth.navigation.authGraph
import com.nexvault.wallet.feature.onboarding.navigation.OnboardingRoutes
import com.nexvault.wallet.feature.onboarding.navigation.onboardingGraph
import com.nexvault.wallet.navigation.MainRoutes
import com.nexvault.wallet.navigation.mainGraph
import kotlinx.coroutines.flow.StateFlow

/**
 * Root composable for the NexVault app.
 *
 * Determines the start destination based on:
 * 1. Is the user authenticated? (the `AppStateManager` session, supplied as
 *    [isAuthenticated]) — YES → main_graph (home, history, dapp, nft, settings)
 * 2. Is a wallet set up? (from SecurityPreferencesDataStore.isWalletSetUp)
 *    - NO  → onboarding_graph (create or import wallet)
 *    - YES → auth_graph (unlock screen)
 *
 * Navigation flow:
 * - First launch: onboarding_graph → (after PIN set) → main_graph
 * - Returning user: auth_graph → (after unlock) → main_graph
 * - After auto-lock: auth_graph → (after unlock) → main_graph
 *
 * The session is checked **first** on purpose (roadmap 2.0.2b): completing onboarding writes
 * `is_wallet_set_up` and unlocks the session, and if the flag were checked first the root
 * NavHost would be rebuilt at the auth graph in between — the defect this item fixes.
 */
@Composable
fun NexVaultApp(
    biometricHelper: BiometricHelper,
    isAuthenticated: StateFlow<Boolean>,
    onAuthSuccess: () -> Unit,
    onAuthRequired: () -> Unit,
    securityPreferences: SecurityPreferencesDataStore,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    val isAuthed by isAuthenticated.collectAsStateWithLifecycle(initialValue = false)

    // Observe wallet creation state from SecurityPreferencesDataStore
    val isWalletSetUp by securityPreferences.isWalletSetUp.collectAsStateWithLifecycle(
        initialValue = false
    )

    // Determine routing key based on state.
    //
    // Order matters (roadmap 2.0.2b): the session wins over the wallet flag, so once
    // CompleteOnboardingUseCase unlocks, a later `is_wallet_set_up` write cannot bounce the
    // graph through Unlock. The unlock itself is the last step of that use case and runs in
    // the same main-thread continuation as the flag write, so the intermediate
    // flag-set/session-locked state is not observable on screen.
    val routingKey = remember(isWalletSetUp, isAuthed) {
        when {
            isAuthed -> "main"
            !isWalletSetUp -> "onboarding"
            else -> "auth"
        }
    }

    key(routingKey) {
        NavHost(
            navController = navController,
            startDestination = when (routingKey) {
                "onboarding" -> OnboardingRoutes.ONBOARDING_GRAPH
                "auth" -> AuthRoutes.AUTH_GRAPH
                else -> MainRoutes.MAIN_GRAPH
            },
            modifier = modifier,
        ) {
            // Onboarding graph
            onboardingGraph(
                navController = navController,
                onOnboardingComplete = {
                    onAuthSuccess()
                },
            )

            // Auth graph
            authGraph(
                navController = navController,
                biometricHelper = biometricHelper,
                onAuthSuccess = {
                    onAuthSuccess()
                },
                onNavigateToOnboarding = {
                    navController.navigate(OnboardingRoutes.ONBOARDING_GRAPH) {
                        popUpTo(AuthRoutes.AUTH_GRAPH) {
                            inclusive = true
                        }
                    }
                },
            )

            // Main graph
            mainGraph()
        }
    }
}
