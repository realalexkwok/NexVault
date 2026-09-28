package com.nexvault.wallet.core.network.adapter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

/**
 * Roadmap 2.0.6 (TC traceability): the adapter-level cases of `doc/07-TEST-CASES.md` had no
 * test home — the three pre-existing core-network test files are explorer-shaped. This file
 * gives TC-NET-001 its mapped test method.
 *
 * The precision-loss trap is real: `fromJson` first routes a JSON **number** through
 * `JsonReader.nextDouble()`, so only the string path can represent an 18-digit decimal
 * exactly. The mapped test therefore feeds a string token, exactly as the spec says.
 */
class BigDecimalAdapterTest {
    private val moshi: Moshi =
        Moshi
            .Builder()
            .add(BigDecimalAdapter())
            .build()

    // TC-NET-001: a JSON string "123456789.123456789" must parse exactly, without precision loss.
    @Test
    fun stringToken_parsesExactlyWithoutPrecisionLoss() {
        val adapter = moshi.adapter(BigDecimal::class.java)

        val result = adapter.fromJson("\"123456789.123456789\"")

        assertThat(result).isEqualTo(BigDecimal("123456789.123456789"))
    }

    // The double route of the same value is lossy; documenting the contrast the spec implies.
    @Test
    fun numberToken_parsesThroughTheDoubleRouteAndLosesPrecision() {
        val adapter = moshi.adapter(BigDecimal::class.java)

        val result = adapter.fromJson("123456789.123456789")

        assertThat(result).isNotEqualTo(BigDecimal("123456789.123456789"))
    }

    @Test
    fun nonNumericString_throwsJsonDataException() {
        val adapter = moshi.adapter(BigDecimal::class.java)

        assertThrows(JsonDataException::class.java) {
            adapter.fromJson("\"not-a-number\"")
        }
    }
}
