package com.nexvault.wallet.core.network.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Roadmap 2.0.6 (TC traceability): mapped home for TC-NET-002 and TC-NET-003.
 *
 * Deviation recorded in the 2.0.6 matrix: `doc/07-TEST-CASES.md` names an `AddressAdapter`
 * that throws `JsonDataException` for invalid input, but the shipped API is
 * `AddressValidator.isValid(address): Boolean` (no exception). The TC's intent — a 42-char
 * `0x`-prefixed address is accepted, anything else is rejected — is covered by the boolean
 * contract; `doc/07` stays frozen as the definition source.
 */
class AddressValidatorTest {
    // TC-NET-002: 42 chars with a 0x prefix is accepted.
    @Test
    fun fortyTwoCharHexAddressWith0xPrefix_isAccepted() {
        assertThat(AddressValidator.isValid("0x1234567890abcdef1234567890abcdef12345678")).isTrue()
    }

    // TC-NET-003: no 0x prefix and too short is rejected.
    @Test
    fun addressWithout0xPrefix_isRejected() {
        assertThat(AddressValidator.isValid("1234567890abcdef")).isFalse()
    }

    @Test
    fun addressWith0xPrefixButWrongLength_isRejected() {
        assertThat(AddressValidator.isValid("0x1234567890abcdef")).isFalse()
    }
}
