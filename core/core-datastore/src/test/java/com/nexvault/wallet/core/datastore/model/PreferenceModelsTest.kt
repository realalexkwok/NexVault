package com.nexvault.wallet.core.datastore.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceModelsTest {

    @Test
    fun themeMode_hasAllExpectedValues() {
        val values = ThemeMode.entries
        assertEquals(3, values.size)
        assert(ThemeMode.LIGHT in values)
        assert(ThemeMode.DARK in values)
        assert(ThemeMode.SYSTEM in values)
    }

    @Test
    fun networkType_hasAllExpectedValues() {
        val values = NetworkType.entries
        assertEquals(4, values.size)
        assert(NetworkType.MAINNET in values)
        assert(NetworkType.SEPOLIA in values)
        assert(NetworkType.BSC in values)
        assert(NetworkType.POLYGON in values)
    }

    @Test
    fun autoLockTimeout_secondsValuesAreCorrect() {
        assertEquals(0, AutoLockTimeout.IMMEDIATE.seconds)
        assertEquals(60, AutoLockTimeout.ONE_MINUTE.seconds)
        assertEquals(300, AutoLockTimeout.FIVE_MINUTES.seconds)
        assertEquals(900, AutoLockTimeout.FIFTEEN_MINUTES.seconds)
        assertEquals(1800, AutoLockTimeout.THIRTY_MINUTES.seconds)
    }

    @Test
    fun autoLockTimeout_neverHasNegativeOne() {
        assertEquals(-1, AutoLockTimeout.NEVER.seconds)
    }

    @Test
    fun autoLockTimeout_fromSeconds_returnsCorrectTimeout() {
        assertEquals(AutoLockTimeout.IMMEDIATE, AutoLockTimeout.fromSeconds(0))
        assertEquals(AutoLockTimeout.ONE_MINUTE, AutoLockTimeout.fromSeconds(60))
        assertEquals(AutoLockTimeout.FIVE_MINUTES, AutoLockTimeout.fromSeconds(300))
        assertEquals(AutoLockTimeout.FIFTEEN_MINUTES, AutoLockTimeout.fromSeconds(900))
        assertEquals(AutoLockTimeout.THIRTY_MINUTES, AutoLockTimeout.fromSeconds(1800))
        assertEquals(AutoLockTimeout.NEVER, AutoLockTimeout.fromSeconds(-1))
    }

    @Test
    fun autoLockTimeout_fromSeconds_returnsDefaultForInvalidValue() {
        assertEquals(AutoLockTimeout.FIVE_MINUTES, AutoLockTimeout.fromSeconds(999))
    }
}
