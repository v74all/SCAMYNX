package com.v7lthronyx.scamynx.ml.dataset

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhishingDataset @Inject constructor() {

    private val phishingPatterns = listOf(
        
        "login-verify", "login-secure", "login-update", "signin-verify",
        "account-verify", "account-suspended", "account-security", "account-locked",
        "verify-identity", "verify-account", "verify-now", "confirm-identity",
        "password-reset", "password-change", "password-update", "password-expired",
        "security-check", "security-alert", "security-update", "security-warning",

        "urgent-action", "immediate-action", "action-required", "required-action",
        "final-warning", "last-chance", "act-now", "expires-today",
        "suspended-account", "limited-access", "restricted-account",

        "banking-secure", "bank-verify", "bank-login", "online-banking",
        "payment-verify", "payment-confirm", "payment-failed", "payment-declined",
        "card-verify", "card-update", "card-suspended", "credit-alert",
        "transaction-verify", "suspicious-activity", "unusual-activity",

        "prize-winner", "you-won", "claim-reward", "claim-prize",
        "congratulations-winner", "free-gift", "exclusive-offer",
        "lottery-winner", "lucky-winner", "selected-winner",

        "document-share", "file-share", "shared-document", "view-document",
        "invoice-attached", "download-invoice", "order-confirmation",

        "support-ticket", "helpdesk-verify", "tech-support", "customer-service",
        "update-info", "update-details", "update-billing", "billing-issue",

        "subscription-expired", "renew-subscription", "subscription-cancelled",
        "membership-expired", "upgrade-account", "premium-expired",

        "secure-login", "secure-verify", "ssl-verify", "https-verify",
        "web-login", "web-secure", "portal-access", "online-access",
        "app-verify", "mobile-verify", "device-verify", "auth-verify",
    )

    private val brandImpersonationPatterns = mapOf(
        
        "paypal" to listOf("paypal-verify", "paypal-confirm", "paypal-login", "paypal-security", "pp-verify"),
        "venmo" to listOf("venmo-verify", "venmo-confirm", "venmo-payment"),
        "cashapp" to listOf("cashapp-verify", "cash-verify", "square-cash"),

        "google" to listOf("google-verify", "gmail-verify", "google-security", "goog1e", "gooogle"),
        "microsoft" to listOf("microsoft-verify", "office365-verify", "outlook-verify", "msft-login", "micr0soft"),
        "apple" to listOf("apple-verify", "icloud-verify", "appleid-verify", "apple-support", "app1e"),
        "amazon" to listOf("amazon-verify", "amazon-prime", "amaz0n", "amazom", "amazon-delivery"),

        "facebook" to listOf("facebook-verify", "fb-verify", "facebook-security", "faceb00k"),
        "instagram" to listOf("instagram-verify", "ig-verify", "insta-verify", "1nstagram"),
        "twitter" to listOf("twitter-verify", "twitter-security", "tw1tter"),
        "linkedin" to listOf("linkedin-verify", "linkedin-security", "l1nkedin"),
        "tiktok" to listOf("tiktok-verify", "tikt0k"),

        "netflix" to listOf("netflix-verify", "netflix-update", "netf1ix", "netflix-billing"),
        "spotify" to listOf("spotify-verify", "spotify-premium", "sp0tify"),
        "disney" to listOf("disneyplus-verify", "disney-verify", "d1sney"),

        "bank" to listOf("bank-verify", "banking-secure", "online-bank", "bank-alert"),
        "chase" to listOf("chase-verify", "chase-secure", "chas3"),
        "wellsfargo" to listOf("wellsfargo-verify", "wells-fargo-secure"),
        "citibank" to listOf("citi-verify", "citibank-secure"),

        "coinbase" to listOf("coinbase-verify", "coinbase-wallet", "c0inbase"),
        "binance" to listOf("binance-verify", "binance-wallet", "b1nance"),
        "metamask" to listOf("metamask-verify", "metamask-connect", "metam4sk"),

        "ups" to listOf("ups-delivery", "ups-tracking", "ups-package"),
        "fedex" to listOf("fedex-delivery", "fedex-tracking", "fedex-package"),
        "usps" to listOf("usps-delivery", "usps-tracking", "usps-package"),
        "dhl" to listOf("dhl-delivery", "dhl-tracking", "dhl-package"),
    )

    private val maliciousDomainPatterns = listOf(
        
        Regex("""[a-z]{10,}\d{3,}"""),
        Regex("""\d{5,}[a-z]{5,}"""),

        Regex("""\d{4}-\d{2}-\d{2}"""),
        Regex("""\d{8}[a-z]+"""),

        Regex("""[a-f0-9]{16,}"""),

        Regex("""[a-z]+-[a-z]+-[a-z]+-[a-z]+"""),
    )

    private val safeDomains = setOf(
        "google.com", "microsoft.com", "apple.com", "amazon.com", "facebook.com",
        "instagram.com", "twitter.com", "linkedin.com", "netflix.com", "spotify.com",
        "github.com", "stackoverflow.com", "wikipedia.org", "reddit.com",
        "paypal.com", "chase.com", "wellsfargo.com", "bankofamerica.com",
        "coinbase.com", "binance.com", "dropbox.com", "adobe.com",
    )

    fun matchPhishingPatterns(url: String): Float {
        val normalizedUrl = url.lowercase()

        if (safeDomains.any {
                normalizedUrl.contains(it) &&
                    !normalizedUrl.contains("-$it") &&
                    !normalizedUrl.contains("$it.")
            }
        ) {
            return 0f
        }

        var score = 0f
        var matchCount = 0

        for (pattern in phishingPatterns) {
            if (normalizedUrl.contains(pattern)) {
                matchCount++
                score += 0.15f
            }
        }

        for ((brand, patterns) in brandImpersonationPatterns) {
            for (pattern in patterns) {
                if (normalizedUrl.contains(pattern)) {
                    score += 0.25f
                    matchCount++
                }
            }
            
            if (normalizedUrl.contains(brand) &&
                !safeDomains.any { normalizedUrl.contains(it) }
            ) {
                score += 0.1f
            }
        }

        for (regex in maliciousDomainPatterns) {
            if (regex.containsMatchIn(normalizedUrl)) {
                score += 0.1f
            }
        }

        return score.coerceIn(0f, 1f)
    }

    fun detectImpersonatedBrand(url: String): String? {
        val normalizedUrl = url.lowercase()

        for ((brand, patterns) in brandImpersonationPatterns) {
            for (pattern in patterns) {
                if (normalizedUrl.contains(pattern)) {
                    return brand
                }
            }
        }
        return null
    }

    fun getTldRiskScore(tld: String): Float {
        return tldRiskScores[tld.lowercase()] ?: 0f
    }

    private val tldRiskScores = mapOf(
        "xyz" to 0.8f, "tk" to 0.9f, "ml" to 0.85f, "ga" to 0.85f, "cf" to 0.85f,
        "gq" to 0.85f, "top" to 0.7f, "club" to 0.6f, "icu" to 0.75f, "buzz" to 0.65f,
        "work" to 0.55f, "live" to 0.5f, "stream" to 0.6f, "click" to 0.7f,
        "link" to 0.6f, "info" to 0.4f, "online" to 0.5f, "site" to 0.5f,
        "website" to 0.5f, "space" to 0.5f, "fun" to 0.55f, "monster" to 0.6f,
    )

    fun isSafeDomain(domain: String): Boolean {
        return safeDomains.any { domain.endsWith(it) }
    }

    fun getDatasetStats(): DatasetStats {
        return DatasetStats(
            phishingPatternCount = phishingPatterns.size,
            brandPatternCount = brandImpersonationPatterns.values.sumOf { it.size },
            maliciousPatternCount = maliciousDomainPatterns.size,
            safeDomainCount = safeDomains.size,
        )
    }
}

data class DatasetStats(
    val phishingPatternCount: Int,
    val brandPatternCount: Int,
    val maliciousPatternCount: Int,
    val safeDomainCount: Int,
)
