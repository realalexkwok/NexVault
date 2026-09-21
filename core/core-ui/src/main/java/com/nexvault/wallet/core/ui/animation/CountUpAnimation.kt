package com.nexvault.wallet.core.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.nexvault.wallet.core.ui.preview.ThemePreviewWrapper
import java.text.DecimalFormat

@Composable
fun CountUpAnimation(
    targetValue: Float,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.displayLarge,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    prefix: String = "$",
    suffix: String = "",
    decimals: Int = 2,
    durationMillis: Int = 1000,
    onAnimationEnd: () -> Unit = {},
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatable.animateTo(
            targetValue = targetValue,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing,
            ),
        )
        onAnimationEnd()
    }

    val formatter = remember(decimals) {
        DecimalFormat().apply {
            minimumFractionDigits = decimals
            maximumFractionDigits = decimals
            isGroupingUsed = true
        }
    }

    val currentValue = remember(targetValue, animatable.value) {
        animatable.value
    }

    Text(
        text = "$prefix${formatter.format(currentValue)}$suffix",
        style = textStyle,
        color = textColor,
        modifier = modifier,
    )
}

@Composable
fun CountUpAnimationInt(
    targetValue: Int,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.displayLarge,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    prefix: String = "",
    suffix: String = "",
    durationMillis: Int = 1000,
    onAnimationEnd: () -> Unit = {},
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatable.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing,
            ),
        )
        onAnimationEnd()
    }

    val currentValue = remember(targetValue, animatable.value) {
        animatable.value.toInt()
    }

    Text(
        text = "$prefix$currentValue$suffix",
        style = textStyle,
        color = textColor,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun CountUpAnimationPreview() {
    ThemePreviewWrapper {
        CountUpAnimation(targetValue = 1234.56f)
    }
}

@Preview(showBackground = true)
@Composable
private fun CountUpAnimationIntPreview() {
    ThemePreviewWrapper {
        CountUpAnimationInt(targetValue = 128, suffix = " txns")
    }
}
