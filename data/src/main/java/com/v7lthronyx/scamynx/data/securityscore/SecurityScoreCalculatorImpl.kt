package com.v7lthronyx.scamynx.data.securityscore

import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.SecurityBadge
import com.v7lthronyx.scamynx.domain.model.SecurityCategory
import com.v7lthronyx.scamynx.domain.model.SecurityIssue
import com.v7lthronyx.scamynx.domain.model.SecurityScoreComponent
import com.v7lthronyx.scamynx.domain.model.SecurityScoreReport
import com.v7lthronyx.scamynx.domain.model.SecurityStatus
import com.v7lthronyx.scamynx.domain.service.PasswordSecurityAnalyzer
import com.v7lthronyx.scamynx.domain.service.SecurityScoreCalculator
import com.v7lthronyx.scamynx.domain.service.WifiSecurityAnalyzer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityScoreCalculatorImpl @Inject constructor(
    private val passwordSecurityAnalyzer: PasswordSecurityAnalyzer,
    private val wifiSecurityAnalyzer: WifiSecurityAnalyzer,
    private val privacyRadarScoreProvider: PrivacyRadarScoreProvider,
    private val deviceHardeningScoreProvider: DeviceHardeningScoreProvider,
    private val breachExposureScoreProvider: BreachExposureScoreProvider,
    private val clock: Clock = Clock.System,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : SecurityScoreCalculator {

    override suspend fun calculateSecurityScore(): SecurityScoreReport = withContext(dispatcher) {
        coroutineScope {
            
            val passwordScore = async { calculatePasswordSecurityScore() }
            val wifiScore = async { calculateWifiSecurityScore() }
            val privacyRadarScore = async { privacyRadarScoreProvider.getScore() }
            val deviceHardeningScore = async { deviceHardeningScoreProvider.getScore() }
            val breachExposureScore = async { breachExposureScoreProvider.getScore() }

            val components = listOf(
                passwordScore.await(),
                wifiScore.await(),
                privacyRadarScore.await(),
                deviceHardeningScore.await(),
                breachExposureScore.await(),
            )

            val overallScore = calculateOverallScore(components)
            val status = mapScoreToStatus(overallScore)

            val topRecommendations = generateTopRecommendations(components)

            val badge = createSecurityBadge(overallScore, status)

            SecurityScoreReport(
                overallScore = overallScore,
                status = status,
                components = components,
                topRecommendations = topRecommendations,
                shareableBadge = badge,
                timestamp = clock.now(),
            )
        }
    }

    private suspend fun calculatePasswordSecurityScore(): SecurityScoreComponent {
        
        val score = 70
        val issues = mutableListOf<SecurityIssue>()
        val recommendations = mutableListOf<String>()

        if (score < 60) {
            issues += SecurityIssue(
                severity = IssueSeverity.HIGH,
                title = "Weak Passwords Detected",
                description = "Some passwords may be weak or exposed in breaches",
                actionable = true,
                actionLabel = "Check Passwords",
            )
            recommendations += "Use the Password Security Checker to identify weak passwords"
            recommendations += "Enable 2FA on all important accounts"
        }

        return SecurityScoreComponent(
            category = SecurityCategory.PASSWORD_SECURITY,
            score = score,
            weight = 0.20,
            status = mapScoreToStatus(score),
            issues = issues,
            recommendations = recommendations,
        )
    }

    private suspend fun calculateWifiSecurityScore(): SecurityScoreComponent {
        val assessment = wifiSecurityAnalyzer.analyzeCurrentNetwork()
        val score = if (assessment != null) {
            
            ((1.0 - assessment.riskScore) * 100).toInt()
        } else {
            50
        }

        val issues = mutableListOf<SecurityIssue>()
        val recommendations = mutableListOf<String>()

        if (assessment != null) {
            when (assessment.riskCategory) {
                com.v7lthronyx.scamynx.domain.model.RiskCategory.HIGH -> {
                    issues += SecurityIssue(
                        severity = IssueSeverity.CRITICAL,
                        title = "Unsafe Wi-Fi Network",
                        description = "Current network has high security risks",
                        actionable = true,
                        actionLabel = "View Details",
                    )
                }
                com.v7lthronyx.scamynx.domain.model.RiskCategory.MEDIUM -> {
                    issues += SecurityIssue(
                        severity = IssueSeverity.MEDIUM,
                        title = "Moderate Wi-Fi Risk",
                        description = "Current network has some security concerns",
                        actionable = true,
                        actionLabel = "View Details",
                    )
                }
                else -> {}
            }
            recommendations.addAll(assessment.recommendations)
        }

        return SecurityScoreComponent(
            category = SecurityCategory.WIFI_SECURITY,
            score = score,
            weight = 0.15,
            status = mapScoreToStatus(score),
            issues = issues,
            recommendations = recommendations,
        )
    }

    private fun calculateOverallScore(components: List<SecurityScoreComponent>): Int {
        val weightedSum = components.sumOf { it.score * it.weight }
        val totalWeight = components.sumOf { it.weight }
        return if (totalWeight > 0) {
            (weightedSum / totalWeight).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    private fun mapScoreToStatus(score: Int): SecurityStatus = when {
        score >= 80 -> SecurityStatus.EXCELLENT
        score >= 60 -> SecurityStatus.GOOD
        score >= 40 -> SecurityStatus.FAIR
        score >= 20 -> SecurityStatus.POOR
        else -> SecurityStatus.CRITICAL
    }

    private fun generateTopRecommendations(components: List<SecurityScoreComponent>): List<String> {
        return components
            .flatMap { it.recommendations }
            .distinct()
            .take(5)
    }

    private fun createSecurityBadge(score: Int, status: SecurityStatus): SecurityBadge {
        val badgeText = when (status) {
            SecurityStatus.EXCELLENT -> "🛡️ Excellent"
            SecurityStatus.GOOD -> "✅ Good"
            SecurityStatus.FAIR -> "⚠️ Fair"
            SecurityStatus.POOR -> "🔴 Poor"
            SecurityStatus.CRITICAL -> "🚨 Critical"
        }

        val badgeColor = when (status) {
            SecurityStatus.EXCELLENT -> "#19D6A3"
            SecurityStatus.GOOD -> "#4FD8F8"
            SecurityStatus.FAIR -> "#FFA14A"
            SecurityStatus.POOR -> "#FF5C7A"
            SecurityStatus.CRITICAL -> "#B1203B"
        }

        val shareableText = "My device security score: $score/100 ($badgeText) - Checked with SCAMYNX 🛡️"

        return SecurityBadge(
            score = score,
            status = status,
            badgeText = badgeText,
            badgeColor = badgeColor,
            shareableText = shareableText,
        )
    }
}

@Singleton
class PrivacyRadarScoreProvider @Inject constructor() {
    suspend fun getScore(): SecurityScoreComponent {
        
        return SecurityScoreComponent(
            category = SecurityCategory.PRIVACY_RADAR,
            score = 75,
            weight = 0.25,
            status = SecurityStatus.GOOD,
            issues = emptyList(),
            recommendations = listOf("Review app permissions regularly"),
        )
    }
}

@Singleton
class DeviceHardeningScoreProvider @Inject constructor() {
    suspend fun getScore(): SecurityScoreComponent {
        
        return SecurityScoreComponent(
            category = SecurityCategory.DEVICE_HARDENING,
            score = 65,
            weight = 0.20,
            status = SecurityStatus.GOOD,
            issues = emptyList(),
            recommendations = listOf("Use One-Tap Device Hardening to improve security"),
        )
    }
}

@Singleton
class BreachExposureScoreProvider @Inject constructor() {
    suspend fun getScore(): SecurityScoreComponent {
        
        return SecurityScoreComponent(
            category = SecurityCategory.BREACH_EXPOSURE,
            score = 80,
            weight = 0.20,
            status = SecurityStatus.EXCELLENT,
            issues = emptyList(),
            recommendations = listOf("Enable Dark Web Breach Monitoring (Premium)"),
        )
    }
}
