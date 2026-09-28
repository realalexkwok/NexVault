package com.nexvault.wallet.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nexvault.wallet.core.ui.preview.ThemePreviewWrapper
import com.nexvault.wallet.core.ui.theme.NexVaultTheme

/**
 * Generic confirmation dialog: a title, an optional body slot, and confirm/dismiss actions.
 *
 * Roadmap 2.6 (TC-UI-010): the send review embeds its PIN entry in [body]; the dialog itself
 * stays screen-agnostic — tapping Confirm invokes [onConfirm], Cancel or outside-tap invokes
 * [onDismiss].
 */
@Composable
fun ConfirmationDialog(
    title: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    body: @Composable () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = { body() },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreview() {
    ThemePreviewWrapper {
        NexVaultTheme(darkTheme = true) {
            ConfirmationDialog(
                title = "Confirm",
                confirmText = "Confirm",
                dismissText = "Cancel",
                onConfirm = {},
                onDismiss = {},
                body = { Text("Are you sure?") },
            )
        }
    }
}
