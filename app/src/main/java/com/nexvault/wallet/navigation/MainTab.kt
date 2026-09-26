package com.nexvault.wallet.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents the 5 main tabs in the bottom navigation bar.
 *
 * Each tab has:
 * - A route string for the nested NavHost
 * - A display label
 * - Selected and unselected icons
 * - A content description for accessibility
 *
 * Tab order (from wireframe in doc/03-UI-UX-DESIGN.md Section 2.3):
 * 1. Home — dashboard with portfolio and token list
 * 2. History — transaction history
 * 3. DApp — WalletConnect DApp browser
 * 4. NFT — NFT gallery
 * 5. Settings — app settings
 */
enum class MainTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDescription: String,
) {
    HOME(
        route = "home_tab",
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        contentDescription = "Home",
    ),
    HISTORY(
        route = "history_tab",
        label = "History",
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt,
        contentDescription = "Transaction History",
    ),
    DAPP(
        route = "dapp_tab",
        label = "DApp",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        contentDescription = "DApp Browser",
    ),
    NFT(
        route = "nft_tab",
        label = "NFT",
        selectedIcon = Icons.Filled.PhotoLibrary,
        unselectedIcon = Icons.Outlined.PhotoLibrary,
        contentDescription = "NFT Gallery",
    ),
    SETTINGS(
        route = "settings_tab",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        contentDescription = "Settings",
    ),
}
