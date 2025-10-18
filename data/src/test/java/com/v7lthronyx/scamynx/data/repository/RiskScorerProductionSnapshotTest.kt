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

class RiskScorerProductionSnapshotTest {

    private data class Snapshot(
        val name: String,
        val targetType: ScanTargetType,
        val vendorVerdicts: List<VendorVerdict>,
        val mlReport: MlReport?,
        val networkReport: NetworkReport?,
        val expectedCategory: RiskCategory,
        val expectedRiskRange: ClosedFloatingPointRange<Double>,
    )

    @Test
    fun `production snapshots align with analyst callouts`() {
        val snapshots = listOf(
            Snapshot(
                name = "Prod - Coordinated phishing kit",
                targetType = ScanTargetType.URL,
                vendorVerdicts = listOf(
                    verdict(Provider.VIRUS_TOTAL, VerdictStatus.MALICIOUS, 0.98),
                    verdict(Provider.THREAT_FOX, VerdictStatus.MALICIOUS, 0.82),
                    verdict(Provider.URL_HAUS, VerdictStatus.MALICIOUS, 0.85),
                    verdict(Provider.PHISH_STATS, VerdictStatus.SUSPICIOUS, 0.73),
                    verdict(Provider.GOOGLE_SAFE_BROWSING, VerdictStatus.SUSPICIOUS, 0.58),
                    verdict(Provider.LOCAL_HEURISTIC, VerdictStatus.MALICIOUS, 0.74),
                ),
                mlReport = MlReport(probability = 0.91),
                networkReport = NetworkReport(
                    tlsVersion = "TLS 1.0",
                    certValid = false,
                    headers = emptyMap(),
                    dnssecSignal = false,
                ),
                expectedCategory = RiskCategory.CRITICAL,
                expectedRiskRange = 4.3..5.0,
            ),
            Snapshot(
                name = "Prod - Heuristic flag dismissed by analysts",
                targetType = ScanTargetType.URL,
                vendorVerdicts = listOf(
                    verdict(Provider.VIRUS_TOTAL, VerdictStatus.CLEAN, 0.0),
                    verdict(Provider.GOOGLE_SAFE_BROWSING, VerdictStatus.CLEAN, 0.0),
                    verdict(Provider.URL_SCAN, VerdictStatus.CLEAN, 0.0),
                    verdict(Provider.LOCAL_HEURISTIC, VerdictStatus.SUSPICIOUS, 0.44),
                ),
                mlReport = MlReport(probability = 0.26),
                networkReport = NetworkReport(
                    tlsVersion = "TLS 1.3",
                    certValid = true,
                    headers = mapOf(
                        "Strict-Transport-Security" to "max-age=31536000",
                        "Content-Security-Policy" to "default-src 'self'",
                        "X-Frame-Options" to "DENY",
                        "X-Content-Type-Options" to "nosniff",
                        "Referrer-Policy" to "no-referrer",
                    ),
                    dnssecSignal = true,
                ),
                expectedCategory = RiskCategory.MINIMAL,
                expectedRiskRange = 0.2..0.6,
            ),
        )

        snapshots.forEach { snapshot ->
            val (risk, breakdown) = RiskScorer.aggregateRisk(
                targetType = snapshot.targetType,
                vendorVerdicts = snapshot.vendorVerdicts,
                mlReport = snapshot.mlReport,
                networkReport = snapshot.networkReport,
                fileReport = null,
                vpnReport = null,
                instagramReport = null,
            )
            val topCategory = breakdown.categories
                .maxByOrNull { it.value }
                ?.key
                ?: RiskCategory.MINIMAL

            assertEquals("Category mismatch for ${snapshot.name}", snapshot.expectedCategory, topCategory)
            assertTrue(
                "Risk score ${"%.2f".format(risk)} out of expected band ${snapshot.expectedRiskRange} for ${snapshot.name}",
                risk in snapshot.expectedRiskRange,
            )
        }
    }

    private fun verdict(
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
