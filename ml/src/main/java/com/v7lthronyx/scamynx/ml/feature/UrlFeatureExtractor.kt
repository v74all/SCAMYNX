package com.v7lthronyx.scamynx.ml.feature

import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.min
import com.v7lthronyx.scamynx.domain.model.FeatureWeight

@Singleton
class UrlFeatureExtractor @Inject constructor() {

    private val suspiciousKeywords = listOf(
        "login",
        "verify",
        "update",
        "password",
        "secure",
        "banking",
        "support",
    )

    fun extract(url: String, htmlSnapshot: String?): FeatureVector {
        val normalized = url.trim().lowercase()
        val uri = runCatching { URI(normalized) }.getOrNull()
        val host = uri?.host.orEmpty()
        val path = uri?.path.orEmpty()
        val query = uri?.query.orEmpty()
        val length = normalized.length.coerceAtLeast(1)
        val digitCount = normalized.count { it.isDigit() }
        val specialCount = normalized.count { !it.isLetterOrDigit() }
        val keywordHits = suspiciousKeywords.count { normalized.contains(it) }
        val entropy = shannonEntropy(normalized)
        val pathDepth = path.count { it == '/' }
        val queryLength = query.length
        val formCount = htmlSnapshot?.countSubstring("<form") ?: 0

        val values = floatArrayOf(
            (length / 200.0).coerceIn(0.0, 1.0).toFloat(),
            (digitCount.toDouble() / length).coerceIn(0.0, 1.0).toFloat(),
            (specialCount.toDouble() / length).coerceIn(0.0, 1.0).toFloat(),
            min(1.0, keywordHits / 3.0).toFloat(),
            (entropy / 4.0).coerceIn(0.0, 1.0).toFloat(),
            (pathDepth / 10.0).coerceIn(0.0, 1.0).toFloat(),
            (queryLength / 100.0).coerceIn(0.0, 1.0).toFloat(),
            min(1.0, formCount / 3.0).toFloat(),
        )

        val featureWeights = listOf(
            FeatureWeight("length", values[0].toDouble()),
            FeatureWeight("digit_ratio", values[1].toDouble()),
            FeatureWeight("special_ratio", values[2].toDouble()),
            FeatureWeight("keyword_hits", values[3].toDouble()),
            FeatureWeight("entropy", values[4].toDouble()),
            FeatureWeight("path_depth", values[5].toDouble()),
            FeatureWeight("query_length", values[6].toDouble()),
            FeatureWeight("form_count", values[7].toDouble()),
        ).sortedByDescending { it.weight }

        return FeatureVector(values = values, features = featureWeights)
    }

    private fun shannonEntropy(input: String): Double {
        val counts = input.groupingBy { it }.eachCount()
        val length = input.length.toDouble()
        return counts.values.fold(0.0) { acc, count ->
            val probability = count / length
            acc - probability * ln(probability)
        }
    }

    private fun String.countSubstring(target: String): Int {
        if (target.isEmpty()) return 0
        var index = 0
        var count = 0
        while (true) {
            val found = indexOf(target, startIndex = index, ignoreCase = true)
            if (found < 0) break
            count++
            index = found + target.length
        }
        return count
    }
}
