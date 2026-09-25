package com.nexvault.wallet

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.nexvault.wallet.core.datastore.model.AutoLockTimeout
import com.nexvault.wallet.core.datastore.preferences.UserPreferencesDataStore
import com.nexvault.wallet.core.datastore.security.SecurityPreferencesDataStore
import com.nexvault.wallet.core.datastore.state.AppStateManager
import com.nexvault.wallet.core.security.biometric.BiometricHelper
import com.nexvault.wallet.core.ui.theme.NexVaultTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricHelper: BiometricHelper

    @Inject
    lateinit var userPreferences: UserPreferencesDataStore

    @Inject
    lateinit var securityPreferences: SecurityPreferencesDataStore

    @Inject
    lateinit var appStateManager: AppStateManager

    private var backgroundedAt: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NexVaultTheme(darkTheme = true) {
                NexVaultApp(
                    biometricHelper = biometricHelper,
                    // One session source: the AppStateManager flag that also gates wallet
                    // material retrieval (roadmap 2.0.2b). Completing onboarding unlocks it, so
                    // the router reaches `main` without a detour through the auth graph, and
                    // process-scoped it survives configuration changes.
                    isAuthenticated = appStateManager.isUnlocked,
                    onAuthSuccess = { appStateManager.unlock() },
                    onAuthRequired = { appStateManager.lock() },
                    securityPreferences = securityPreferences,
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (appStateManager.isUnlocked.value) {
            backgroundedAt = System.currentTimeMillis()
        }
    }

    override fun onStart() {
        super.onStart()
        if (backgroundedAt > 0L && appStateManager.isUnlocked.value) {
            val elapsedMs = System.currentTimeMillis() - backgroundedAt

            lifecycleScope.launch {
                val autoLockTimeout = userPreferences.autoLockTimeout.first()

                val timeoutMillis = when (autoLockTimeout) {
                    AutoLockTimeout.NEVER -> Long.MAX_VALUE
                    AutoLockTimeout.IMMEDIATE -> 0L
                    else -> autoLockTimeout.seconds * 1000L
                }

                if (elapsedMs > timeoutMillis) {
                    appStateManager.lock()
                }
                backgroundedAt = 0L
            }
        } else {
            backgroundedAt = 0L
        }
    }
}
