package com.v7lthronyx.scamynx.data.repository

import com.v7lthronyx.scamynx.domain.model.MlReport
import com.v7lthronyx.scamynx.domain.model.NetworkReport
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.model.VendorVerdict
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScorerTest {

    @Test
    fun aggregateRisk_multipleMaliciousSources_yieldsCriticalScore() {
        val vendors = listOf(
            vendor(Provider.VIRUS_TOTAL, VerdictStatus.MALICIOUS, 1.0),
            vendor(Provider.URL_HAUS, VerdictStatus.MALICIOUS, 0.9),
            vendor(Provider.PHISH_STATS, VerdictStatus.SUSPICIOUS, 0.7),
        )

        val (risk, breakdown) = RiskScorer.aggregateRisk(
            targetType = ScanTargetType.URL,
            vendorVerdicts = vendors,
            mlReport = MlReport(probability = 0.82, topFeatures = emptyList()),
            networkReport = null,
            fileReport = null,
            vpnReport = null,
            instagramReport = null,
        )

        assertTrue("risk should be in critical range", risk > 4.5)
        val criticalShare = breakdown.categories[RiskCategory.CRITICAL] ?: 0.0
        assertTrue("critical membership should dominate", criticalShare > 0.55)
    }

    @Test
    fun aggregateRisk_cleanSignalsSuppressScoreWhenMlLow() {
        val vendors = listOf(
            vendor(Provider.VIRUS_TOTAL, VerdictStatus.CLEAN, 0.0),
            vendor(Provider.GOOGLE_SAFE_BROWSING, VerdictStatus.CLEAN, 0.0),
        )

        val (risk, breakdown) = RiskScorer.aggregateRisk(
            targetType = ScanTargetType.URL,
            vendorVerdicts = vendors,
            mlReport = MlReport(probability = 0.1, topFeatures = emptyList()),
            networkReport = null,
            fileReport = null,
            vpnReport = null,
            instagramReport = null,
        )

        assertTrue("risk should remain very low", risk < 0.3)
        val minimalShare = breakdown.categories[RiskCategory.MINIMAL] ?: 0.0
        assertTrue("minimal membership should dominate", minimalShare > 0.7)
    }

    @Test
    fun aggregateRisk_networkWeaknessesElevateScore() {
        val vendors = listOf(
            vendor(Provider.GOOGLE_SAFE_BROWSING, VerdictStatus.SUSPICIOUS, 0.4),
        )

        val strongNetwork = NetworkReport(
            tlsVersion = "TLS 1.3",
            cipherSuite = "TLS_AES_128_GCM_SHA256",
            certValid = true,
            headers = mapOf(
                "Strict-Transport-Security" to "max-age=63072000; includeSubDomains",
                "Content-Security-Policy" to "default-src 'self'",
                "X-Frame-Options" to "DENY",
                "X-Content-Type-Options" to "nosniff",
                "Referrer-Policy" to "no-referrer",
            ),
            dnssecSignal = true,
        )

        val weakNetwork = NetworkReport(
            tlsVersion = null,
            cipherSuite = null,
            certValid = false,
            headers = emptyMap(),
            dnssecSignal = false,
        )

        val strongRisk = RiskScorer.aggregateRisk(
            targetType = ScanTargetType.URL,
            vendorVerdicts = vendors,
            mlReport = MlReport(probability = 0.35, topFeatures = emptyList()),
            networkReport = strongNetwork,
            fileReport = null,
            vpnReport = null,
            instagramReport = null,
        ).first

        val weakRisk = RiskScorer.aggregateRisk(
            targetType = ScanTargetType.URL,
            vendorVerdicts = vendors,
            mlReport = MlReport(probability = 0.35, topFeatures = emptyList()),
            networkReport = weakNetwork,
            fileReport = null,
            vpnReport = null,
            instagramReport = null,
        ).first

        assertTrue("weak network should increase risk", weakRisk > strongRisk)
        assertTrue("network penalty should be substantial", (weakRisk - strongRisk) > 1.0)
    }

    private fun vendor(
        provider: Provider,
        status: VerdictStatus,
        score: Double,
    ): VendorVerdict = VendorVerdict(
        provider = provider,
        status = status,
        score = score,
        details = emptyMap(),
    )

    @Test
    fun adjustForConfidence_shiftsRiskTowardTrustedSignals() {
        val method = RiskScorer::class.java.getDeclaredMethod(
            "adjustForConfidence",
            Double::class.javaPrimitiveType,
            Double::class.javaPrimitiveType,
            Double::class.javaPrimitiveType,
        ).apply { isAccessible = true }

        val highConfidenceMalicious = method.invoke(RiskScorer, 0.6, 0.9, 0.9) as Double
        val lowConfidenceMalicious = method.invoke(RiskScorer, 0.6, 0.9, 0.2) as Double
        assertTrue("high confidence malicious signals should increase risk more", highConfidenceMalicious > lowConfidenceMalicious)

        val highConfidenceClean = method.invoke(RiskScorer, 0.4, 0.1, 0.9) as Double
        val lowConfidenceClean = method.invoke(RiskScorer, 0.4, 0.1, 0.2) as Double
        assertTrue("trusted clean signals should suppress risk further", highConfidenceClean < lowConfidenceClean)

        val dampened = method.invoke(RiskScorer, 0.6, 0.9, 0.1) as Double
        assertTrue("very low confidence should dampen risk", dampened < lowConfidenceMalicious)
    }
}
