package com.nexvault.wallet.core.security.encryption

import javax.crypto.SecretKey

data class DerivedKeyResult(
    val key: SecretKey,
    val salt: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DerivedKeyResult

        if (!key.equals(other.key)) return false
        if (!salt.contentEquals(other.salt)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + salt.contentHashCode()
        return result
    }
}

data class PasswordEncryptedData(
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val salt: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PasswordEncryptedData

        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (!salt.contentEquals(other.salt)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        return result
    }
}
