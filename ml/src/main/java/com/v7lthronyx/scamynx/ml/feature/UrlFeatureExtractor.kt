package com.v7lthronyx.scamynx.ml.feature

import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.min
import com.v7lthronyx.scamynx.domain.model.FeatureWeight

/**
 * Extracts 25 features from a URL and optional HTML content for phishing/scam detection.
 * 
 * Features extracted (in order):
 * 1. length - Normalized URL length
 * 2. digit_ratio - Ratio of digits in URL
 * 3. special_ratio - Ratio of special characters
 * 4. keyword_hits - Suspicious keyword count
 * 5. entropy - Shannon entropy of URL
 * 6. path_depth - Depth of URL path
 * 7. query_length - Query string length
 * 8. form_count - Number of forms in HTML
 * 9. subdomain_count - Number of subdomains
 * 10. domain_length - Length of domain
 * 11. has_ip_address - Whether URL contains IP address
 * 12. tld_risk - Risk score of TLD
 * 13. hyphen_count - Number of hyphens in domain
 * 14. brand_impersonation - Brand name detection score
 * 15. url_shortener - URL shortener service detection
 * 16. homograph_risk - Homograph attack risk
 * 17. subdomain_entropy - Entropy of subdomains
 * 18. phishing_pattern - Phishing URL pattern score
 * 19. urgency_score - Urgency keywords score
 * 20. password_fields - Password field count in HTML
 * 21. hidden_fields - Hidden field count in HTML
 * 22. iframe_count - Iframe count in HTML
 * 23. external_link_ratio - Ratio of external links
 * 24. no_https - Whether URL lacks HTTPS
 * 25. non_std_port - Non-standard port detection
 */
@Singleton
class UrlFeatureExtractor @Inject constructor() {

    companion object {
        private const val NUM_FEATURES = 25
        
        private val SUSPICIOUS_KEYWORDS = listOf(
            "login", "verify", "update", "password", "secure", "banking",
            "support", "confirm", "account", "suspend", "alert", "urgent"
        )
        
        private val URGENCY_KEYWORDS = listOf(
            "urgent", "immediately", "expire", "suspend", "verify", "confirm",
            "limited", "act-now", "asap", "warning", "alert", "important"
        )
        
        private val BRAND_NAMES = listOf(
            "paypal", "apple", "google", "microsoft", "amazon", "facebook",
            "netflix", "instagram", "whatsapp", "twitter", "linkedin", "bank",
            "chase", "wellsfargo", "citibank", "dropbox", "adobe", "spotify"
        )
        
        private val URL_SHORTENERS = listOf(
            "bit.ly", "goo.gl", "t.co", "tinyurl.com", "ow.ly", "is.gd",
            "buff.ly", "j.mp", "rb.gy", "cutt.ly", "short.link", "tiny.cc"
        )
        
        private val RISKY_TLDS = mapOf(
            "tk" to 0.9f, "ml" to 0.85f, "ga" to 0.85f, "cf" to 0.85f, "gq" to 0.85f,
            "xyz" to 0.6f, "top" to 0.6f, "pw" to 0.7f, "cc" to 0.5f, "click" to 0.6f,
            "link" to 0.5f, "work" to 0.5f, "live" to 0.4f, "online" to 0.5f,
            "site" to 0.5f, "info" to 0.35f, "biz" to 0.35f, "ru" to 0.4f, "cn" to 0.4f
        )
        
        // Common confusable characters for homograph detection
        private val HOMOGRAPH_CHARS = mapOf(
            'а' to 'a', 'е' to 'e', 'о' to 'o', 'р' to 'p', 'с' to 'c', 
            'х' to 'x', 'у' to 'y', 'і' to 'i', 'ј' to 'j', 'ѕ' to 's',
            '0' to 'o', '1' to 'l', '!' to 'i'
        )
        
        private val PHISHING_PATTERNS = listOf(
            Regex("""login.*\d+""", RegexOption.IGNORE_CASE),
            Regex("""secure.*bank""", RegexOption.IGNORE_CASE),
            Regex("""verify.*account""", RegexOption.IGNORE_CASE),
            Regex("""update.*password""", RegexOption.IGNORE_CASE),
            Regex("""confirm.*identity""", RegexOption.IGNORE_CASE),
            Regex("""\d{2,}[a-z]""", RegexOption.IGNORE_CASE),
            Regex("""[a-z]-[a-z]-[a-z]""", RegexOption.IGNORE_CASE)
        )
        
        private val IP_PATTERN = Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""")
    }

    fun extract(url: String, htmlSnapshot: String?): FeatureVector {
        val normalized = url.trim().lowercase()
        val uri = runCatching { URI(normalized) }.getOrNull()
        val host = uri?.host.orEmpty()
        val path = uri?.path.orEmpty()
        val query = uri?.query.orEmpty()
        val scheme = uri?.scheme.orEmpty()
        val port = uri?.port ?: -1
        
        // Parse domain components
        val hostParts = host.split(".")
        val tld = hostParts.lastOrNull().orEmpty()
        val domain = if (hostParts.size >= 2) {
            hostParts.takeLast(2).joinToString(".")
        } else host
        val subdomains = if (hostParts.size > 2) {
            hostParts.dropLast(2)
        } else emptyList()
        
        // Feature 1: URL length (normalized 0-1)
        val length = normalized.length.coerceAtLeast(1)
        val lengthFeature = (length / 200.0).coerceIn(0.0, 1.0).toFloat()
        
        // Feature 2: Digit ratio
        val digitCount = normalized.count { it.isDigit() }
        val digitRatio = (digitCount.toDouble() / length).coerceIn(0.0, 1.0).toFloat()
        
        // Feature 3: Special character ratio
        val specialCount = normalized.count { !it.isLetterOrDigit() }
        val specialRatio = (specialCount.toDouble() / length).coerceIn(0.0, 1.0).toFloat()
        
        // Feature 4: Suspicious keyword hits
        val keywordHits = SUSPICIOUS_KEYWORDS.count { normalized.contains(it) }
        val keywordFeature = min(1.0, keywordHits / 4.0).toFloat()
        
        // Feature 5: Shannon entropy
        val entropy = shannonEntropy(normalized)
        val entropyFeature = (entropy / 4.5).coerceIn(0.0, 1.0).toFloat()
        
        // Feature 6: Path depth
        val pathDepth = path.count { it == '/' }
        val pathDepthFeature = (pathDepth / 10.0).coerceIn(0.0, 1.0).toFloat()
        
        // Feature 7: Query length
        val queryLengthFeature = (query.length / 100.0).coerceIn(0.0, 1.0).toFloat()
        
        // Feature 8: Form count in HTML
        val formCount = htmlSnapshot?.countSubstring("<form") ?: 0
        val formCountFeature = min(1.0, formCount / 3.0).toFloat()
        
        // Feature 9: Subdomain count
        val subdomainCount = subdomains.size
        val subdomainCountFeature = min(1.0, subdomainCount / 4.0).toFloat()
        
        // Feature 10: Domain length
        val domainLengthFeature = (domain.length / 30.0).coerceIn(0.0, 1.0).toFloat()
        
        // Feature 11: Has IP address
        val hasIpAddress = IP_PATTERN.containsMatchIn(host)
        val hasIpFeature = if (hasIpAddress) 1.0f else 0.0f
        
        // Feature 12: TLD risk score
        val tldRiskFeature = RISKY_TLDS[tld] ?: 0.1f
        
        // Feature 13: Hyphen count in domain
        val hyphenCount = host.count { it == '-' }
        val hyphenFeature = min(1.0, hyphenCount / 5.0).toFloat()
        
        // Feature 14: Brand impersonation
        val brandImpersonation = detectBrandImpersonation(host, subdomains, path)
        val brandFeature = brandImpersonation.toFloat()
        
        // Feature 15: URL shortener detection
        val isShortener = URL_SHORTENERS.any { host.contains(it) || domain == it }
        val shortenerFeature = if (isShortener) 1.0f else 0.0f
        
        // Feature 16: Homograph risk
        val homographRisk = detectHomographRisk(host)
        val homographFeature = homographRisk.toFloat()
        
        // Feature 17: Subdomain entropy
        val subdomainText = subdomains.joinToString(".")
        val subdomainEntropy = if (subdomainText.isNotEmpty()) {
            shannonEntropy(subdomainText)
        } else 0.0
        val subdomainEntropyFeature = (subdomainEntropy / 4.0).coerceIn(0.0, 1.0).toFloat()
        
        // Feature 18: Phishing pattern score
        val phishingPatternScore = PHISHING_PATTERNS.count { it.containsMatchIn(normalized) }
        val phishingPatternFeature = min(1.0, phishingPatternScore / 3.0).toFloat()
        
        // Feature 19: Urgency score
        val urgencyHits = URGENCY_KEYWORDS.count { normalized.contains(it) }
        val urgencyFeature = min(1.0, urgencyHits / 3.0).toFloat()
        
        // Feature 20: Password fields in HTML
        val passwordFields = htmlSnapshot?.countSubstring("type=\"password\"")
            ?.plus(htmlSnapshot.countSubstring("type='password'")) ?: 0
        val passwordFieldsFeature = min(1.0, passwordFields / 2.0).toFloat()
        
        // Feature 21: Hidden fields in HTML
        val hiddenFields = htmlSnapshot?.countSubstring("type=\"hidden\"")
            ?.plus(htmlSnapshot.countSubstring("type='hidden'")) ?: 0
        val hiddenFieldsFeature = min(1.0, hiddenFields / 10.0).toFloat()
        
        // Feature 22: Iframe count
        val iframeCount = htmlSnapshot?.countSubstring("<iframe") ?: 0
        val iframeFeature = min(1.0, iframeCount / 3.0).toFloat()
        
        // Feature 23: External link ratio
        val externalLinkRatio = computeExternalLinkRatio(htmlSnapshot, host)
        val externalLinkFeature = externalLinkRatio.toFloat()
        
        // Feature 24: No HTTPS
        val noHttps = scheme != "https"
        val noHttpsFeature = if (noHttps) 1.0f else 0.0f
        
        // Feature 25: Non-standard port
        val nonStdPort = port > 0 && port !in listOf(80, 443, -1)
        val nonStdPortFeature = if (nonStdPort) 1.0f else 0.0f

        val values = floatArrayOf(
            lengthFeature,           // 0: length
            digitRatio,              // 1: digit_ratio
            specialRatio,            // 2: special_ratio
            keywordFeature,          // 3: keyword_hits
            entropyFeature,          // 4: entropy
            pathDepthFeature,        // 5: path_depth
            queryLengthFeature,      // 6: query_length
            formCountFeature,        // 7: form_count
            subdomainCountFeature,   // 8: subdomain_count
            domainLengthFeature,     // 9: domain_length
            hasIpFeature,            // 10: has_ip_address
            tldRiskFeature,          // 11: tld_risk
            hyphenFeature,           // 12: hyphen_count
            brandFeature,            // 13: brand_impersonation
            shortenerFeature,        // 14: url_shortener
            homographFeature,        // 15: homograph_risk
            subdomainEntropyFeature, // 16: subdomain_entropy
            phishingPatternFeature,  // 17: phishing_pattern
            urgencyFeature,          // 18: urgency_score
            passwordFieldsFeature,   // 19: password_fields
            hiddenFieldsFeature,     // 20: hidden_fields
            iframeFeature,           // 21: iframe_count
            externalLinkFeature,     // 22: external_link_ratio
            noHttpsFeature,          // 23: no_https
            nonStdPortFeature,       // 24: non_std_port
        )

        val featureNames = listOf(
            "length", "digit_ratio", "special_ratio", "keyword_hits", "entropy",
            "path_depth", "query_length", "form_count", "subdomain_count", "domain_length",
            "has_ip_address", "tld_risk", "hyphen_count", "brand_impersonation", "url_shortener",
            "homograph_risk", "subdomain_entropy", "phishing_pattern", "urgency_score",
            "password_fields", "hidden_fields", "iframe_count", "external_link_ratio",
            "no_https", "non_std_port"
        )

        val featureWeights = featureNames.mapIndexed { index, name ->
            FeatureWeight(name, values[index].toDouble())
        }.sortedByDescending { it.weight }

        return FeatureVector(values = values, features = featureWeights)
    }

    /**
     * Detects potential brand impersonation in the URL.
     * Returns a score from 0 to 1 indicating likelihood of impersonation.
     */
    private fun detectBrandImpersonation(host: String, subdomains: List<String>, path: String): Double {
        var score = 0.0
        val subdomainText = subdomains.joinToString(".")
        
        for (brand in BRAND_NAMES) {
            // Check if brand appears in subdomain but not in main domain
            if (subdomainText.contains(brand) && !host.endsWith(".$brand.com")) {
                score += 0.4
            }
            // Check if brand is in path (phishing tactic)
            if (path.contains(brand)) {
                score += 0.3
            }
            // Check for typosquatting patterns
            if (host.contains(brand) && !host.endsWith("$brand.com") && !host.endsWith("$brand.net")) {
                score += 0.5
            }
        }
        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Detects homograph attack risk using confusable characters.
     */
    private fun detectHomographRisk(host: String): Double {
        var confusableCount = 0
        for (char in host) {
            if (HOMOGRAPH_CHARS.containsKey(char)) {
                confusableCount++
            }
        }
        // Also check for mixed script
        val hasLatin = host.any { it in 'a'..'z' || it in 'A'..'Z' }
        val hasCyrillic = host.any { it in '\u0400'..'\u04FF' }
        val mixedScript = hasLatin && hasCyrillic
        
        var risk = (confusableCount / 3.0).coerceIn(0.0, 0.8)
        if (mixedScript) risk = (risk + 0.4).coerceIn(0.0, 1.0)
        
        return risk
    }

    /**
     * Computes the ratio of external links to total links in HTML.
     */
    private fun computeExternalLinkRatio(html: String?, host: String): Double {
        if (html.isNullOrBlank()) return 0.0
        
        val hrefPattern = Regex("""href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val hrefs = hrefPattern.findAll(html).map { it.groupValues[1] }.toList()
        
        if (hrefs.isEmpty()) return 0.0
        
        var externalCount = 0
        for (href in hrefs) {
            val hrefUri = runCatching { URI(href) }.getOrNull()
            val hrefHost = hrefUri?.host.orEmpty()
            if (hrefHost.isNotEmpty() && !hrefHost.contains(host) && !host.contains(hrefHost)) {
                externalCount++
            }
        }
        
        return (externalCount.toDouble() / hrefs.size).coerceIn(0.0, 1.0)
    }

    private fun shannonEntropy(input: String): Double {
        if (input.isEmpty()) return 0.0
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
