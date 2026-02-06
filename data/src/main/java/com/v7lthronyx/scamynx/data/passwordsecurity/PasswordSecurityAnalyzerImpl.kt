package com.v7lthronyx.scamynx.data.passwordsecurity

import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.domain.model.PasswordSecurityReport
import com.v7lthronyx.scamynx.domain.model.PasswordStrength
import com.v7lthronyx.scamynx.domain.service.PasswordSecurityAnalyzer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordSecurityAnalyzerImpl @Inject constructor(
    private val breachDataSource: PasswordBreachDataSource,
    private val clock: Clock = Clock.System,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PasswordSecurityAnalyzer {

    override suspend fun evaluate(password: String): PasswordSecurityReport = withContext(dispatcher) {
        val trimmed = password.trim()
        val entropySnapshot = calculateEntropy(trimmed)
        val warnings = detectPatternWarnings(trimmed)
        val breachCount = if (trimmed.isEmpty()) 0 else breachDataSource.lookup(trimmed)
        val scoring = computeStrengthScore(
            entropyBits = entropySnapshot.entropyBits,
            length = entropySnapshot.length,
            categoriesUsed = entropySnapshot.categoriesUsed,
            warningCount = warnings.size,
            breachCount = breachCount,
        )
        val recommendations = buildRecommendations(trimmed, warnings, breachCount > 0, entropySnapshot)
        PasswordSecurityReport(
            passwordLength = entropySnapshot.length,
            entropyBits = entropySnapshot.entropyBits,
            strengthScore = scoring.strengthPercent / 100.0,
            strength = mapScoreToStrength(scoring.score),
            breachCount = breachCount,
            warnings = warnings,
            recommendations = recommendations,
            timestamp = clock.now(),
        )
    }

    private fun mapScoreToStrength(score: Double): PasswordStrength = when {
        score < 0.2 -> PasswordStrength.VERY_WEAK
        score < 0.4 -> PasswordStrength.WEAK
        score < 0.65 -> PasswordStrength.FAIR
        score < 0.85 -> PasswordStrength.STRONG
        else -> PasswordStrength.EXCELLENT
    }
}
