package com.v7lthronyx.scamynx.data.heuristics

import com.v7lthronyx.scamynx.data.db.ThreatFeedDao
import com.v7lthronyx.scamynx.data.db.ThreatIndicatorEntity
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.di.ThreatIntelJson
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.VendorVerdict
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import java.net.URI
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp
import kotlin.math.max
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val IP_ADDRESS_REGEX = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
private val HEX_ADDRESS_REGEX = Regex("""^(0x[a-f0-9]+)$""", RegexOption.IGNORE_CASE)
private val URGENT_KEYWORDS = listOf(
    "alert",
    "billing",
    "bonus",
    "claim",
    "confirm",
    "invoice",
    "login",
    "lottery",
    "password",
    "refund",
    "secure",
    "support",
    "update",
    "verify",
)
private val SPOOF_KEYWORDS = listOf(
    "paypal",
    "microsoft",
    "apple",
    "google",
    "amazon",
    "instagram",
    "facebook",
    "coinbase",
    "binance",
    "bankofamerica",
    "chase",
    "netflix",
)
private val BRAND_ALLOW_LIST = mapOf(
    "paypal" to setOf("paypal.com", "paypalobjects.com"),
    "microsoft" to setOf("microsoft.com", "office.com", "live.com", "xbox.com"),
    "apple" to setOf("apple.com", "icloud.com", "me.com"),
    "google" to setOf("google.com", "googleapis.com", "gstatic.com"),
    "amazon" to setOf("amazon.com", "amazonaws.com"),
    "instagram" to setOf("instagram.com", "cdninstagram.com"),
    "facebook" to setOf("facebook.com", "fb.com", "meta.com"),
    "coinbase" to setOf("coinbase.com"),
    "binance" to setOf("binance.com"),
    "bankofamerica" to setOf("bankofamerica.com"),
    "chase" to setOf("chase.com", "jpmorganchase.com"),
    "netflix" to setOf("netflix.com"),
)
private val HIGH_RISK_TLDS = setOf(
    "zip",
    "ru",
    "cn",
    "tk",
    "top",
    "xyz",
    "club",
    "gq",
    "ml",
    "info",
    "online",
    "pw",
    "work",
    "men",
    "kim",
    "country",
    "stream",
    "party",
)

private const val SCORE_SENSITIVITY = 1.45
private const val MALICIOUS_THRESHOLD = 0.72
private const val SUSPICIOUS_THRESHOLD = 0.42
private const val UNKNOWN_THRESHOLD = 0.22

@Singleton
class LocalHeuristicsEvaluator @Inject constructor(
    private val threatFeedDao: ThreatFeedDao,
    @ThreatIntelJson private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val tagSerializer = ListSerializer(String.serializer())

    suspend fun evaluate(url: String): VendorVerdict = withContext(ioDispatcher) {
        val uri = runCatching { URI(url) }.getOrNull()
        val host = uri?.host?.lowercase(Locale.US).orEmpty()
        val scheme = uri?.scheme.orEmpty()
        val path = uri?.path.orEmpty()
        val query = uri?.query.orEmpty()
        val baseDomain = extractBaseDomain(host)

        val signals = LinkedHashMap<String, Double>()

        if (!scheme.equals("https", ignoreCase = true)) {
            signals["no_https"] = 0.22
        }
        val spoofedBrand = detectBrandSpoof(host, baseDomain)

        if (host.isBlank()) {
            signals["unparsable_host"] = 0.45
        } else {
            if (IP_ADDRESS_REGEX.matches(host) || HEX_ADDRESS_REGEX.matches(host)) {
                signals["ip_host"] = 0.65
            }
            if (host.contains("xn--")) {
                signals["punycode"] = 0.35
            }
            val subdomainDepth = host.count { it == '.' }
            if (subdomainDepth >= 3) {
                signals["deep_subdomain"] = 0.16
            }
            val tld = host.substringAfterLast('.', missingDelimiterValue = "")
            if (tld in HIGH_RISK_TLDS) {
                signals["risky_tld"] = 0.3
            }
            val digitRatio = host.count(Char::isDigit) / host.length.toDouble()
            if (digitRatio > 0.3) {
                signals["numeric_host"] = 0.22
            }
            val hyphenCount = host.count { it == '-' }
            if (hyphenCount >= 3) {
                signals["hyphenated_host"] = 0.12
            }
            if (host.length >= 28) {
                signals["long_host"] = 0.14
            }
            if (spoofedBrand != null) {
                signals["brand_spoof_$spoofedBrand"] = 0.34
            }
            val mixedCase = host.any { it.isUpperCase() }
            if (mixedCase) {
                signals["mixed_case_host"] = 0.1
            }
        }

        val urgentHits = URGENT_KEYWORDS.count { keyword ->
            url.contains(keyword, ignoreCase = true)
        }
        if (urgentHits > 0) {
            signals["urgent_language"] = max(0.16, urgentHits * 0.07)
        }
        if (url.count { it == '@' } > 0 || uri?.userInfo != null) {
            signals["at_symbol"] = 0.28
        }
        if (path.length > 60) {
            signals["long_path"] = 0.1
        }
        if (query.length > 80) {
            signals["long_query"] = 0.1
        }
        if (path.contains("//")) {
            signals["double_slash"] = 0.08
        }
        if (query.contains("session=", ignoreCase = true) || query.contains("token=", ignoreCase = true)) {
            signals["credential_query"] = 0.14
        }

        val threatMatches = findThreatFeedMatches(url, host)
        val highestThreatScore = threatMatches.maxOfOrNull { normalizeRiskScore(it.riskScore) } ?: 0.0
        if (threatMatches.isNotEmpty()) {
            signals["threat_feed_match"] = 0.5 + highestThreatScore * 0.5
        }

        val rawScore = signals.values.sum()
        val score = (1 - exp(-rawScore * SCORE_SENSITIVITY)).coerceIn(0.0, 1.0)
        val status = when {
            threatMatches.isNotEmpty() && highestThreatScore >= 0.6 -> VerdictStatus.MALICIOUS
            threatMatches.isNotEmpty() -> VerdictStatus.SUSPICIOUS
            spoofedBrand != null && score >= SUSPICIOUS_THRESHOLD -> VerdictStatus.MALICIOUS
            score >= MALICIOUS_THRESHOLD -> VerdictStatus.MALICIOUS
            score >= SUSPICIOUS_THRESHOLD -> VerdictStatus.SUSPICIOUS
            score >= UNKNOWN_THRESHOLD -> VerdictStatus.UNKNOWN
            else -> VerdictStatus.CLEAN
        }

        val detailSignals = signals.entries
            .sortedByDescending { it.value }
            .joinToString(separator = "; ") { "${it.key}:${"%.2f".format(Locale.US, it.value)}" }

        val threatSources = threatMatches
            .map { it.source }
            .distinct()
            .joinToString(separator = ", ")
            .ifBlank { null }

        val details = mutableMapOf<String, String?>()
        if (host.isNotBlank()) {
            details["host"] = host
        }
        if (detailSignals.isNotBlank()) {
            details["signals"] = detailSignals
        }
        if (threatSources != null) {
            details["threat_feed_sources"] = threatSources
            details["threat_feed_score"] = String.format(Locale.US, "%.2f", highestThreatScore)
        }
        threatMatches.size.takeIf { it > 0 }?.let { details["threat_feed_hits"] = it.toString() }
        val topTags = threatMatches
            .flatMap { parseTags(it) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(3)
            .joinToString(separator = ", ")
            .ifBlank { null }
        if (topTags != null) {
            details["threat_feed_tags"] = topTags
        }
        if (spoofedBrand != null) {
            details["spoof_brand"] = spoofedBrand
        }

        VendorVerdict(
            provider = Provider.LOCAL_HEURISTIC,
            status = status,
            score = score,
            details = details,
        )
    }

    private suspend fun findThreatFeedMatches(url: String, host: String): List<ThreatIndicatorEntity> {
        val directMatches = threatFeedDao.findByUrl(url)
        if (host.isBlank()) {
            return directMatches
        }
        val hostMatches = threatFeedDao.findByHost(host, limit = 25)
        if (directMatches.isEmpty()) {
            return hostMatches
        }
        val merged = LinkedHashMap<String, ThreatIndicatorEntity>()
        (directMatches + hostMatches).forEach { indicator ->
            merged[indicator.indicatorId] = indicator
        }
        return merged.values.toList()
    }

    private fun normalizeRiskScore(riskScore: Double): Double {
        return when {
            riskScore.isNaN() -> 0.0
            riskScore <= 1.0 -> riskScore.coerceIn(0.0, 1.0)
            riskScore <= 100.0 -> (riskScore / 100.0).coerceIn(0.0, 1.0)
            else -> 1.0
        }
    }

    private fun parseTags(entity: ThreatIndicatorEntity): List<String> {
        if (entity.tagsJson.isBlank()) return emptyList()
        return runCatching { json.decodeFromString(tagSerializer, entity.tagsJson) }.getOrDefault(emptyList())
    }

    private fun detectBrandSpoof(host: String, baseDomain: String): String? {
        if (host.isBlank()) return null
        for (brand in SPOOF_KEYWORDS) {
            if (!host.contains(brand)) continue
            val allowList = BRAND_ALLOW_LIST[brand].orEmpty()
            val matchesAllow = allowList.any { allowed ->
                baseDomain.endsWith(allowed) || host.endsWith(allowed)
            }
            if (matchesAllow) continue
            return brand
        }
        return null
    }

    private fun extractBaseDomain(host: String): String {
        if (host.isBlank()) return ""
        val parts = host.split('.').filter { it.isNotBlank() }
        if (parts.isEmpty()) return host
        if (parts.size == 1) return parts.first()
        val last = parts.last()
        val secondLast = parts[parts.size - 2]
        val isCountryCode = last.length == 2
        if (isCountryCode && parts.size >= 3) {
            val thirdLast = parts[parts.size - 3]
            return "$thirdLast.$secondLast.$last"
        }
        return "$secondLast.$last"
    }
}
