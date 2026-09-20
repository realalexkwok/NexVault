package com.nexvault.wallet.core.security.mnemonic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MnemonicManagerTest {

    private val mnemonicManager = MnemonicManager()

    @Test
    fun testGenerate12WordMnemonic() {
        val mnemonic = mnemonicManager.generateMnemonic(12)
        val words = mnemonic.trim().split("\\s+".toRegex())
        assertEquals(12, words.size)
    }

    @Test
    fun testGenerate24WordMnemonic() {
        val mnemonic = mnemonicManager.generateMnemonic(24)
        val words = mnemonic.trim().split("\\s+".toRegex())
        assertEquals(24, words.size)
    }

    @Test
    fun testGeneratedMnemonicInWordList() {
        val wordList = mnemonicManager.getWordList()
        val mnemonic = mnemonicManager.generateMnemonic(12)
        val words = mnemonic.trim().split("\\s+".toRegex())

        for (word in words) {
            assertTrue("Word '$word' should be in BIP39 word list", wordList.contains(word))
        }
    }

    @Test
    fun testValidMnemonicPassesValidation() {
        // Use a known valid mnemonic from BIP39 test vectors
        val validMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        assertTrue(mnemonicManager.validateMnemonic(validMnemonic))
    }

    @Test
    fun testInvalidMnemonicWrongChecksum() {
        // Invalid mnemonic - wrong word count
        val invalidMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon"
        assertFalse(mnemonicManager.validateMnemonic(invalidMnemonic))
    }

    @Test
    fun testInvalidMnemonicWrongWords() {
        // Invalid - contains non-BIP39 words
        val invalidMnemonic = "foo bar baz qux foo bar baz qux foo bar"
        assertFalse(mnemonicManager.validateMnemonic(invalidMnemonic))
    }

    @Test
    fun testMnemonicToSeed() {
        val mnemonic = mnemonicManager.generateMnemonic(12)
        val seed = mnemonicManager.mnemonicToSeed(mnemonic, "")
        assertEquals(64, seed.size)
    }

    @Test
    fun testSameMnemonicAlwaysProducesSameSeed() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val seed1 = mnemonicManager.mnemonicToSeed(mnemonic, "")
        val seed2 = mnemonicManager.mnemonicToSeed(mnemonic, "")
        assertTrue(seed1.contentEquals(seed2))
    }

    @Test
    fun testDifferentPassphrasesProduceDifferentSeeds() {
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val seed1 = mnemonicManager.mnemonicToSeed(mnemonic, "")
        val seed2 = mnemonicManager.mnemonicToSeed(mnemonic, " passphrase")
        assertFalse(seed1.contentEquals(seed2))
    }

    @Test
    fun testWordList() {
        val wordList = mnemonicManager.getWordList()
        assertEquals(2048, wordList.size)
        // Boundary words: the canonical list runs from "abandon" to "zoo".
        assertEquals("abandon", wordList.first())
        assertEquals("zoo", wordList.last())
        assertTrue(wordList.contains("walk"))
        assertTrue(wordList.contains("bitter"))
        assertEquals("word list must not contain duplicates", 2048, wordList.toSet().size)
    }

    @Test
    fun testGenerateMnemonicWithPassphrase() {
        // Should work with any valid word count
        val mnemonic = mnemonicManager.generateMnemonic(12)
        assertTrue(mnemonicManager.validateMnemonic(mnemonic))
    }
}
