package com.v7lthronyx.scamynx.data.passwordsecurity

import com.v7lthronyx.scamynx.domain.model.PasswordPatternWarning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHeuristicsTest {

    @Test
    fun `entropy grows with length and charset size`() {
        val lowEntropy = calculateEntropy("aaaaaa")
        val mixedEntropy = calculateEntropy("Aa1!BB")
        val longMixed = calculateEntropy("Aa1!securePass123")

        assertTrue(mixedEntropy.entropyBits > lowEntropy.entropyBits)
        assertTrue(longMixed.entropyBits > mixedEntropy.entropyBits)
        assertTrue(longMixed.categoriesUsed >= mixedEntropy.categoriesUsed)
    }

    @Test
    fun `common patterns are detected`() {
        val warnings = detectPatternWarnings("Password1234")
        assertTrue(warnings.contains(PasswordPatternWarning.COMMON_WORD))
        assertTrue(warnings.contains(PasswordPatternWarning.SEQUENTIAL_CHARS))
    }

    @Test
    fun `year and keyboard patterns flagged`() {
        val yearWarnings = detectPatternWarnings("MyPass2001!!")
        assertTrue(yearWarnings.contains(PasswordPatternWarning.YEAR_PATTERN))

        val keyboardWarnings = detectPatternWarnings("Qwer!234")
        assertTrue(keyboardWarnings.contains(PasswordPatternWarning.KEYBOARD_PATTERN))
    }

    @Test
    fun `strength score responds to warnings and breaches`() {
        val strong = computeStrengthScore(
            entropyBits = 80.0,
            length = 20,
            categoriesUsed = 4,
            warningCount = 0,
            breachCount = 0,
        )
        val weak = computeStrengthScore(
            entropyBits = 10.0,
            length = 6,
            categoriesUsed = 1,
            warningCount = 3,
            breachCount = 5,
        )

        assertTrue(strong.score > weak.score)
        assertEquals(0.15, weak.score, 0.001)
    }
}
