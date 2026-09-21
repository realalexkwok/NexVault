package com.nexvault.wallet.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.nexvault.wallet.core.ui.preview.ThemePreviewWrapper
import com.nexvault.wallet.core.ui.theme.NexVaultColors
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.theme.NexVaultTheme

/**
 * PIN input field with 6 dot indicators and a numeric keypad.
 *
 * @param onDigitClick Callback when a digit is pressed. ViewModel should append this to its pin state.
 * @param onBackspaceClick Callback when backspace is pressed. ViewModel should remove last digit.
 * @param filledCount Number of digits already entered (controls dot display).
 * @param modifier Modifier for the dot row (for animations).
 * @param pinLength The length of the PIN (default 6).
 * @param isError Whether to show error state (red dots).
 */
@Composable
fun PinInputField(
    onDigitClick: (Int) -> Unit,
    onBackspaceClick: () -> Unit,
    filledCount: Int,
    modifier: Modifier = Modifier,
    pinLength: Int = 6,
    isError: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PinDotRow(
            filledCount = filledCount,
            pinLength = pinLength,
            isError = isError,
            modifier = modifier,
        )

        Spacer(modifier = Modifier.height(NexVaultDimens.spacingXl))

        NumericKeypad(
            onDigitClick = onDigitClick,
            onBackspaceClick = onBackspaceClick,
            pinLength = pinLength,
        )
    }
}

/**
 * Displays the PIN as a row of dots.
 * Filled dots represent entered digits, empty dots represent remaining digits.
 *
 * @param filledCount Number of dots that should be filled.
 * @param pinLength Total number of dots.
 * @param isError Whether to show error state (red dots).
 * @param modifier Modifier for applying animations (e.g., shake).
 */
@Composable
private fun PinDotRow(
    filledCount: Int,
    pinLength: Int,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingMd),
    ) {
        repeat(pinLength) { index ->
            PinDot(
                filled = index < filledCount,
                isError = isError,
            )
        }
    }
}

/**
 * A single PIN dot indicator.
 * @param filled Whether this dot should be filled (digit entered).
 * @param isError Whether to show error state (red).
 */
@Composable
private fun PinDot(
    filled: Boolean,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = NexVaultTheme.colors

    val dotColor = when {
        isError -> colors.negative
        filled -> colors.textHigh
        else -> Color.Transparent
    }

    val borderColor = when {
        isError -> colors.negative
        filled -> colors.textHigh
        else -> colors.textMedium
    }

    Box(
        modifier = modifier
            .size(NexVaultDimens.iconSizeXxs)
            .clip(CircleShape)
            .background(dotColor)
            .border(
                width = NexVaultDimens.borderWidthEmphasis,
                color = borderColor,
                shape = CircleShape,
            ),
    )
}

/**
 * Numeric keypad for PIN entry.
 * Shows digits 1-9, 0, and backspace button.
 * Calls [onDigitClick] when a digit is pressed and [onBackspaceClick] when backspace is pressed.
 */
@Composable
private fun NumericKeypad(
    onDigitClick: (Int) -> Unit,
    onBackspaceClick: () -> Unit,
    pinLength: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NexVaultDimens.spacing12),
    ) {
        // Row 1: 1, 2, 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf(1, 2, 3).forEach { digit ->
                KeypadDigit(
                    digit = digit,
                    onClick = { onDigitClick(digit) },
                )
            }
        }

        // Row 2: 4, 5, 6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf(4, 5, 6).forEach { digit ->
                KeypadDigit(
                    digit = digit,
                    onClick = { onDigitClick(digit) },
                )
            }
        }

        // Row 3: 7, 8, 9
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            listOf(7, 8, 9).forEach { digit ->
                KeypadDigit(
                    digit = digit,
                    onClick = { onDigitClick(digit) },
                )
            }
        }

        // Row 4: empty, 0, backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Box(modifier = Modifier.size(NexVaultDimens.pinKeySize))

            KeypadDigit(
                digit = 0,
                onClick = { onDigitClick(0) },
            )

            KeypadBackspace(onClick = onBackspaceClick)
        }
    }
}

/**
 * A single digit button in the numeric keypad.
 */
@Composable
private fun KeypadDigit(
    digit: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NexVaultTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .size(NexVaultDimens.pinKeySize)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = digit.toString(),
                fontSize = NexVaultDimens.pinDigitTextSize,
                fontWeight = FontWeight.Medium,
                color = colors.textHigh,
            )
        }
    }
}

/**
 * Backspace button in the numeric keypad.
 */
@Composable
private fun KeypadBackspace(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NexVaultTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .size(NexVaultDimens.pinKeySize)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = colors.textHigh,
                modifier = Modifier.size(NexVaultDimens.iconSizeMedium),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PinInputFieldPreview() {
    ThemePreviewWrapper {
        PinInputField(
            onDigitClick = {},
            onBackspaceClick = {},
            filledCount = 3,
        )
    }
}
