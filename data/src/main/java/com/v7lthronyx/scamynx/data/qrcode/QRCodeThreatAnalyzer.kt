package com.v7lthronyx.scamynx.data.qrcode

import com.v7lthronyx.scamynx.domain.model.ConfidenceLevel
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.QRCodeContent
import com.v7lthronyx.scamynx.domain.model.QRCodeType
import com.v7lthronyx.scamynx.domain.model.QRThreat
import com.v7lthronyx.scamynx.domain.model.QRThreatAssessment
import com.v7lthronyx.scamynx.domain.model.QRThreatType
import com.v7lthronyx.scamynx.domain.model.QRWarning
import com.v7lthronyx.scamynx.domain.model.QRWarningType
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.WifiSecurityType
import java.net.URI
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyzes QR code content for potential security threats and risks.
 * Provides threat assessment including risk level, threats, warnings, and recommendations.
 */
@Singleton
class QRCodeThreatAnalyzer @Inject constructor() {

    companion object {
        // Known URL shorteners that can hide malicious links
        private val URL_SHORTENERS = setOf(
            "bit.ly", "goo.gl", "t.co", "tinyurl.com", "ow.ly",
            "is.gd", "buff.ly", "adf.ly", "j.mp", "tr.im",
            "cli.gs", "short.io", "cutt.ly", "rebrand.ly", "v.gd"
        )

        // Suspicious TLDs often used in scams
        private val SUSPICIOUS_TLDS = setOf(
            ".xyz", ".top", ".work", ".click", ".link", ".surf",
            ".win", ".bid", ".loan", ".date", ".faith", ".review",
            ".party", ".racing", ".stream", ".download", ".gdn"
        )

        // Premium SMS prefixes (can result in charges)
        private val PREMIUM_SMS_PREFIXES = setOf(
            "900", "901", "902", "905", "906", "907", "908", "909"
        )

        // IP address pattern
        private val IP_ADDRESS_PATTERN = Pattern.compile(
            "^https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"
        )

        // Homograph/punycode detection
        private val PUNYCODE_PATTERN = Pattern.compile("xn--")

        // Suspicious keywords in URLs
        private val SUSPICIOUS_KEYWORDS = setOf(
            "login", "signin", "verify", "update", "secure", "account",
            "confirm", "banking", "wallet", "password", "paypal", "amazon",
            "apple", "microsoft", "google", "facebook", "instagram"
        )

        // Known phishing domain patterns
        private val PHISHING_PATTERNS = listOf(
            Pattern.compile(".*-login\\..*"),
            Pattern.compile(".*-secure\\..*"),
            Pattern.compile(".*-verify\\..*"),
            Pattern.compile(".*\\.com-.*"),
            Pattern.compile(".*\\d{5,}.*\\.(com|net|org)$")
        )
    }

    /**
     * Analyzes QR code content for threats (suspend version for async operations).
     */
    suspend fun analyze(
        rawContent: String,
        contentType: QRCodeType,
        parsedContent: QRCodeContent,
    ): QRThreatAssessment {
        return analyzeSync(rawContent, contentType, parsedContent)
    }

    /**
     * Analyzes QR code content for threats (synchronous version for camera analysis).
     */
    fun analyzeSync(
        rawContent: String,
        contentType: QRCodeType,
        parsedContent: QRCodeContent,
    ): QRThreatAssessment {
        val threats = mutableListOf<QRThreat>()
        val warnings = mutableListOf<QRWarning>()
        val recommendations = mutableListOf<String>()

        when (parsedContent) {
            is QRCodeContent.Url -> {
                analyzeUrl(parsedContent, threats, warnings, recommendations)
            }

            is QRCodeContent.Text -> {
                if (parsedContent.containsUrls) {
                    parsedContent.extractedUrls.forEach { url ->
                        analyzeUrlString(url, threats, warnings, recommendations)
                    }
                }
            }

            is QRCodeContent.Wifi -> {
                analyzeWifi(parsedContent, threats, warnings, recommendations)
            }

            is QRCodeContent.Phone -> {
                analyzePhone(parsedContent, warnings, recommendations)
            }

            is QRCodeContent.Sms -> {
                analyzeSms(parsedContent, threats, warnings, recommendations)
            }

            is QRCodeContent.Email -> {
                analyzeEmail(parsedContent, warnings, recommendations)
            }

            is QRCodeContent.CryptoPayment -> {
                analyzeCrypto(parsedContent, threats, warnings, recommendations)
            }

            is QRCodeContent.UpiPayment -> {
                analyzeUpi(parsedContent, warnings, recommendations)
            }

            is QRCodeContent.AppLink -> {
                analyzeAppLink(parsedContent, warnings, recommendations)
            }

            is QRCodeContent.Contact -> {
                analyzeContact(parsedContent, warnings, recommendations)
            }

            else -> {
                // Low-risk content types (GeoLocation, CalendarEvent, Unknown)
            }
        }

        val riskScore = calculateRiskScore(threats, warnings)
        val riskLevel = determineRiskLevel(riskScore)
        val shouldBlock = riskLevel == RiskCategory.CRITICAL ||
            threats.any { it.severity == IssueSeverity.CRITICAL }

        val confidence = when {
            threats.isNotEmpty() -> ConfidenceLevel.HIGH
            warnings.isNotEmpty() -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.HIGH
        }

        return QRThreatAssessment(
            riskLevel = riskLevel,
            riskScore = riskScore,
            threats = threats,
            warnings = warnings,
            recommendations = recommendations,
            shouldBlock = shouldBlock,
            confidence = confidence,
        )
    }

    private fun analyzeUrl(
        content: QRCodeContent.Url,
        threats: MutableList<QRThreat>,
        warnings: MutableList<QRWarning>,
        recommendations: MutableList<String>,
    ) {
        val url = content.url
        val domain = content.domain.lowercase()

        // Check HTTPS
        if (!content.isHttps) {
            warnings.add(
                QRWarning(
                    type = QRWarningType.NO_HTTPS,
                    message = "This URL does not use secure HTTPS encryption"
                )
            )
            recommendations.add("Be cautious when entering sensitive information on non-HTTPS sites")
        }

        // Check for URL shorteners
        if (URL_SHORTENERS.any { domain.contains(it) }) {
            warnings.add(
                QRWarning(
                    type = QRWarningType.HIDDEN_REDIRECT,
                    message = "This URL uses a link shortener that hides the real destination"
                )
            )
            threats.add(
                QRThreat(
                    type = QRThreatType.URL_SHORTENER,
                    severity = IssueSeverity.MEDIUM,
                    description = "URL shorteners can hide malicious destinations",
                    evidence = "Domain: $domain"
                )
            )
            recommendations.add("Consider using a URL expander service to see the real destination")
        }

        // Check for suspicious TLDs
        if (SUSPICIOUS_TLDS.any { domain.endsWith(it) }) {
            warnings.add(
                QRWarning(
                    type = QRWarningType.SUSPICIOUS_TLD,
                    message = "This domain uses a TLD commonly associated with spam or scams"
                )
            )
        }

        // Check for IP address URLs
        if (IP_ADDRESS_PATTERN.matcher(url).find()) {
            warnings.add(
                QRWarning(
                    type = QRWarningType.IP_ADDRESS_URL,
                    message = "This URL uses an IP address instead of a domain name"
                )
            )
            threats.add(
                QRThreat(
                    type = QRThreatType.SUSPICIOUS_REDIRECT,
                    severity = IssueSeverity.HIGH,
                    description = "IP-based URLs are often used to bypass security filters",
                    evidence = url
                )
            )
            recommendations.add("Avoid visiting URLs that use IP addresses instead of domain names")
        }

        // Check for homograph/punycode attacks
        if (PUNYCODE_PATTERN.matcher(domain).find()) {
            threats.add(
                QRThreat(
                    type = QRThreatType.HOMOGRAPH_ATTACK,
                    severity = IssueSeverity.HIGH,
                    description = "This domain uses internationalized characters that may impersonate a legitimate site",
                    evidence = "Domain contains punycode: $domain"
                )
            )
            recommendations.add("This URL may be trying to impersonate a legitimate website")
        }

        // Check for phishing patterns
        analyzePhishingPatterns(domain, url, threats, recommendations)

        // Check for very long URLs (potential data exfiltration)
        if (url.length > 500) {
            warnings.add(
                QRWarning(
                    type = QRWarningType.LONG_URL,
                    message = "This URL is unusually long and may contain hidden data"
                )
            )
        }
    }

    private fun analyzeUrlString(
        url: String,
        threats: MutableList<QRThreat>,
        warnings: MutableList<QRWarning>,
        recommendations: MutableList<String>,
    ) {
        try {
            val uri = URI(url)
            val domain = uri.host?.lowercase() ?: return
            val isHttps = uri.scheme.equals("https", ignoreCase = true)

            val content = QRCodeContent.Url(
                url = url,
                domain = domain,
                isHttps = isHttps,
            )
            analyzeUrl(content, threats, warnings, recommendations)
        } catch (e: Exception) {
            // Invalid URL format
        }
    }

    private fun analyzePhishingPatterns(
        domain: String,
        url: String,
        threats: MutableList<QRThreat>,
        recommendations: MutableList<String>,
    ) {
        // Check for suspicious keywords in domain
        val domainLower = domain.lowercase()
        val suspiciousKeywordsFound = SUSPICIOUS_KEYWORDS.filter { keyword ->
            domainLower.contains(keyword) && !isLegitimateService(domain, keyword)
        }

        if (suspiciousKeywordsFound.isNotEmpty()) {
            threats.add(
                QRThreat(
                    type = QRThreatType.PHISHING_URL,
                    severity = IssueSeverity.HIGH,
                    description = "This domain contains suspicious keywords commonly used in phishing",
                    evidence = "Keywords: ${suspiciousKeywordsFound.joinToString(", ")}"
                )
            )
            recommendations.add("Verify this is the official website before entering any information")
        }

        // Check against phishing patterns
        for (pattern in PHISHING_PATTERNS) {
            if (pattern.matcher(domainLower).matches()) {
                threats.add(
                    QRThreat(
                        type = QRThreatType.PHISHING_URL,
                        severity = IssueSeverity.HIGH,
                        description = "This URL matches known phishing patterns",
                        evidence = "Pattern match: $domain"
                    )
                )
                break
            }
        }
    }

    private fun isLegitimateService(domain: String, keyword: String): Boolean {
        val legitimateDomains = mapOf(
            "google" to listOf("google.com", "google.co.", "googleapis.com"),
            "facebook" to listOf("facebook.com", "fb.com", "facebook.net"),
            "amazon" to listOf("amazon.com", "amazon.co.", "amazonaws.com"),
            "apple" to listOf("apple.com", "icloud.com"),
            "microsoft" to listOf("microsoft.com", "live.com", "outlook.com"),
            "paypal" to listOf("paypal.com", "paypal.me"),
            "instagram" to listOf("instagram.com"),
        )

        val patterns = legitimateDomains[keyword] ?: return false
        return patterns.any { domain.contains(it) }
    }

    private fun analyzeWifi(
        content: QRCodeContent.Wifi,
        threats: MutableList<QRThreat>,
        warnings: MutableList<QRWarning>,
        recommendations: MutableList<String>,
    ) {
        // Check for open WiFi networks
        if (content.securityType == WifiSecurityType.OPEN) {
            warnings.add(
                QRWarning(
                    type = QRWarningType.OPEN_WIFI,
                    message = "This is an open WiFi network without password protection"
                )
            )
            threats.add(
                QRThreat(
                    type = QRThreatType.FAKE_WIFI,
                    severity = IssueSeverity.MEDIUM,
                    description = "Open WiFi networks can be used for man-in-the-middle attacks",
                    evidence = "SSID: ${content.ssid}, Security: OPEN"
                )
            )
            recommendations.add("Use a VPN when connecting to open WiFi networks")
            recommendations.add("Avoid accessing sensitive accounts on public networks")
        }

        // Check for suspicious SSID names (impersonating legitimate networks)
        val suspiciousSSIDPatterns = listOf(
            "free", "airport", "hotel", "guest", "public", "open"
        )
        val ssidLower = content.ssid.lowercase()
        if (suspiciousSSIDPatterns.any { ssidLower.contains(it) }) {
            warnings.add(
                QRWarning(
                    type = QRWarningType.UNKNOWN_SENDER,
                    message = "This network name could be an impersonation of a public network"
                )
            )
            recommendations.add("Verify this is the official network with the establishment")
        }
    }

    private fun analyzePhone(
        content: QRCodeContent.Phone,
        warnings: MutableList<QRWarning>,
        recommendations: MutableList<String>,
    ) {
        // Check for premium rate numbers
        val number = content.number.replace(Regex("[\\s\\-()]"), "")
        if (PREMIUM_SMS_PREFIXES.any { number.startsWith(it) || number.startsWith("+1$it") }) {
            warnings.add(
                QRWarning(
                    type = QRWarningType.HIGH_AMOUNT,
                    message = "This appears to be a premium rate number that may incur charges"
                )
            )
            recommendations.add("Calling this number may result in premium charges")
        }
    }

    private fun analyzeSms(
        content: QRCodeContent.Sms,
        threats: MutableList<QRThreat>,
        warnings: MutableList<QRWarning>,
        recommendations: MutableList<String>,
    ) {
        val number = content.number.replace(Regex("[\\s\\-()]"), "")

        // Check for premium SMS
        if (PREMIUM_SMS_PREFIXES.any { number.startsWith(it) || number.startsWith("+1$it") }) {
            threats.add(
                QRThreat(
                    type = QRThreatType.PREMIUM_SMS,
                    severity = IssueSeverity.HIGH,
                    description = "This is a premium SMS number that will incur charges",
                    evidence = "Number: ${content.number}"
                )
            )
            recommendations.add("Sending SMS to this number will result in premium charges")
        }

        // Check message for suspicious content
        content.message?.let { message ->
            if (message.contains("http", ignoreCase = true)) {
                warnings.add(
                    QRWarning(
                        type = QRWarningType.HIDDEN_REDIRECT,
                        message = "The pre-filled SMS contains a URL"
                    )
                )
            }
        }
    }

    private fun analyzeEmail(
        content: QRCodeContent.Email,
        warnings: MutableList<QRWarning>,
        recommendations: MutableList<String>,
    ) {
        // Check for suspicious email domains
        val domain = content.address.substringAfter("@").lowercase()
        if (SUSPICIOUS_TLDS.any { domain.endsWith(it) }) {
            warnings.add(
                QRWarning(
                    type = QRWarningType.SUSPICIOUS_TLD,
                    message = "The email domain uses a TLD commonly associated with spam"
                )
            )
        }
    }

    private fun analyzeCrypto(
        content: QRCodeContent.CryptoPayment,
        threats: MutableList<QRThreat>,
        warnings: MutableList<QRWarning>,
        recommendations: MutableList<String>,
    ) {
        // Always warn about crypto payments
        warnings.add(
            QRWarning(
                type = QRWarningType.HIGH_AMOUNT,
                message = "Cryptocurrency transactions are irreversible"
            )
        )
        recommendations.add("Verify the recipient address carefully before sending")
        recommendations.add("Cryptocurrency transactions cannot be reversed if sent to the wrong address")

        // Check for any amount
        content.amount?.let { amount ->
            val amountValue = amount.toDoubleOrNull() ?: 0.0
            if (amountValue > 0) {
                threats.add(
                    QRThreat(
                        type = QRThreatType.CRYPTO_SCAM,
                        severity = IssueSeverity.MEDIUM,
                        description = "This QR code is requesting a cryptocurrency payment",
                        evidence = "Amount: $amount ${content.currency}"
                    )
                )
            }
        }
    }

    private fun analyzeUpi(
        content: QRCodeContent.UpiPayment,
        warnings: MutableList<QRWarning>,
        recommendations: MutableList<String>,
    ) {
        // Always warn about payment requests
        warnings.add(
            QRWarning(
                type = QRWarningType.HIGH_AMOUNT,
                message = "This QR code is requesting a payment"
            )
        )
        recommendations.add("Verify the recipient before making any payment")

        content.amount?.let { amount ->
            val amountValue = amount.toDoubleOrNull() ?: 0.0
            if (amountValue > 1000) {
                warnings.add(
                    QRWarning(
                        type = QRWarningType.HIGH_AMOUNT,
                        message = "This payment request is for a large amount: ₹$amount"
                    )
                )
            }
        }
    }

    private fun analyzeAppLink(
        content: QRCodeContent.AppLink,
        warnings: MutableList<QRWarning>,
        recommendations: MutableList<String>,
    ) {
        warnings.add(
            QRWarning(
                type = QRWarningType.EXTERNAL_APP,
                message = "This QR code will open an external application"
            )
        )
        recommendations.add("Only open app links from trusted sources")
    }

    private fun analyzeContact(
        content: QRCodeContent.Contact,
        warnings: MutableList<QRWarning>,
        recommendations: MutableList<String>,
    ) {
        // Check if contact has website that could be suspicious
        content.website?.let { website ->
            if (SUSPICIOUS_TLDS.any { website.lowercase().contains(it) }) {
                warnings.add(
                    QRWarning(
                        type = QRWarningType.SUSPICIOUS_TLD,
                        message = "The contact's website uses a suspicious domain"
                    )
                )
            }
        }
    }

    private fun calculateRiskScore(
        threats: List<QRThreat>,
        warnings: List<QRWarning>,
    ): Double {
        var score = 0.0

        // Calculate threat score
        for (threat in threats) {
            score += when (threat.severity) {
                IssueSeverity.CRITICAL -> 40.0
                IssueSeverity.HIGH -> 25.0
                IssueSeverity.MEDIUM -> 15.0
                IssueSeverity.LOW -> 5.0
            }
        }

        // Add warning score
        score += warnings.size * 5.0

        // Cap at 100
        return minOf(score, 100.0)
    }

    private fun determineRiskLevel(score: Double): RiskCategory {
        return when {
            score >= 70 -> RiskCategory.CRITICAL
            score >= 50 -> RiskCategory.HIGH
            score >= 25 -> RiskCategory.MEDIUM
            score >= 10 -> RiskCategory.LOW
            else -> RiskCategory.MINIMAL
        }
    }
}
