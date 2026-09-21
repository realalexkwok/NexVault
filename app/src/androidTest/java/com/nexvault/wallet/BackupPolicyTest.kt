package com.nexvault.wallet

import android.content.res.XmlResourceParser
import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

/**
 * Locks in NexVault's backup posture (legacy CR 1.1, finding 1.1-1).
 *
 * A non-custodial wallet must never let key material leave the device: not to Google
 * Drive, and not through a device-to-device transfer. `android:allowBackup="false"`
 * alone disables cloud backup but **not** D2D transfer on Android 12+ (API 31+), so the
 * exclusion is enforced by `res/xml/data_extraction_rules.xml` as well.
 *
 * These assertions fail if a future manifest or rules edit silently re-enables either path.
 */
@RunWith(AndroidJUnit4::class)
class BackupPolicyTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** The app must not carry the platform's backup-eligible flag at all. */
    @Test
    fun appIsNotEligibleForAutoBackup() {
        val info = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertEquals(
            "FLAG_ALLOW_BACKUP must be off — key material must never leave the device",
            0,
            info.flags and ApplicationInfo.FLAG_ALLOW_BACKUP
        )
    }

    /** The API <= 30 Auto Backup rules must exclude every storage domain outright. */
    @Test
    fun fullBackupRulesExcludeEveryDomain() {
        val excluded = exclusionsIn(R.xml.backup_rules, section = null)
        assertEquals(
            "backup_rules.xml must exclude every domain, found: ${excluded.keys}",
            ALL_DOMAINS,
            excluded.keys
        )
        assertTrue(
            "every exclusion must cover the whole domain (path '.'), found: $excluded",
            excluded.values.all { it == "." }
        )
    }

    /** API 31+ rules must exclude everything from both cloud backup and D2D transfer. */
    @Test
    fun dataExtractionRulesExcludeCloudBackupAndDeviceTransfer() {
        val cloud = exclusionsIn(R.xml.data_extraction_rules, "cloud-backup")
        val deviceTransfer = exclusionsIn(R.xml.data_extraction_rules, "device-transfer")
        assertEquals("cloud-backup must exclude every domain, found: ${cloud.keys}", ALL_DOMAINS, cloud.keys)
        assertEquals(
            "device-transfer must exclude every domain (Android 12+ D2D is on by default), found: ${deviceTransfer.keys}",
            ALL_DOMAINS,
            deviceTransfer.keys
        )
        assertTrue(
            "every exclusion must cover the whole domain (path '.')",
            (cloud.values + deviceTransfer.values).all { it == "." }
        )
    }

    /**
     * Reads `<exclude domain="…" path="…">` entries out of a backup rules resource.
     *
     * @param resId the rules XML to parse
     * @param section when non-null, only exclusions inside that section element are read
     * @return map of excluded domain to its path
     */
    private fun exclusionsIn(resId: Int, section: String?): Map<String, String> {
        val parser: XmlResourceParser = context.resources.getXml(resId)
        val excluded = mutableMapOf<String, String>()
        var currentSection: String? = null
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name
                        if (tag == "cloud-backup" || tag == "device-transfer") {
                            currentSection = tag
                        }
                        if (tag == "exclude" && (section == null || section == currentSection)) {
                            val domain = parser.getAttributeValue(null, "domain")
                            val path = parser.getAttributeValue(null, "path")
                            if (domain != null && path != null) excluded[domain] = path
                        }
                    }

                    XmlPullParser.END_TAG -> if (parser.name == currentSection) {
                        currentSection = null
                    }
                }
                event = parser.next()
            }
        } finally {
            parser.close()
        }
        return excluded
    }

    private companion object {
        val ALL_DOMAINS = setOf(
            "root",
            "file",
            "database",
            "sharedpref",
            "external",
            "device_root",
            "device_file",
            "device_database",
            "device_sharedpref"
        )
    }
}
