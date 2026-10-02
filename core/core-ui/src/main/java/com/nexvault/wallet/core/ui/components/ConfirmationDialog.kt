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
 * The three user-facing strings of a [ConfirmationDialog], bundled so the dialog's parameter
 * list stays readable (roadmap 2.10 scan fix, `kotlin:S107`).
 *
 * @property title Dialog title
 * @property confirmText Label of the confirm action
 * @property dismissText Label of the dismiss action
 */
data class ConfirmationDialogTexts(
    val title: String,
    val confirmText: String,
    val dismissText: String,
)

/**
 * Generic confirmation dialog: a title, an optional body slot, and confirm/dismiss actions.
 *
 * Roadmap 2.6 (TC-UI-010): the send review embeds its PIN entry in [body]; the dialog itself
 * stays screen-agnostic — tapping Confirm invokes [onConfirm], Cancel or outside-tap invokes
 * [onDismiss].
 */
@Composable
fun ConfirmationDialog(
    texts: ConfirmationDialogTexts,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmEnabled: Boolean = true,
    body: @Composable () -> Unit = {},
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = texts.title, style = MaterialTheme.typography.titleLarge) },
        text = { body() },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text(texts.confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(texts.dismissText)
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
                texts =
                    ConfirmationDialogTexts(
                        title = "Confirm",
                        confirmText = "Confirm",
                        dismissText = "Cancel",
                    ),
                onConfirm = {},
                onDismiss = {},
                body = { Text("Are you sure?") },
            )
        }
    }
}
