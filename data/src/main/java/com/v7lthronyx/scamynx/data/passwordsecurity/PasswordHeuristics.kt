package com.v7lthronyx.scamynx.data.passwordsecurity

import com.v7lthronyx.scamynx.domain.model.PasswordPatternWarning
import com.v7lthronyx.scamynx.domain.model.PasswordRecommendation
import kotlin.math.log2
import kotlin.math.min
import kotlin.math.roundToInt

internal data class PasswordEntropySnapshot(
    val length: Int,
    val entropyBits: Double,
    val charsetSize: Int,
    val categoriesUsed: Int,
)

private val commonWords = listOf(
    "password",
    "qwerty",
    "123456",
    "letmein",
    "admin",
    "welcome",
    "dragon",
    "football",
    "princess",
    "linkedin",
    "abc123",
)

private val keyboardRows = listOf(
    "1234567890",
    "qwertyuiop",
    "asdfghjkl",
    "zxcvbnm",
)

internal fun calculateEntropy(password: String): PasswordEntropySnapshot {
    if (password.isEmpty()) return PasswordEntropySnapshot(0, 0.0, 0, 0)
    var charsetSize = 0
    var categories = 0
    if (password.any { it.isLowerCase() }) {
        charsetSize += 26
        categories++
    }
    if (password.any { it.isUpperCase() }) {
        charsetSize += 26
        categories++
    }
    if (password.any { it.isDigit() }) {
        charsetSize += 10
        categories++
    }
    if (password.any { !it.isLetterOrDigit() }) {
        charsetSize += 32
        categories++
    }
    if (charsetSize == 0) charsetSize = 10
    val entropyBits = password.length * log2(charsetSize.toDouble())
    return PasswordEntropySnapshot(
        length = password.length,
        entropyBits = entropyBits,
        charsetSize = charsetSize,
        categoriesUsed = categories,
    )
}

internal fun detectPatternWarnings(password: String): List<PasswordPatternWarning> {
    if (password.isBlank()) return emptyList()
    val lower = password.lowercase()
    val warnings = mutableSetOf<PasswordPatternWarning>()
    if (commonWords.any { lower.contains(it) }) {
        warnings += PasswordPatternWarning.COMMON_WORD
    }

    if (containsSequentialCharacters(lower)) {
        warnings += PasswordPatternWarning.SEQUENTIAL_CHARS
    }

    if (Regex("(.)\\1{2,}").containsMatchIn(lower)) {
        warnings += PasswordPatternWarning.REPEATED_CHARS
    }

    if (keyboardRows.any { row -> containsSubsequence(lower, row) }) {
        warnings += PasswordPatternWarning.KEYBOARD_PATTERN
    }

    if (Regex("19\\d{2}|20\\d{2}").containsMatchIn(lower)) {
        warnings += PasswordPatternWarning.YEAR_PATTERN
    }
    return warnings.toList()
}

private fun containsSequentialCharacters(value: String): Boolean {
    if (value.length < 4) return false
    var ascendingRun = 1
    var descendingRun = 1
    for (index in 1 until value.length) {
        val diff = value[index] - value[index - 1]
        when (diff) {
            1 -> {
                ascendingRun++
                descendingRun = 1
            }
            -1 -> {
                descendingRun++
                ascendingRun = 1
            }
            else -> {
                ascendingRun = 1
                descendingRun = 1
            }
        }
        if (ascendingRun >= 4 || descendingRun >= 4) return true
    }
    return false
}

private fun containsSubsequence(value: String, sample: String): Boolean {
    if (value.length < 4) return false
    for (i in 0..sample.length - 4) {
        val segment = sample.substring(i, i + 4)
        if (value.contains(segment) || value.contains(segment.reversed())) return true
    }
    return false
}

internal data class PasswordScoringSnapshot(
    val score: Double,
    val strengthPercent: Int,
)

internal fun computeStrengthScore(
    entropyBits: Double,
    length: Int,
    categoriesUsed: Int,
    warningCount: Int,
    breachCount: Int,
): PasswordScoringSnapshot {
    val normalizedEntropy = min(entropyBits / 80.0, 1.0)
    val lengthScore = min(length / 18.0, 1.0)
    val diversityScore = (categoriesUsed / 4.0).coerceIn(0.0, 1.0)
    var score = (normalizedEntropy * 0.55) + (lengthScore * 0.25) + (diversityScore * 0.2)
    val warningPenalty = warningCount * 0.08
    score -= warningPenalty
    
    score = score.coerceIn(0.0, 1.0)
    if (breachCount > 0) {
        
        score = 0.15
    }
    return PasswordScoringSnapshot(
        score = score,
        strengthPercent = (score * 100).roundToInt(),
    )
}

internal fun buildRecommendations(
    password: String,
    warnings: List<PasswordPatternWarning>,
    breached: Boolean,
    entropySnapshot: PasswordEntropySnapshot,
): List<PasswordRecommendation> {
    val recommendations = mutableSetOf<PasswordRecommendation>()
    if (password.length < 12) {
        recommendations += PasswordRecommendation.INCREASE_LENGTH
    }
    if (entropySnapshot.categoriesUsed <= 2) {
        recommendations += PasswordRecommendation.ADD_VARIETY
    }
    if (warnings.isNotEmpty()) {
        recommendations += PasswordRecommendation.AVOID_COMMON_PATTERNS
    }
    recommendations += PasswordRecommendation.USE_PASSWORD_MANAGER
    recommendations += PasswordRecommendation.ENABLE_2FA
    if (breached) {
        recommendations += PasswordRecommendation.RESET_BREACHED_PASSWORDS
    }
    return recommendations.toList()
}
