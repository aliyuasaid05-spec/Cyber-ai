package com.example

import com.example.domain.HashAlgorithm
import com.example.util.CryptoToolkit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CryptoToolkitTest {

    @Test
    fun `password analysis scores strong generated password high`() {
        val generated = CryptoToolkit.generateSecurePassword(length = 18, includeSymbols = true)
        assertEquals(18, generated.length)

        val analysis = CryptoToolkit.analyzePassword(generated)
        assertTrue(analysis.entropyBits > 65.0)
        assertTrue(analysis.scoreFraction >= 0.8f)
    }

    @Test
    fun `password analysis flags weak sequential passwords`() {
        val weak = CryptoToolkit.analyzePassword("abcd1234")
        assertTrue(weak.entropyBits < 45.0)
        assertTrue(weak.feedback.isNotEmpty())
    }

    @Test
    fun `sha256 digest matches known vector`() {
        val hash = CryptoToolkit.computeHash("abc", HashAlgorithm.SHA256)
        assertEquals(64, hash.length)
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hash
        )
    }

    @Test
    fun `base64 encode and decode roundtrip`() {
        val original = "Cyber AI Threat Defense"
        val encoded = CryptoToolkit.encodeBase64(original)
        val decoded = CryptoToolkit.decodeBase64(encoded).getOrThrow()
        assertEquals(original, decoded)
    }
}
