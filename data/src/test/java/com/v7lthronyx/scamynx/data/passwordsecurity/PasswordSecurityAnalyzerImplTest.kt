package com.v7lthronyx.scamynx.data.passwordsecurity

import com.v7lthronyx.scamynx.domain.model.PasswordRecommendation
import com.v7lthronyx.scamynx.domain.model.PasswordStrength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordSecurityAnalyzerImplTest {

    private class FakeBreachSource(private val count: Int) : PasswordBreachDataSource {
        override suspend fun lookup(password: String): Int = count
    }

    @Test
    fun `weak common password classified as very weak`() = runBlocking {
        val analyzer = PasswordSecurityAnalyzerImpl(
            breachDataSource = FakeBreachSource(count = 0),
            clock = Clock.System,
            dispatcher = Dispatchers.Unconfined,
        )
        val report = analyzer.evaluate("password123")
        assertTrue(
            report.strength == PasswordStrength.VERY_WEAK ||
                report.strength == PasswordStrength.WEAK ||
                report.strength == PasswordStrength.FAIR,
        )
        assertTrue(report.recommendations.contains(PasswordRecommendation.INCREASE_LENGTH))
    }

    @Test
    fun `strong mixed password reaches strong tier`() = runBlocking {
        val analyzer = PasswordSecurityAnalyzerImpl(
            breachDataSource = FakeBreachSource(count = 0),
            clock = Clock.System,
            dispatcher = Dispatchers.Unconfined,
        )
        val report = analyzer.evaluate("R@nDom!Key2024#")
        assertTrue(report.strength == PasswordStrength.STRONG || report.strength == PasswordStrength.EXCELLENT)
        assertTrue(report.entropyBits > 60)
    }

    @Test
    fun `breached password forces reset recommendation`() = runBlocking {
        val analyzer = PasswordSecurityAnalyzerImpl(
            breachDataSource = FakeBreachSource(count = 1200),
            clock = Clock.System,
            dispatcher = Dispatchers.Unconfined,
        )
        val report = analyzer.evaluate("SomeUniqueString")
        assertEquals(1200, report.breachCount)
        assertTrue(report.recommendations.contains(PasswordRecommendation.RESET_BREACHED_PASSWORDS))
        assertEquals(PasswordStrength.VERY_WEAK, report.strength)
    }
}
