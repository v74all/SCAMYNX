package com.v7lthronyx.scamynx.data.antiphishing

import com.v7lthronyx.scamynx.domain.model.AntiPhishingAnalysis
import com.v7lthronyx.scamynx.domain.model.LinkDisposition
import com.v7lthronyx.scamynx.domain.service.AntiPhishingAnalyzer
import kotlinx.datetime.Clock
import java.net.IDN
import java.net.URI
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class HeuristicAntiPhishingAnalyzer @Inject constructor() : AntiPhishingAnalyzer {

    private val suspiciousTlds = setOf(
        "zip",
        "mov",
        "xyz",
        "gq",
        "tk",
        "cn",
        "ru",
        "work",
        "rest",
        "support",
        "info",
    )
    private val shortenerHosts = setOf(
        "bit.ly",
        "tinyurl.com",
        "t.co",
        "goo.gl",
        "cutt.ly",
        "rebrand.ly",
        "rb.gy",
        "s.id",
    )
    private val brandKeywords = listOf(
        "login",
        "signin",
        "wallet",
        "bonus",
        "airdrop",
        "verify",
        "secure",
        "account",
        "support",
        "helpdesk",
    )
    private val credentialParams = listOf(
        "pass",
        "password",
        "otp",
        "pin",
        "session",
        "token",
    )
    private val reputationIndicators = listOf(
        "secure-meta-help",
        "gift-whatsapp",
        "telegram-premium-bot",
        "instagram-support-center",
    )
    private val ipRegex = Regex("^\\d{1,3}(?:\\.\\d{1,3}){3}$")
    private val unicodeRegex = Regex("[^\\x00-\\x7F]")

    override suspend fun analyze(input: String): AntiPhishingAnalysis {
        val normalized = normalize(input)
        val inspectedAt = Clock.System.now()
        val uri = runCatching { URI(normalized) }.getOrNull()
        if (uri?.host.isNullOrBlank()) {
            return AntiPhishingAnalysis(
                url = input,
                normalizedUrl = normalized,
                score = 1.0,
                disposition = LinkDisposition.MALICIOUS,
                triggers = listOf("invalid_url"),
                inspectedAt = inspectedAt,
            )
        }
        val host = uri!!.host.lowercase(Locale.US)
        var score = 0.0
        val triggers = mutableListOf<String>()
        fun bump(amount: Double, reason: String) {
            score += amount
            triggers += reason
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            bump(0.2, "non_https")
        }
        val asciiHost = runCatching { IDN.toASCII(host) }.getOrDefault(host)
        if (ipRegex.matches(host)) {
            bump(0.35, "ip_host")
        }
        val labelCount = asciiHost.count { it == '.' } + 1
        if (labelCount >= 4) {
            bump(0.1, "deep_subdomain")
        }
        if (unicodeRegex.containsMatchIn(host) || asciiHost.contains("xn--")) {
            bump(0.2, "unicode_homograph")
        }
        val tld = asciiHost.substringAfterLast('.', "")
        if (tld in suspiciousTlds) {
            bump(0.1, "suspicious_tld:$tld")
        }
        if (host in shortenerHosts) {
            bump(0.25, "link_shortener")
        }
        val keywordHits = brandKeywords.count { keyword ->
            host.contains(keyword) || uri.rawPath?.contains(keyword, ignoreCase = true) == true
        }
        if (keywordHits > 0) {
            bump(min(0.25, keywordHits * 0.05), "brand_keywords:$keywordHits")
        }
        val query = uri.rawQuery?.lowercase(Locale.US).orEmpty()
        val credentialHits = credentialParams.count { query.contains(it) }
        if (credentialHits > 0) {
            bump(min(0.15, credentialHits * 0.03), "credential_params:$credentialHits")
        }
        val reputationHits = reputationIndicators.filter { normalized.contains(it, ignoreCase = true) }
        if (reputationHits.isNotEmpty()) {
            bump(0.45, "reputation_hit")
        }
        if (uri.rawPath?.count { it == '@' }?.let { it > 0 } == true) {
            bump(0.15, "path_uses_at_symbol")
        }
        val finalScore = min(score, 1.0)
        val disposition = when {
            finalScore >= 0.7 -> LinkDisposition.MALICIOUS
            finalScore >= 0.45 -> LinkDisposition.SUSPICIOUS
            else -> LinkDisposition.SAFE
        }
        return AntiPhishingAnalysis(
            url = input,
            normalizedUrl = normalized,
            score = finalScore,
            disposition = disposition,
            triggers = triggers.distinct(),
            reputationMatches = reputationHits,
            inspectedAt = inspectedAt,
        )
    }

    private fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        return "https://$trimmed"
    }
}
