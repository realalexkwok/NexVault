package com.nexvault.wallet.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
class NexVaultColors(
    // Crypto-specific colors
    val positive: Color,
    val negative: Color,
    val warning: Color,
    val info: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val shimmer: Color,
    val shimmerHighlight: Color,
    // Text emphasis colors
    val textHigh: Color,
    val textMedium: Color,
    val textDisabled: Color,
    // Gradient colors
    val gradientStart: Color,
    val gradientEnd: Color,
)

private val DarkNexVaultColors = NexVaultColors(
    positive = Color(0xFF00E676),
    negative = Color(0xFFFF5252),
    warning = Color(0xFFFFB74D),
    info = Color(0xFF2196F3),
    cardSurface = Color(0xFF141928),
    cardBorder = Color(0xFF1E2440),
    shimmer = Color(0xFF1E2440),
    shimmerHighlight = Color(0xFF2A3148),
    textHigh = Color(0xFFFFFFFF),
    textMedium = Color(0xFFA0A8C8),
    textDisabled = Color(0xFF5A6180),
    gradientStart = Color(0xFF0A0E1A),
    gradientEnd = Color(0xFF121829),
)

private val LightNexVaultColors = NexVaultColors(
    positive = Color(0xFF00C853),
    negative = Color(0xFFD32F2F),
    warning = Color(0xFFFF9800),
    info = Color(0xFF1976D2),
    cardSurface = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE0E2E8),
    shimmer = Color(0xFFE8E9EF),
    shimmerHighlight = Color(0xFFF5F6FA),
    textHigh = Color(0xFF0A0E1A),
    textMedium = Color(0xFF5A6180),
    textDisabled = Color(0xFF9A9CA8),
    gradientStart = Color(0xFFF5F6FA),
    gradientEnd = Color(0xFFFFFFFF),
)

fun getDarkNexVaultColors(): NexVaultColors = DarkNexVaultColors
fun getLightNexVaultColors(): NexVaultColors = LightNexVaultColors
