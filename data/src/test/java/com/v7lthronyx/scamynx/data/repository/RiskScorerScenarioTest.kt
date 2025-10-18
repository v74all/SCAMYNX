package com.v7lthronyx.scamynx.data.repository

import com.v7lthronyx.scamynx.domain.model.MlReport
import com.v7lthronyx.scamynx.domain.model.NetworkReport
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.model.VendorVerdict
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScorerScenarioTest {

    private data class Scenario(
        val name: String,
        val targetType: ScanTargetType,
        val vendorVerdicts: List<VendorVerdict>,
        val mlReport: MlReport?,
        val networkReport: NetworkReport?,
        val expectedCategory: RiskCategory,
        val expectedScoreRange: ClosedFloatingPointRange<Double>,
    )

    @Test
    fun `scenario snapshots for confidence-weighted scoring`() {
        val scenarios = listOf(
            Scenario(
                name = "High consensus malicious with weak transport",
                targetType = ScanTargetType.URL,
                vendorVerdicts = listOf(
                    vendor(Provider.VIRUS_TOTAL, VerdictStatus.MALICIOUS, 0.95),
                    vendor(Provider.URL_HAUS, VerdictStatus.MALICIOUS, 0.92),
                    vendor(Provider.PHISH_STATS, VerdictStatus.MALICIOUS, 0.88),
                    vendor(Provider.GOOGLE_SAFE_BROWSING, VerdictStatus.MALICIOUS, 0.91),
                ),
                mlReport = MlReport(probability = 0.87),
                networkReport = NetworkReport(
                    tlsVersion = "TLS 1.0",
                    certValid = false,
                    headers = emptyMap(),
                    dnssecSignal = false,
                ),
                expectedCategory = RiskCategory.CRITICAL,
                expectedScoreRange = 4.6..5.0,
            ),
            Scenario(
                name = "Single malicious hit with mostly unknown peers",
                targetType = ScanTargetType.URL,
                vendorVerdicts = listOf(
                    vendor(Provider.VIRUS_TOTAL, VerdictStatus.MALICIOUS, 0.92),
                    vendor(Provider.GOOGLE_SAFE_BROWSING, VerdictStatus.UNKNOWN, 0.2),
                    vendor(Provider.URL_SCAN, VerdictStatus.UNKNOWN, 0.2),
                    vendor(Provider.THREAT_FOX, VerdictStatus.ERROR, 0.0),
                ),
                mlReport = MlReport(probability = 0.46),
                networkReport = null,
                expectedCategory = RiskCategory.MEDIUM,
                expectedScoreRange = 2.1..2.8,
            ),
            Scenario(
                name = "Clean consensus but poor network posture",
                targetType = ScanTargetType.URL,
                vendorVerdicts = listOf(
                    vendor(Provider.VIRUS_TOTAL, VerdictStatus.CLEAN, 0.0),
                    vendor(Provider.GOOGLE_SAFE_BROWSING, VerdictStatus.CLEAN, 0.0),
                    vendor(Provider.URL_SCAN, VerdictStatus.CLEAN, 0.0),
                ),
                mlReport = MlReport(probability = 0.15),
                networkReport = NetworkReport(
                    tlsVersion = null,
                    certValid = false,
                    headers = emptyMap(),
                    dnssecSignal = false,
                ),
                expectedCategory = RiskCategory.LOW,
                expectedScoreRange = 1.2..2.1,
            ),
            Scenario(
                name = "Clean consensus with moderate transport weaknesses",
                targetType = ScanTargetType.URL,
                vendorVerdicts = listOf(
                    vendor(Provider.VIRUS_TOTAL, VerdictStatus.CLEAN, 0.0),
                    vendor(Provider.GOOGLE_SAFE_BROWSING, VerdictStatus.CLEAN, 0.0),
                    vendor(Provider.URL_SCAN, VerdictStatus.CLEAN, 0.0),
                ),
                mlReport = MlReport(probability = 0.05),
                networkReport = NetworkReport(
                    tlsVersion = "TLS 1.2",
                    certValid = false,
                    headers = mapOf(
                        "Strict-Transport-Security" to "max-age=31536000",
                        "Content-Security-Policy" to "default-src 'self'",
                        "X-Frame-Options" to "DENY",
                        "X-Content-Type-Options" to "nosniff",
                        "Referrer-Policy" to "no-referrer",
                    ),
                    dnssecSignal = null,
                ),
                expectedCategory = RiskCategory.MINIMAL,
                expectedScoreRange = 0.6..0.7,
            ),
            Scenario(
                name = "Strong ML signal with light vendor support",
                targetType = ScanTargetType.URL,
                vendorVerdicts = listOf(
                    vendor(Provider.URL_HAUS, VerdictStatus.SUSPICIOUS, 0.62),
                    vendor(Provider.PHISH_STATS, VerdictStatus.UNKNOWN, 0.35),
                    vendor(Provider.GOOGLE_SAFE_BROWSING, VerdictStatus.CLEAN, 0.0),
                ),
                mlReport = MlReport(probability = 0.81),
                networkReport = NetworkReport(
                    tlsVersion = "TLS 1.3",
                    certValid = true,
                    headers = mapOf(
                        "Strict-Transport-Security" to "max-age=63072000; includeSubDomains",
                        "Content-Security-Policy" to "default-src 'self'",
                        "X-Frame-Options" to "DENY",
                        "X-Content-Type-Options" to "nosniff",
                        "Referrer-Policy" to "no-referrer",
                    ),
                    dnssecSignal = true,
                ),
                expectedCategory = RiskCategory.LOW,
                expectedScoreRange = 1.4..2.1,
            ),
        )

        scenarios.forEach { scenario ->
            val (score, breakdown) = RiskScorer.aggregateRisk(
                targetType = scenario.targetType,
                vendorVerdicts = scenario.vendorVerdicts,
                mlReport = scenario.mlReport,
                networkReport = scenario.networkReport,
                fileReport = null,
                vpnReport = null,
                instagramReport = null,
            )
            val topCategory = breakdown.categories
                .maxByOrNull { it.value }
                ?.key
                ?: RiskCategory.MINIMAL

            assertEquals("Top category mismatch for ${scenario.name}", scenario.expectedCategory, topCategory)
            assertTrue(
                "Risk score ${"%.2f".format(score)} is outside expected range ${scenario.expectedScoreRange} for ${scenario.name}",
                score in scenario.expectedScoreRange,
            )
        }
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
}
