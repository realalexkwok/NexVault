package com.nexvault.wallet.feature.onboarding.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.nexvault.wallet.feature.onboarding.screen.CreateWalletScreen
import com.nexvault.wallet.feature.onboarding.screen.ImportWalletScreen
import com.nexvault.wallet.feature.onboarding.screen.SetPinScreen
import com.nexvault.wallet.feature.onboarding.screen.VerifyMnemonicScreen
import com.nexvault.wallet.feature.onboarding.screen.WelcomeScreen

/**
 * Registers the onboarding navigation graph.
 *
 * Flow options:
 * 1. Welcome → Create Wallet → Verify Mnemonic → Set PIN → Main
 * 2. Welcome → Import Wallet → Set PIN → Main
 *
 * @param navController the NavController for navigation
 * @param onOnboardingComplete callback when the full onboarding flow finishes
 *        (after PIN is set). The caller should navigate to the main graph
 *        and clear the onboarding backstack.
 */
fun NavGraphBuilder.onboardingGraph(
    navController: NavController,
    onOnboardingComplete: () -> Unit,
) {
    navigation(
        route = OnboardingRoutes.ONBOARDING_GRAPH,
        startDestination = OnboardingRoutes.WELCOME,
    ) {
        composable(route = OnboardingRoutes.WELCOME) {
            WelcomeScreen(
                onNavigateToCreateWallet = {
                    navController.navigate(OnboardingRoutes.CREATE_WALLET)
                },
                onNavigateToImportWallet = {
                    navController.navigate(OnboardingRoutes.IMPORT_WALLET)
                },
            )
        }

        composable(route = OnboardingRoutes.CREATE_WALLET) {
            CreateWalletScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVerifyMnemonic = { walletId ->
                    navController.navigate(OnboardingRoutes.verifyMnemonic(walletId))
                },
            )
        }

        composable(
            route = OnboardingRoutes.VERIFY_MNEMONIC,
            arguments = listOf(
                navArgument("walletId") { type = NavType.StringType },
            ),
        ) {
            VerifyMnemonicScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSetPin = {
                    navController.navigate(OnboardingRoutes.SET_PIN) {
                        popUpTo(OnboardingRoutes.WELCOME) {
                            inclusive = false
                        }
                    }
                },
            )
        }

        composable(route = OnboardingRoutes.IMPORT_WALLET) {
            ImportWalletScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSetPin = {
                    navController.navigate(OnboardingRoutes.SET_PIN) {
                        popUpTo(OnboardingRoutes.WELCOME) {
                            inclusive = false
                        }
                    }
                },
            )
        }

        composable(route = OnboardingRoutes.SET_PIN) {
            SetPinScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMain = {
                    onOnboardingComplete()
                },
                showStepIndicator = true,
            )
        }
    }
}
