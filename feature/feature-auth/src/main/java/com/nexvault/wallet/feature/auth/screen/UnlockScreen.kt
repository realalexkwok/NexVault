package com.nexvault.wallet.feature.auth.screen

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexvault.wallet.core.security.biometric.BiometricHelper
import com.nexvault.wallet.core.ui.components.PinInputField
import com.nexvault.wallet.core.ui.theme.NexVaultDimens
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import com.nexvault.wallet.feature.auth.R
import com.nexvault.wallet.feature.auth.viewmodel.UnlockViewModel
import kotlin.math.roundToInt

private const val PIN_LENGTH = 6

@Composable
fun UnlockScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    biometricHelper: BiometricHelper,
    viewModel: UnlockViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val promptInfo =
        biometricHelper.createPromptInfo(
            title = R.string.unlock_biometric_prompt_title,
            subtitle = R.string.unlock_biometric_prompt_subtitle,
            negativeButtonText = R.string.unlock_use_pin,
        )

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                UnlockViewModel.NavigationEvent.NavigateToMain -> onNavigateToMain()
                UnlockViewModel.NavigationEvent.NavigateToOnboarding -> onNavigateToOnboarding()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.biometricEvent.collect { event ->
            when (event) {
                UnlockViewModel.BiometricEvent.ShowBiometricPrompt -> {
                    activity?.let { fragActivity ->
                        showBiometricPrompt(
                            activity = fragActivity,
                            promptInfo = promptInfo,
                            onSuccess = { viewModel.onBiometricSuccess() },
                            onError = { errorMsg -> viewModel.onBiometricError(errorMsg) },
                        )
                    }
                }
            }
        }
    }

    UnlockScreenContent(
        uiState = uiState,
        onDigitPressed = viewModel::onDigitPressed,
        onBackspacePressed = viewModel::onBackspacePressed,
        onBiometricRequested = viewModel::onBiometricRequested,
        onShakeAnimationComplete = viewModel::onShakeAnimationComplete,
    )
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    promptInfo: BiometricPrompt.PromptInfo,
    onSuccess: () -> Unit,
    onError: (String?) -> Unit,
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
            ) {
                onError(errString.toString())
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
        }
    }

    val biometricPrompt = BiometricPrompt(activity, executor, callback)
    biometricPrompt.authenticate(promptInfo)
}

@Composable
private fun UnlockScreenContent(
    uiState: UnlockViewModel.UiState,
    onDigitPressed: (Int) -> Unit,
    onBackspacePressed: () -> Unit,
    onBiometricRequested: () -> Unit,
    onShakeAnimationComplete: () -> Unit,
) {
    val shakeOffset = remember { Animatable(0f) }
    val errorArgs = uiState.errorArgs.toTypedArray()
    val errorText =
        uiState.errorMessage
            ?: uiState.errorRes?.let { stringResource(it, *errorArgs) }
            ?: uiState.errorPluralsRes?.let { pluralStringResource(it, uiState.errorQuantity, *errorArgs) }
    val hasError = uiState.errorMessage != null || uiState.errorRes != null || uiState.errorPluralsRes != null

    LaunchedEffect(uiState.isShakeError) {
        if (uiState.isShakeError) {
            repeat(3) {
                shakeOffset.animateTo(
                    targetValue = 12f,
                    animationSpec = tween(durationMillis = 50),
                )
                shakeOffset.animateTo(
                    targetValue = -12f,
                    animationSpec = tween(durationMillis = 50),
                )
            }
            shakeOffset.animateTo(0f, animationSpec = tween(durationMillis = 50))
            onShakeAnimationComplete()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = NexVaultDimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(NexVaultDimens.spacing64))

            // App icon placeholder
            Surface(
                modifier = Modifier.size(NexVaultDimens.logoSizeSmall),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "N",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))

            Text(
                text = stringResource(R.string.unlock_brand_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(NexVaultDimens.spacingSm))

            Text(
                text = if (uiState.isLockedOut) {
                    stringResource(R.string.unlock_locked_out)
                } else {
                    stringResource(R.string.unlock_enter_pin)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(NexVaultDimens.spacingLg))

            // Error message
            AnimatedVisibility(
                visible = hasError,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = errorText.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = NexVaultDimens.spacingSm),
                )
            }

            // Lockout countdown
            AnimatedVisibility(
                visible = uiState.isLockedOut,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.unlock_try_again_in),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingXs))
                    Text(
                        text = "${uiState.lockoutRemainingSeconds}s",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(NexVaultDimens.spacingMd))
                }
            }

            if (!uiState.isLockedOut) {
                key(uiState.failedAttempts) {
                    Box(
                        modifier = Modifier,
                    ) {
                        PinInputField(
                            onDigitClick = onDigitPressed,
                            onBackspaceClick = onBackspacePressed,
                            filledCount = uiState.pin.length,
                            isError = uiState.isShakeError,
                        )
                    }
                }
            } else {
                // During lockout, show disabled PIN dots without keypad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = NexVaultDimens.spacingXl),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(NexVaultDimens.spacingMd)) {
                        repeat(PIN_LENGTH) {
                            Box(
                                modifier = Modifier
                                    .size(NexVaultDimens.spacingMd)
                                    .clip(CircleShape)
                                    .border(
                                        width = NexVaultDimens.borderWidthEmphasis,
                                        color = MaterialTheme.colorScheme.outline,
                                        shape = CircleShape,
                                    )
                                    .background(Color.Transparent)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Biometric button
            if (uiState.isBiometricEnabled && !uiState.isLockedOut) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = NexVaultDimens.spacingMd),
                ) {
                    IconButton(onClick = onBiometricRequested) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = stringResource(R.string.unlock_use_biometric),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(NexVaultDimens.spacingXl),
                        )
                    }
                    TextButton(onClick = onBiometricRequested) {
                        Text(
                            text = stringResource(R.string.unlock_use_biometric),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(NexVaultDimens.spacingXl))
        }

        // Verification overlay
        if (uiState.isVerifying) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(NexVaultDimens.iconSizeXl),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UnlockScreenDefaultPreview() {
    NexVaultTheme {
        UnlockScreenContent(
            uiState = UnlockViewModel.UiState(
                pin = "12",
                isBiometricAvailable = true,
                isBiometricEnabled = true,
            ),
            onDigitPressed = {},
            onBackspacePressed = {},
            onBiometricRequested = {},
            onShakeAnimationComplete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UnlockScreenErrorPreview() {
    NexVaultTheme {
        UnlockScreenContent(
            uiState = UnlockViewModel.UiState(
                pin = "",
                errorPluralsRes = R.plurals.unlock_incorrect_pin_attempts,
                errorQuantity = 2,
                failedAttempts = 3,
                isBiometricAvailable = true,
                isBiometricEnabled = true,
            ),
            onDigitPressed = {},
            onBackspacePressed = {},
            onBiometricRequested = {},
            onShakeAnimationComplete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UnlockScreenLockedOutPreview() {
    NexVaultTheme {
        UnlockScreenContent(
            uiState = UnlockViewModel.UiState(
                pin = "",
                isLockedOut = true,
                lockoutRemainingSeconds = 22,
                failedAttempts = 5,
                errorRes = R.string.unlock_too_many_attempts,
                isBiometricAvailable = false,
                isBiometricEnabled = false,
            ),
            onDigitPressed = {},
            onBackspacePressed = {},
            onBiometricRequested = {},
            onShakeAnimationComplete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun UnlockScreenNoBiometricPreview() {
    NexVaultTheme {
        UnlockScreenContent(
            uiState = UnlockViewModel.UiState(
                pin = "",
                isBiometricAvailable = false,
                isBiometricEnabled = false,
            ),
            onDigitPressed = {},
            onBackspacePressed = {},
            onBiometricRequested = {},
            onShakeAnimationComplete = {},
        )
    }
}
