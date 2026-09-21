package com.nexvault.wallet.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nexvault.wallet.core.ui.preview.ThemePreviewWrapper
import com.nexvault.wallet.core.ui.theme.NexVaultDimens

@Composable
fun NexVaultButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    secondary: Boolean = false,
) {
    if (secondary) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(NexVaultDimens.buttonHeight),
            enabled = enabled && !isLoading,
            shape = RoundedCornerShape(NexVaultDimens.cornerRadiusLarge),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ),
            contentPadding = PaddingValues(horizontal = NexVaultDimens.spacingLg, vertical = NexVaultDimens.spacingMd),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(NexVaultDimens.iconSizeSmall),
                    strokeWidth = NexVaultDimens.borderWidthStrong,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(NexVaultDimens.buttonHeight),
            enabled = enabled && !isLoading,
            shape = RoundedCornerShape(NexVaultDimens.cornerRadiusLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            ),
            contentPadding = PaddingValues(horizontal = NexVaultDimens.spacingLg, vertical = NexVaultDimens.spacingMd),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(NexVaultDimens.iconSizeSmall),
                    strokeWidth = NexVaultDimens.borderWidthStrong,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NexVaultButtonPreview() {
    ThemePreviewWrapper {
        NexVaultButton(text = "Continue", onClick = {})
    }
}
