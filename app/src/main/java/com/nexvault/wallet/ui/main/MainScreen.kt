package com.nexvault.wallet.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.feature.home.HomeScreen
import com.nexvault.wallet.feature.tokens.TokenDetailScreen
import com.nexvault.wallet.navigation.MainTab

/**
 * Main screen scaffold with bottom navigation bar and a nested NavHost
 * for the 5 tab destinations.
 *
 * Behavior (ref: AC-1.7):
 * - Bottom navigation bar shows 5 tabs with correct icons and labels
 * - Tab switching preserves scroll position and state (via saveState/restoreState)
 * - The NavHost inside manages tab content independently from the root NavHost
 *
 * ARCHITECTURE NOTE:
 * This screen uses its OWN NavController (tabNavController) separate from the
 * root app NavController. The root NavController navigates between onboarding,
 * auth, and main graphs. The tabNavController navigates between tabs within main.
 *
 * In Phase 2, each tab will be replaced with its own nested NavGraph containing
 * the actual feature screens (e.g., HomeTab will have HomeScreen → TokenDetailScreen).
 * For now, each tab shows a placeholder.
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val tabs = MainTab.entries

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                tabs.forEach { tab ->
                    val isSelected = when (tab) {
                        MainTab.HOME -> {
                            val r = currentDestination?.route.orEmpty()
                            r == "home_dashboard" ||
                                r.startsWith("token_detail") ||
                                currentDestination?.hierarchy?.any { it.route == MainTab.HOME.route } == true
                        }
                        else -> currentDestination?.hierarchy?.any {
                            it.route == tab.route
                        } == true
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.contentDescription,
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = MainTab.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            navigation(
                route = MainTab.HOME.route,
                startDestination = "home_dashboard",
            ) {
                composable("home_dashboard") {
                    HomeScreen(
                        onNavigateToTokenDetail = { contractAddress, chainId ->
                            tabNavController.navigate("token_detail/$contractAddress/$chainId")
                        },
                    )
                }
                composable(
                    route = "token_detail/{contractAddress}/{chainId}",
                    arguments = listOf(
                        navArgument("contractAddress") { type = NavType.StringType },
                        navArgument("chainId") { type = NavType.IntType },
                    ),
                ) {
                    TokenDetailScreen(
                        onNavigateBack = { tabNavController.popBackStack() },
                    )
                }
            }

            composable(MainTab.HISTORY.route) {
                PlaceholderTabScreen(
                    tabName = "History",
                    description = "Transaction history with filters and pagination.\nComing in Phase 2.",
                    emoji = "\uD83D\uDCDC",
                )
            }

            composable(MainTab.DAPP.route) {
                PlaceholderTabScreen(
                    tabName = "DApp Browser",
                    description = "WalletConnect v2 DApp connectivity.\nComing in Phase 3.",
                    emoji = "\uD83C\uDF10",
                )
            }

            composable(MainTab.NFT.route) {
                PlaceholderTabScreen(
                    tabName = "NFT Gallery",
                    description = "ERC-721 and ERC-1155 NFT display.\nComing in Phase 3.",
                    emoji = "\uD83D\uDDBC",
                )
            }

            composable(MainTab.SETTINGS.route) {
                PlaceholderTabScreen(
                    tabName = "Settings",
                    description = "Security, network, theme, and address book settings.\nComing in Phase 3.",
                    emoji = "\u2699\uFE0F",
                )
            }
        }
    }
}

/**
 * Placeholder screen displayed for each tab until the actual feature
 * screens are implemented in Phase 2/3.
 *
 * Shows an emoji icon, tab name, and description of what will be built.
 */
@Composable
private fun PlaceholderTabScreen(
    tabName: String,
    description: String,
    emoji: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = tabName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenPreview() {
    NexVaultTheme(darkTheme = true) {
        MainScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderTabScreenPreview() {
    NexVaultTheme(darkTheme = true) {
        PlaceholderTabScreen(
            tabName = "Home",
            description = "Portfolio dashboard.\nComing in Phase 2.",
            emoji = "\uD83C\uDFE0",
        )
    }
}