package com.v7lthronyx.scamynx.ml.analyzer

import com.v7lthronyx.scamynx.domain.model.AnomalyScores
import com.v7lthronyx.scamynx.ml.feature.FeatureVector
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

@Singleton
class AnomalyDetector @Inject constructor() {

    private val baselineMeans = floatArrayOf(
        0.25f,
        0.05f,
        0.08f,
        0.3f,
        0.2f,
        0.01f,
        0.1f,
        0.15f,
        0.02f,
        0.05f,
        0.01f,
        0.3f,
        0.05f,
        0.08f,
        0.02f,
        0.5f,
        0.25f,
        0.15f,
        0.2f,
        0.1f,
        0.1f,
        0.05f,
        0.3f,
        0.1f,
        0.02f,
    )

    private val baselineStdDevs = floatArrayOf(
        0.15f,
        0.05f,
        0.06f,
        0.25f,
        0.12f,
        0.1f,
        0.15f,
        0.1f,
        0.1f,
        0.15f,
        0.08f,
        0.2f,
        0.15f,
        0.1f,
        0.08f,
        0.2f,
        0.2f,
        0.12f,
        0.15f,
        0.12f,
        0.12f,
        0.1f,
        0.25f,
        0.2f,
        0.1f,
    )

    private val structuralFeatureIndices = listOf(0, 1, 2, 3, 4, 5, 6, 7)
    private val reputationFeatureIndices = listOf(8, 9, 10, 11, 12)
    private val behavioralFeatureIndices = listOf(13, 14, 15, 16, 17)
    private val contentFeatureIndices = listOf(18, 19, 20, 21, 22)

    fun detectAnomalies(featureVector: FeatureVector): AnomalyScores {
        val values = featureVector.values

        val zScores = values.mapIndexed { index, value ->
            val mean = baselineMeans.getOrNull(index) ?: 0.5f
            val stdDev = baselineStdDevs.getOrNull(index) ?: 0.2f
            if (stdDev > 0) (value - mean) / stdDev else 0f
        }

        val structuralAnomaly = calculateCategoryAnomaly(zScores, structuralFeatureIndices)
        val behavioralAnomaly = calculateCategoryAnomaly(zScores, behavioralFeatureIndices)
        val statisticalAnomaly = calculateOverallAnomaly(zScores)

        val overallAnomaly = (
            structuralAnomaly * 0.3 +
                behavioralAnomaly * 0.4 +
                statisticalAnomaly * 0.3
            ).coerceIn(0.0, 1.0)

        val isOutlier = zScores.any { abs(it) > 2.5f }

        return AnomalyScores(
            overallAnomaly = overallAnomaly,
            structuralAnomaly = structuralAnomaly,
            behavioralAnomaly = behavioralAnomaly,
            statisticalAnomaly = statisticalAnomaly,
            isOutlier = isOutlier,
        )
    }

    private fun calculateCategoryAnomaly(zScores: List<Float>, indices: List<Int>): Double {
        if (indices.isEmpty()) return 0.0

        val categoryZScores = indices.mapNotNull { zScores.getOrNull(it) }
        if (categoryZScores.isEmpty()) return 0.0

        val rms = sqrt(categoryZScores.map { it * it }.average())

        return (rms / 3.0).coerceIn(0.0, 1.0)
    }

    private fun calculateOverallAnomaly(zScores: List<Float>): Double {
        if (zScores.isEmpty()) return 0.0

        val significantDeviations = zScores.count { abs(it) > 1.5f }
        val extremeDeviations = zScores.count { abs(it) > 2.5f }

        val deviationScore = (significantDeviations * 0.1 + extremeDeviations * 0.3)
            .coerceIn(0.0, 1.0)

        val avgAbsZ = zScores.map { abs(it) }.average()
        val avgScore = (avgAbsZ / 2.0).coerceIn(0.0, 1.0)

        return (deviationScore + avgScore) / 2.0
    }

    fun getMostAnomalousFeatures(
        featureVector: FeatureVector,
        topN: Int = 5,
    ): List<AnomalousFeature> {
        val values = featureVector.values
        val featureNames = listOf(
            "url_length", "digit_ratio", "special_ratio", "subdomain_count",
            "domain_length", "has_ip_address", "tld_risk", "hyphen_count",
            "brand_impersonation", "url_shortener", "homograph_risk", "subdomain_entropy",
            "phishing_pattern", "keyword_hits", "urgency_score", "entropy",
            "path_depth", "query_params", "form_count", "password_fields",
            "hidden_fields", "iframe_count", "external_link_ratio", "no_https", "non_std_port",
        )

        return values.mapIndexed { index, value ->
            val mean = baselineMeans.getOrNull(index) ?: 0.5f
            val stdDev = baselineStdDevs.getOrNull(index) ?: 0.2f
            val zScore = if (stdDev > 0) (value - mean) / stdDev else 0f

            AnomalousFeature(
                name = featureNames.getOrNull(index) ?: "unknown",
                value = value,
                expectedValue = mean,
                deviation = abs(zScore),
            )
        }
            .sortedByDescending { it.deviation }
            .take(topN)
    }
}

data class AnomalousFeature(
    val name: String,
    val value: Float,
    val expectedValue: Float,
    val deviation: Float,
)
