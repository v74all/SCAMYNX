package com.v7lthronyx.scamynx.data.repository

import com.v7lthronyx.scamynx.domain.model.FileScanReport
import com.v7lthronyx.scamynx.domain.model.InstagramScanReport
import com.v7lthronyx.scamynx.domain.model.MlReport
import com.v7lthronyx.scamynx.domain.model.NetworkReport
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.RiskBreakdown
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.model.VendorVerdict
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import com.v7lthronyx.scamynx.domain.model.VpnConfigReport
import kotlin.math.max
import kotlin.math.pow
internal object RiskScorer {

    private val expectedSecurityHeaders = listOf(
        "Strict-Transport-Security",
        "Content-Security-Policy",
        "X-Frame-Options",
        "X-Content-Type-Options",
        "Referrer-Policy",
    )

    fun aggregateRisk(
        targetType: ScanTargetType,
        vendorVerdicts: List<VendorVerdict>,
        mlReport: MlReport?,
        networkReport: NetworkReport?,
        fileReport: FileScanReport?,
        vpnReport: VpnConfigReport?,
        instagramReport: InstagramScanReport?,
    ): Pair<Double, RiskBreakdown> {
        val vendorAggregate = aggregateVendorSignals(vendorVerdicts)
        val networkAdjust = networkAdjustment(networkReport)
        val normalizedRaw = when (targetType) {
            ScanTargetType.URL -> combineUrlRisk(
                vendorAggregate = vendorAggregate,
                mlScore = mlReport?.probability,
                networkAdjustment = networkAdjust,
                vendorVerdicts = vendorVerdicts,
            )
            ScanTargetType.FILE -> combineSpecializedRisk(
                vendorAggregate = vendorAggregate,
                specializedScore = fileReport?.riskScore,
            )
            ScanTargetType.VPN_CONFIG -> combineSpecializedRisk(
                vendorAggregate = vendorAggregate,
                specializedScore = vpnReport?.riskScore,
            )
            ScanTargetType.INSTAGRAM -> combineSpecializedRisk(
                vendorAggregate = vendorAggregate,
                specializedScore = instagramReport?.riskScore,
            )
        }
        val normalizedClamped = normalizedRaw.sanitizeProbability()
        val riskValue = (normalizedClamped * 5.0).sanitizeFinite().coerceIn(0.0, 5.0)
        val breakdown = buildRiskBreakdown(normalizedClamped)
        return riskValue to breakdown
    }

    private fun combineUrlRisk(
        vendorAggregate: VendorAggregate,
        mlScore: Double?,
        networkAdjustment: Double,
        vendorVerdicts: List<VendorVerdict>,
    ): Double {
        val vendorComponent = vendorAggregate.score
        val mlSignal = mlScore?.sanitizeProbability()
        val mlComponent = mlSignal ?: (vendorComponent * 0.8 + vendorAggregate.consensusBoost * 0.2)
        val base = (vendorComponent * 0.6) + (mlComponent * 0.3) +
            vendorAggregate.consensusBoost + networkAdjustment.sanitizeFinite()
        val constrained = base.sanitizeProbability()
        val confidenceAdjusted = adjustForConfidence(
            value = constrained,
            vendorScore = vendorComponent,
            confidence = vendorAggregate.confidence,
        )
        val normalized = confidenceAdjusted.coerceIn(0.0, 1.0)
        val networkFloor = if (networkAdjustment > 0.22) {
            (0.12 + (networkAdjustment - 0.22) * 0.8).coerceIn(0.12, 0.6)
        } else {
            0.0
        }
        val allClean = vendorVerdicts.isNotEmpty() && vendorVerdicts.all { it.status == VerdictStatus.CLEAN }
        val lowMlSignal = (mlSignal ?: 0.0) < 0.2
        val adjustedForClean = if (allClean && lowMlSignal) {
            (normalized * 0.45).coerceIn(0.0, 1.0)
        } else {
            normalized
        }
        return max(adjustedForClean, networkFloor).sanitizeProbability()
    }

    private fun combineSpecializedRisk(
        vendorAggregate: VendorAggregate,
        specializedScore: Double?,
    ): Double {
        val normalizedSpecialized = specializedScore?.sanitizeProbability()
        val base = when (normalizedSpecialized) {
            null -> vendorAggregate.score
            else -> (vendorAggregate.score * 0.55) + (normalizedSpecialized * 0.45)
        }
        val consensusAdjusted = base + (vendorAggregate.consensusBoost * 0.5)
        val confidenceAdjusted = adjustForConfidence(
            value = consensusAdjusted,
            vendorScore = vendorAggregate.score,
            confidence = vendorAggregate.confidence,
        )
        return confidenceAdjusted.sanitizeProbability()
    }

    private fun aggregateVendorSignals(vendorVerdicts: List<VendorVerdict>): VendorAggregate {
        if (vendorVerdicts.isEmpty()) {
            return VendorAggregate(score = 0.0, consensusBoost = 0.0, confidence = 0.0, sampleSize = 0)
        }
        var weightedScore = 0.0
        var weightSum = 0.0
        var maliciousCount = 0
        var suspiciousCount = 0
        var cleanCount = 0

        vendorVerdicts.forEach { verdict ->
            val providerWeight = providerReliability(verdict.provider)
            val verdictWeight = verdictWeight(verdict)
            weightedScore += verdictWeight * providerWeight
            weightSum += providerWeight
            when (verdict.status) {
                VerdictStatus.MALICIOUS -> maliciousCount++
                VerdictStatus.SUSPICIOUS -> suspiciousCount++
                VerdictStatus.CLEAN -> cleanCount++
                else -> Unit
            }
        }

        val normalizedScore = if (weightSum > 0) weightedScore / weightSum else 0.0
        var consensusBoost = 0.0
        if (maliciousCount >= 2) {
            consensusBoost += 0.18
        } else if (maliciousCount == 1 && suspiciousCount >= 1) {
            consensusBoost += 0.12
        }
        if (suspiciousCount >= 2) {
            consensusBoost += 0.05
        }
        if (cleanCount == vendorVerdicts.size && maliciousCount == 0 && suspiciousCount == 0) {
            consensusBoost -= 0.05
        }

        val confidentSignals = vendorVerdicts.count {
            it.status != VerdictStatus.UNKNOWN && it.status != VerdictStatus.ERROR
        }
        val confidence = confidentSignals.toDouble() / vendorVerdicts.size.toDouble()

        return VendorAggregate(
            score = normalizedScore.sanitizeProbability(),
            consensusBoost = consensusBoost.coerceIn(-0.05, 0.25),
            confidence = confidence.sanitizeProbability(),
            sampleSize = vendorVerdicts.size,
        )
    }

    private fun networkAdjustment(networkReport: NetworkReport?): Double {
        if (networkReport == null) return 0.0
        var adjustment = 0.0
        adjustment += when (networkReport.tlsVersion) {
            null -> 0.26
            "TLS 1.0", "TLS 1.1" -> 0.2
            "TLS 1.2" -> 0.06
            "TLS 1.3" -> -0.05
            else -> 0.02
        }
        adjustment += when (networkReport.certValid) {
            false -> 0.22
            true -> -0.03
            null -> 0.0
        }
        val missingHeaders = expectedSecurityHeaders.count { header ->
            networkReport.headers[header].isNullOrBlank()
        }
        adjustment += missingHeaders * 0.03
        if (missingHeaders == 0 && networkReport.headers.isNotEmpty()) {
            adjustment -= 0.05
        }
        networkReport.dnssecSignal?.let { secured ->
            adjustment += if (secured) -0.02 else 0.06
        }
        return adjustment.coerceIn(-0.12, 0.45)
    }

    private fun buildRiskBreakdown(normalized: Double): RiskBreakdown {
        val safeNormalized = normalized.sanitizeProbability()
        val categories = mapOf(
            RiskCategory.MINIMAL to triangularMembership(safeNormalized, start = 0.0, peak = 0.0, end = 0.2),
            RiskCategory.LOW to triangularMembership(safeNormalized, start = 0.15, peak = 0.3, end = 0.45),
            RiskCategory.MEDIUM to triangularMembership(safeNormalized, start = 0.35, peak = 0.55, end = 0.75),
            RiskCategory.HIGH to triangularMembership(safeNormalized, start = 0.65, peak = 0.82, end = 0.95),
            RiskCategory.CRITICAL to rightShoulderMembership(safeNormalized, start = 0.85),
        )
        return RiskBreakdown(categories)
    }

    private fun triangularMembership(x: Double, start: Double, peak: Double, end: Double): Double {
        if (!x.isFinite()) return 0.0
        if (x <= start) {
            return if (x == start) 1.0 else 0.0
        }
        if (x >= end) {
            return 0.0
        }
        return if (x <= peak) {
            val denominator = peak - start
            if (denominator == 0.0) {
                1.0
            } else {
                (x - start) / denominator
            }
        } else {
            val denominator = end - peak
            if (denominator == 0.0) {
                1.0
            } else {
                (end - x) / denominator
            }
        }.coerceIn(0.0, 1.0)
    }

    private fun rightShoulderMembership(x: Double, start: Double): Double {
        if (!x.isFinite()) return 0.0
        if (x <= start) return 0.0
        return ((x - start) / (1.0 - start)).coerceIn(0.0, 1.0)
    }

    private fun adjustForConfidence(
        value: Double,
        vendorScore: Double,
        confidence: Double,
    ): Double {
        val normalizedConfidence = confidence.sanitizeProbability()
        val safeValue = value.sanitizeProbability()
        val safeVendorScore = vendorScore.sanitizeProbability()
        val alignmentWeight = 0.15 + normalizedConfidence.pow(0.75) * 0.3
        val aligned = safeValue + (safeVendorScore - safeValue) * alignmentWeight
        val penalty = if (normalizedConfidence >= 0.4) {
            0.0
        } else {
            (0.4 - normalizedConfidence).pow(2) * 0.12
        }
        val adjusted = aligned - penalty
        return adjusted.sanitizeProbability()
    }

    private fun providerReliability(provider: Provider): Double = when (provider) {
        Provider.VIRUS_TOTAL -> 1.0
        Provider.GOOGLE_SAFE_BROWSING -> 0.9
        Provider.URL_SCAN -> 0.85
        Provider.URL_HAUS -> 0.9
        Provider.PHISH_STATS -> 0.8
        Provider.THREAT_FOX -> 0.85
        Provider.NETWORK -> 0.75
        Provider.ML -> 0.75
        Provider.FILE_STATIC -> 0.8
        Provider.VPN_CONFIG -> 0.7
        Provider.INSTAGRAM -> 0.7
        Provider.LOCAL_HEURISTIC -> 0.6
        Provider.CHAT_GPT -> 0.85
        Provider.MANUAL -> 0.5
    }

    private fun verdictWeight(verdict: VendorVerdict): Double {
        val normalizedScore = verdict.score.sanitizeProbability()
        val statusWeight = when (verdict.status) {
            VerdictStatus.MALICIOUS -> 1.0
            VerdictStatus.SUSPICIOUS -> 0.7
            VerdictStatus.UNKNOWN -> 0.4
            VerdictStatus.ERROR -> 0.6
            VerdictStatus.CLEAN -> 0.0
        }
        val blended = (statusWeight * 0.6) + (normalizedScore * 0.4)
        return when (verdict.provider) {
            Provider.LOCAL_HEURISTIC -> blended.coerceAtMost(0.75)
            else -> blended
        }
    }

    private data class VendorAggregate(
        val score: Double,
        val consensusBoost: Double,
        val confidence: Double,
        val sampleSize: Int,
    )

    private fun Double.sanitizeFinite(default: Double = 0.0): Double = if (isFinite()) this else default

    private fun Double.sanitizeProbability(): Double = sanitizeFinite().coerceIn(0.0, 1.0)
}
