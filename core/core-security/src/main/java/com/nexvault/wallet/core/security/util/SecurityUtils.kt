package com.nexvault.wallet.core.security.util

import org.web3j.crypto.Hash
import java.security.SecureRandom

object SecureUtils {

    fun ByteArray.secureWipe() {
        for (i in indices) {
            this[i] = 0
        }
    }

    fun CharArray.secureWipe() {
        for (i in indices) {
            this[i] = '\u0000'
        }
    }

    fun secureWipeString(value: String) {
        try {
            val charArrayField = value.javaClass.getDeclaredField("value")
            charArrayField.isAccessible = true
            val chars = charArrayField.get(value) as CharArray
            chars.secureWipe()
        } catch (e: Exception) {
            // Best effort - String is immutable in JVM
        }
    }
}

object SecurityUtils {

    private val HEX_CHARS = "0123456789abcdef".toCharArray()
    private const val HASH_ITERATIONS = 65536
    private const val HASH_LENGTH = 32

    fun generateSecureRandom(length: Int): ByteArray {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes
    }

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false

        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }

    fun ByteArray.toHex(): String {
        val result = StringBuilder(size * 2)
        forEach { byte ->
            val i = byte.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }
        return result.toString()
    }

    fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0) { "Hex string must have even length" }

        val len = length / 2
        val result = ByteArray(len)
        for (i in 0 until len) {
            val high = Character.digit(this[i * 2], 16) shl 4
            val low = Character.digit(this[i * 2 + 1], 16)
            result[i] = (high or low).toByte()
        }
        return result
    }

    fun checksumAddress(address: String): String {
        if (!isValidEthereumAddress(address)) {
            return address
        }

        val cleanAddress = address.removePrefix("0x").lowercase()
        // EIP-55 hashes the lowercase hex form with Keccak-256 — NOT NIST SHA3-256, which uses
        // different padding. Each character is uppercased when the matching nibble of the hash
        // is >= 8.
        val hash = Hash.sha3(cleanAddress.toByteArray(Charsets.UTF_8))

        return buildString {
            append("0x")
            for (i in cleanAddress.indices) {
                val hashByte = hash[i / 2].toInt()
                val hashNibble = if (i % 2 == 0) {
                    (hashByte shr 4) and 0x0f
                } else {
                    hashByte and 0x0f
                }
                if (hashNibble >= 8) {
                    append(cleanAddress[i].uppercaseChar())
                } else {
                    append(cleanAddress[i])
                }
            }
        }
    }

    fun isValidEthereumAddress(address: String): Boolean {
        if (!address.startsWith("0x")) return false
        if (address.length != 42) return false

        val hexPart = address.substring(2)
        return hexPart.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
    }

    fun hashPassword(password: String): String {
        val salt = generateSecureRandom(16)
        val spec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt,
            HASH_ITERATIONS,
            HASH_LENGTH * 8
        )

        try {
            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = factory.generateSecret(spec).encoded

            // Combine salt and hash: salt(16 bytes) + hash(32 bytes)
            val result = ByteArray(salt.size + hash.size)
            System.arraycopy(salt, 0, result, 0, salt.size)
            System.arraycopy(hash, 0, result, salt.size, hash.size)

            return result.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    fun verifyPassword(password: String, storedHashHex: String): Boolean {
        return try {
            val storedHashBytes = storedHashHex.hexToByteArray()
            val salt = storedHashBytes.copyOfRange(0, 16)
            val expectedHash = storedHashBytes.copyOfRange(16, storedHashBytes.size)

            val spec = javax.crypto.spec.PBEKeySpec(
                password.toCharArray(),
                salt,
                HASH_ITERATIONS,
                HASH_LENGTH * 8
            )

            try {
                val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val actualHash = factory.generateSecret(spec).encoded

                constantTimeEquals(expectedHash, actualHash)
            } finally {
                spec.clearPassword()
            }
        } catch (e: Exception) {
            false
        }
    }
}
