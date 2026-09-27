package com.nexvault.wallet.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.nexvault.wallet.ui.main.MainScreen

/**
 * Route constants for the main graph.
 */
object MainRoutes {
    const val MAIN_GRAPH = "main_graph"
    const val MAIN_SCREEN = "main_screen"
}

/**
 * Registers the main navigation graph containing the MainScreen
 * with bottom navigation and placeholder tab content.
 *
 * In Phase 2/3, this graph will be expanded to include nested graphs
 * for each tab (HomeScreen → TokenDetailScreen, etc.)
 */
fun NavGraphBuilder.mainGraph() {
    navigation(
        route = MainRoutes.MAIN_GRAPH,
        startDestination = MainRoutes.MAIN_SCREEN,
    ) {
        composable(route = MainRoutes.MAIN_SCREEN) {
            MainScreen()
        }
    }
}
