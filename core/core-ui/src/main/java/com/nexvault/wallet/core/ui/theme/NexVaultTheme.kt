package com.nexvault.wallet.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalNexVaultColors = staticCompositionLocalOf { getDarkNexVaultColors() }
val LocalNexVaultDimens = staticCompositionLocalOf { NexVaultDimens }

@Composable
fun NexVaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) NexVaultDarkColorScheme else NexVaultLightColorScheme
    val nexVaultColors = remember(darkTheme) {
        if (darkTheme) getDarkNexVaultColors() else getLightNexVaultColors()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalNexVaultColors provides nexVaultColors,
        LocalNexVaultDimens provides NexVaultDimens,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NexVaultTypography,
            shapes = NexVaultShapes,
            content = content,
        )
    }
}

object NexVaultTheme {
    val colors: NexVaultColors
        @Composable
        get() = LocalNexVaultColors.current

    val dimens: NexVaultDimens
        @Composable
        get() = LocalNexVaultDimens.current
}
