package com.nexvault.wallet.core.security.mnemonic

import org.web3j.crypto.MnemonicUtils
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MnemonicManager @Inject constructor() {

    private val secureRandom = SecureRandom()

    fun generateMnemonic(wordCount: Int = 12): String {
        require(wordCount == 12 || wordCount == 24) {
            "Word count must be 12 or 24"
        }

        val entropyBits = when (wordCount) {
            12 -> 128
            24 -> 256
            else -> throw IllegalArgumentException("Invalid word count")
        }

        val entropy = ByteArray(entropyBits / 8)
        secureRandom.nextBytes(entropy)

        return MnemonicUtils.generateMnemonic(entropy)
    }

    fun validateMnemonic(mnemonic: String): Boolean {
        val words = mnemonic.trim().split("\\s+".toRegex())
        if (words.size != 12 && words.size != 24) {
            return false
        }

        return try {
            MnemonicUtils.validateMnemonic(mnemonic)
        } catch (e: Exception) {
            false
        }
    }

    fun mnemonicToSeed(mnemonic: String, passphrase: String): ByteArray {
        return MnemonicUtils.generateSeed(mnemonic, passphrase)
    }

    /**
     * Returns the canonical 2048-word BIP-39 English word list.
     *
     * Delegates to web3j instead of carrying a local copy: the same list backs
     * [generateMnemonic] and [validateMnemonic], so any mnemonic this class produces is
     * guaranteed to be expressible in the list it reports.
     */
    fun getWordList(): List<String> = MnemonicUtils.getWords()
}
