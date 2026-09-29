package com.nexvault.wallet

import androidx.activity.ComponentActivity

/**
 * Empty activity host for Robolectric/device Compose tests that need to call `setContent`
 * themselves (roadmap 2.6, TC-UI-010). Lives in the debug source set so it never ships in
 * release builds; the app's MainActivity cannot host them because it already sets content.
 */
class DialogTestHostActivity : ComponentActivity()
