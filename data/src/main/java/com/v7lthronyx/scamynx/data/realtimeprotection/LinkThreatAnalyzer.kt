package com.v7lthronyx.scamynx.data.realtimeprotection

import com.v7lthronyx.scamynx.domain.model.ConfidenceLevel
import com.v7lthronyx.scamynx.domain.model.DomainReputation
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.PopularityLevel
import com.v7lthronyx.scamynx.domain.model.ThreatType
import java.net.URI
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of link threat analysis.
 */
data class LinkAnalysisResult(
    val isSafe: Boolean,
    val threatType: ThreatType?,
    val severity: IssueSeverity?,
    val confidence: ConfidenceLevel,
    val categories: List<String>,
    val reputation: DomainReputation,
)

/**
 * Analyzes URLs for potential security threats.
 * Uses heuristics and pattern matching to detect phishing, malware, and other threats.
 */
@Singleton
class LinkThreatAnalyzer @Inject constructor() {

    companion object {
        // Known URL shorteners
        private val URL_SHORTENERS = setOf(
            "bit.ly", "goo.gl", "t.co", "tinyurl.com", "ow.ly",
            "is.gd", "buff.ly", "adf.ly", "j.mp", "tr.im",
            "cli.gs", "short.io", "cutt.ly", "rebrand.ly", "v.gd"
        )

        // Suspicious TLDs
        private val SUSPICIOUS_TLDS = setOf(
            ".xyz", ".top", ".work", ".click", ".link", ".surf",
            ".win", ".bid", ".loan", ".date", ".faith", ".review",
            ".party", ".racing", ".stream", ".download", ".gdn"
        )

        // Trusted domains (simplified list)
        private val TRUSTED_DOMAINS = setOf(
            "google.com", "facebook.com", "amazon.com", "apple.com",
            "microsoft.com", "github.com", "stackoverflow.com", "wikipedia.org",
            "twitter.com", "linkedin.com", "instagram.com", "youtube.com",
            "reddit.com", "netflix.com", "spotify.com", "paypal.com",
            "dropbox.com", "cloudflare.com", "mozilla.org", "whatsapp.com"
        )

        // IP address pattern
        private val IP_ADDRESS_PATTERN = Pattern.compile(
            "^https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"
        )

        // Punycode detection
        private val PUNYCODE_PATTERN = Pattern.compile("xn--")

        // Suspicious keywords
        private val SUSPICIOUS_KEYWORDS = setOf(
            "login", "signin", "sign-in", "verify", "verification",
            "update", "secure", "account", "confirm", "banking",
            "wallet", "password", "paypal", "amazon", "apple",
            "microsoft", "google", "facebook", "instagram", "netflix"
        )

        // Phishing patterns
        private val PHISHING_PATTERNS = listOf(
            Pattern.compile(".*-login\\..*"),
            Pattern.compile(".*-secure\\..*"),
            Pattern.compile(".*-verify\\..*"),
            Pattern.compile(".*\\.com-.*"),
            Pattern.compile(".*\\d{5,}.*\\.(com|net|org)$"),
            Pattern.compile(".*login.*\\.(xyz|top|work|click)$"),
        )
    }

    /**
     * Analyzes a URL for potential threats.
     */
    fun analyze(url: String): LinkAnalysisResult {
        val domain = extractDomain(url)
        var riskScore = 0.0
        val categories = mutableListOf<String>()
        var threatType: ThreatType? = null
        var severity: IssueSeverity? = null

        // Check if trusted domain
        if (TRUSTED_DOMAINS.any { domain.endsWith(it) }) {
            return LinkAnalysisResult(
                isSafe = true,
                threatType = null,
                severity = null,
                confidence = ConfidenceLevel.HIGH,
                categories = listOf("trusted"),
                reputation = DomainReputation(
                    score = 95,
                    hasValidSsl = url.startsWith("https"),
                    popularity = PopularityLevel.VERY_POPULAR,
                ),
            )
        }

        // Check for URL shorteners
        if (URL_SHORTENERS.any { domain.contains(it) }) {
            riskScore += 20
            categories.add("url_shortener")
        }

        // Check for suspicious TLDs
        if (SUSPICIOUS_TLDS.any { domain.endsWith(it) }) {
            riskScore += 25
            categories.add("suspicious_tld")
        }

        // Check for IP address URLs
        if (IP_ADDRESS_PATTERN.matcher(url).find()) {
            riskScore += 40
            categories.add("ip_address")
            threatType = ThreatType.PHISHING
            severity = IssueSeverity.HIGH
        }

        // Check for punycode (potential homograph attack)
        if (PUNYCODE_PATTERN.matcher(domain).find()) {
            riskScore += 50
            categories.add("homograph")
            threatType = ThreatType.PHISHING
            severity = IssueSeverity.HIGH
        }

        // Check for suspicious keywords
        val suspiciousKeywordsFound = SUSPICIOUS_KEYWORDS.filter { keyword ->
            domain.contains(keyword) && !isLegitimateService(domain, keyword)
        }
        if (suspiciousKeywordsFound.isNotEmpty()) {
            riskScore += suspiciousKeywordsFound.size * 15
            categories.add("suspicious_keywords")
        }

        // Check phishing patterns
        for (pattern in PHISHING_PATTERNS) {
            if (pattern.matcher(domain).matches()) {
                riskScore += 35
                categories.add("phishing_pattern")
                threatType = ThreatType.PHISHING
                severity = IssueSeverity.HIGH
                break
            }
        }

        // Check for HTTP (not HTTPS)
        if (url.startsWith("http://")) {
            riskScore += 10
            categories.add("no_https")
        }

        // Check for very long URLs
        if (url.length > 200) {
            riskScore += 10
            categories.add("long_url")
        }

        // Check for excessive subdomains
        val subdomainCount = domain.count { it == '.' }
        if (subdomainCount > 3) {
            riskScore += 15
            categories.add("many_subdomains")
        }

        // Determine final result
        val isSafe = riskScore < 30
        
        if (threatType == null && !isSafe) {
            threatType = when {
                riskScore >= 60 -> ThreatType.PHISHING
                riskScore >= 40 -> ThreatType.SCAM
                else -> ThreatType.SOCIAL_ENGINEERING
            }
        }

        if (severity == null && !isSafe) {
            severity = when {
                riskScore >= 60 -> IssueSeverity.HIGH
                riskScore >= 40 -> IssueSeverity.MEDIUM
                else -> IssueSeverity.LOW
            }
        }

        val confidence = when {
            riskScore >= 60 || riskScore == 0.0 -> ConfidenceLevel.HIGH
            riskScore >= 30 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

        val reputationScore = (100 - riskScore.coerceIn(0.0, 100.0)).toInt()

        return LinkAnalysisResult(
            isSafe = isSafe,
            threatType = threatType,
            severity = severity,
            confidence = confidence,
            categories = categories,
            reputation = DomainReputation(
                score = reputationScore,
                hasValidSsl = url.startsWith("https"),
                isNewlyRegistered = categories.contains("suspicious_tld"),
                popularity = when {
                    reputationScore >= 80 -> PopularityLevel.POPULAR
                    reputationScore >= 50 -> PopularityLevel.MODERATE
                    reputationScore >= 30 -> PopularityLevel.LOW
                    else -> PopularityLevel.VERY_LOW
                },
            ),
        )
    }

    private fun extractDomain(url: String): String {
        return try {
            URI(url).host?.lowercase()?.removePrefix("www.") ?: url
        } catch (e: Exception) {
            url.removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
                .substringBefore("?")
                .lowercase()
                .removePrefix("www.")
        }
    }

    private fun isLegitimateService(domain: String, keyword: String): Boolean {
        val legitimateDomains = mapOf(
            "google" to listOf("google.com", "google.co.", "googleapis.com"),
            "facebook" to listOf("facebook.com", "fb.com", "facebook.net"),
            "amazon" to listOf("amazon.com", "amazon.co.", "amazonaws.com"),
            "apple" to listOf("apple.com", "icloud.com"),
            "microsoft" to listOf("microsoft.com", "live.com", "outlook.com", "azure.com"),
            "paypal" to listOf("paypal.com", "paypal.me"),
            "instagram" to listOf("instagram.com"),
            "netflix" to listOf("netflix.com"),
        )

        val patterns = legitimateDomains[keyword] ?: return false
        return patterns.any { domain.endsWith(it) }
    }
}
