package com.nexvault.wallet.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.nexvault.wallet.core.ui.preview.ThemePreviewWrapper
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.theme.NexVaultTheme

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    lottieResId: Int? = null,
    actionButton: @Composable (() -> Unit)? = null,
) {
    val colors = NexVaultTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(NexVaultDimens.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            lottieResId != null -> {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(lottieResId)
                )
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(NexVaultDimens.iconSize3xl),
                )
            }
            icon != null -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(NexVaultDimens.iconSize2xl),
                    tint = colors.textMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textHigh,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(NexVaultDimens.spacingXs))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMedium,
            textAlign = TextAlign.Center,
        )

        if (actionButton != null) {
            Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))
            actionButton()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStateViewPreview() {
    ThemePreviewWrapper {
        EmptyStateView(
            title = "No tokens yet",
            subtitle = "Add a token to start tracking your portfolio",
            icon = Icons.Default.Search,
            actionButton = {
                NexVaultButton(text = "Add token", onClick = {})
            },
        )
    }
}
