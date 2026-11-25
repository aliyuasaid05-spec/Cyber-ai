package com.example.util

import android.util.Base64
import com.example.domain.HashAlgorithm
import com.example.domain.PasswordAnalysis
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.log2
import kotlin.math.pow

object CryptoToolkit {
    private val secureRandom = SecureRandom()

    private const val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val DIGITS = "23456789"
    private const val SYMBOLS = "!@#\$%^&*()-_=+[]{}?"

    fun analyzePassword(password: String): PasswordAnalysis {
        if (password.isEmpty()) {
            return PasswordAnalysis(
                length = 0,
                entropyBits = 0.0,
                strengthLabel = "Empty",
                scoreFraction = 0f,
                estimatedCrackTime = "Instant",
                feedback = listOf("Enter a password or passphrase to measure real entropy.")
            )
        }

        val hasLower = password.any { it in 'a'..'z' }
        val hasUpper = password.any { it in 'A'..'Z' }
        val hasDigit = password.any { it in '0'..'9' }
        val hasSymbol = password.any { !it.isLetterOrDigit() }

        var poolSize = 0
        if (hasLower) poolSize += 26
        if (hasUpper) poolSize += 26
        if (hasDigit) poolSize += 10
        if (hasSymbol) poolSize += 32
        if (poolSize == 0) poolSize = 26

        val uniqueChars = password.toSet().size
        val rawEntropy = password.length * log2(poolSize.toDouble())
        val uniquenessRatio = uniqueChars.toDouble() / password.length.toDouble()
        var effectiveEntropy = rawEntropy * (0.65 + 0.35 * uniquenessRatio)

        val feedback = mutableListOf<String>()
        if (password.length < 12) {
            feedback.add("Use at least 12–16 characters to resist offline GPU cracking.")
        }
        if (!hasUpper || !hasLower) {
            feedback.add("Mix uppercase and lowercase characters.")
        }
        if (!hasDigit) {
            feedback.add("Add numeric digits or unrelated words.")
        }
        if (!hasSymbol) {
            feedback.add("Include special symbols to widen the character pool.")
        }
        if (hasSequentialRun(password)) {
            effectiveEntropy *= 0.78
            feedback.add("Avoid predictable sequences (e.g., 1234, abcd, qwerty).")
        }
        if (hasRepeatedRun(password)) {
            effectiveEntropy *= 0.82
            feedback.add("Avoid repeated characters (e.g., aaa, 111).")
        }

        val clampedBits = effectiveEntropy.coerceAtLeast(0.0)
        val (label, fraction) = when {
            clampedBits < 28 -> "Very Weak" to 0.15f
            clampedBits < 45 -> "Weak" to 0.35f
            clampedBits < 65 -> "Moderate" to 0.60f
            clampedBits < 85 -> "Strong" to 0.82f
            else -> "Very Strong" to 1.0f
        }

        if (feedback.isEmpty()) {
            feedback.add("Strong character diversity and length. Store uniquely in a password manager.")
        }

        return PasswordAnalysis(
            length = password.length,
            entropyBits = clampedBits,
            strengthLabel = label,
            scoreFraction = fraction,
            estimatedCrackTime = estimateOfflineCrackTime(clampedBits),
            feedback = feedback
        )
    }

    fun generateSecurePassword(length: Int = 18, includeSymbols: Boolean = true): String {
        val safeLength = length.coerceIn(10, 48)
        val pools = mutableListOf(LOWER, UPPER, DIGITS)
        if (includeSymbols) pools.add(SYMBOLS)
        val allChars = pools.joinToString("")

        val chars = MutableList(safeLength) { idx ->
            if (idx < pools.size) {
                val pool = pools[idx]
                pool[secureRandom.nextInt(pool.length)]
            } else {
                allChars[secureRandom.nextInt(allChars.length)]
            }
        }

        // Fisher-Yates shuffle using SecureRandom
        for (i in chars.lastIndex downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val tmp = chars[i]
            chars[i] = chars[j]
            chars[j] = tmp
        }
        return chars.joinToString("")
    }

    fun computeHash(input: String, algorithm: HashAlgorithm): String {
        val digest = MessageDigest.getInstance(algorithm.jcaName)
        val bytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun encodeBase64(input: String): String {
        val bytes = input.toByteArray(StandardCharsets.UTF_8)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun decodeBase64(encoded: String): Result<String> {
        return runCatching {
            val cleaned = encoded.trim()
            val decoded = Base64.decode(cleaned, Base64.DEFAULT)
            String(decoded, StandardCharsets.UTF_8)
        }
    }

    private fun estimateOfflineCrackTime(entropyBits: Double): String {
        if (entropyBits <= 1.0) return "Instant"
        // Assume 100 billion (10^11) guesses/sec for fast GPU cluster
        val guessesPerSec = 1e11
        val avgGuesses = 2.0.pow((entropyBits - 1.0).coerceAtMost(128.0))
        val seconds = avgGuesses / guessesPerSec
        return when {
            seconds < 1.0 -> "< 1 second"
            seconds < 60.0 -> "${seconds.toInt()} seconds"
            seconds < 3600.0 -> "${(seconds / 60).toInt()} minutes"
            seconds < 86400.0 -> "${(seconds / 3600).toInt()} hours"
            seconds < 31_536_000.0 -> "${(seconds / 86400).toInt()} days"
            seconds < 31_536_000_000.0 -> "${(seconds / 31_536_000.0).toInt()} years"
            seconds < 3.15e13 -> "${(seconds / 31_536_000_000.0).toInt()} millennia"
            else -> "Centuries+"
        }
    }

    private fun hasSequentialRun(value: String): Boolean {
        val lower = value.lowercase()
        var asc = 1
        var desc = 1
        for (i in 1 until lower.length) {
            val diff = lower[i].code - lower[i - 1].code
            asc = if (diff == 1) asc + 1 else 1
            desc = if (diff == -1) desc + 1 else 1
            if (asc >= 4 || desc >= 4) return true
        }
        return false
    }

    private fun hasRepeatedRun(value: String): Boolean {
        var streak = 1
        for (i in 1 until value.length) {
            streak = if (value[i] == value[i - 1]) streak + 1 else 1
            if (streak >= 3) return true
        }
        return false
    }
}
