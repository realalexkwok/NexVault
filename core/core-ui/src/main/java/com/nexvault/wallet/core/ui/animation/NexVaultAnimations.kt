package com.nexvault.wallet.core.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

object NexVaultAnimations {
    const val DURATION_SHORT = 200
    const val DURATION_MEDIUM = 350
    const val DURATION_LONG = 500

    val FastOutSlowIn = FastOutSlowInEasing
    val LinearOutSlowIn = LinearOutSlowInEasing

    fun fadeSlideInVertically(
        durationMillis: Int = DURATION_MEDIUM,
    ): EnterTransition {
        return fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing,
            ),
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    fun fadeSlideOutVertically(
        durationMillis: Int = DURATION_MEDIUM,
    ): ExitTransition {
        return fadeOut(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing,
            ),
        ) + slideOutVertically(
            targetOffsetY = { it / 4 },
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    fun defaultEnterTransition(): EnterTransition = fadeSlideInVertically()

    fun defaultExitTransition(): ExitTransition = fadeSlideOutVertically()
}
