package com.v7lthronyx.scamynx.data.analyzer

import com.v7lthronyx.scamynx.domain.model.InstagramScanReport
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.ScanIssue
import com.v7lthronyx.scamynx.domain.model.VendorVerdict
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val HANDLE_KEYWORDS = listOf(
    "support",
    "verify",
    "helpcenter",
    "restore",
    "claim",
    "official",
    "giveaway",
    "free",
    "bonus",
    "lottery",
    "crypto",
    "wallet",
    "instagram",
    "meta",
    "payment",
    "refund",
)

private val MESSAGE_KEYWORDS = listOf(
    "verify your account",
    "reset your password",
    "violation",
    "copyright",
    "appeal",
    "click the link",
    "within 24 hours",
    "confirm ownership",
    "monetization",
    "business manager",
)

private val SEVERITY_WEIGHTS = mapOf(
    IssueSeverity.LOW to 0.15,
    IssueSeverity.MEDIUM to 0.4,
    IssueSeverity.HIGH to 0.65,
    IssueSeverity.CRITICAL to 0.85,
)

@Singleton
class InstagramScamAnalyzer @Inject constructor() {

    data class Result(
        val report: InstagramScanReport,
        val verdict: VendorVerdict,
    )

    fun analyze(raw: String, metadata: Map<String, String>): Result {
        val normalizedHandle = extractHandle(raw)
        val displayName = metadata["displayName"]?.takeIf { it.isNotBlank() }
        val followerCount = metadata["followerCount"]?.toIntOrNull()
        val bio = metadata["bio"] ?: ""
        val message = metadata["message"] ?: ""

        val issues = mutableListOf<ScanIssue>()
        val suspiciousSignals = mutableListOf<String>()

        if (normalizedHandle.isBlank()) {
            issues += ScanIssue(
                id = "invalid_handle",
                title = "Handle missing or invalid",
                severity = IssueSeverity.MEDIUM,
                description = "Unable to interpret the Instagram handle from the provided input.",
            )
        }

        val lowerHandle = normalizedHandle.lowercase(Locale.US)
        HANDLE_KEYWORDS.forEach { keyword ->
            if (lowerHandle.contains(keyword)) {
                suspiciousSignals += "Handle contains \"$keyword\""
            }
        }
        if (suspiciousSignals.isNotEmpty()) {
            issues += ScanIssue(
                id = "keyword_handle",
                title = "Handle resembles phishing keywords",
                severity = IssueSeverity.HIGH,
                description = suspiciousSignals.joinToString(),
            )
        }

        if (lowerHandle.count { it.isDigit() } > lowerHandle.length / 2 && lowerHandle.isNotBlank()) {
            issues += ScanIssue(
                id = "excessive_digits",
                title = "Handle contains many digits",
                severity = IssueSeverity.MEDIUM,
                description = "Scam profiles often append random digits to appear legitimate.",
            )
        }

        if (lowerHandle.length >= 20) {
            issues += ScanIssue(
                id = "long_handle",
                title = "Handle unusually long",
                severity = IssueSeverity.LOW,
                description = "Very long handles are commonly used by impersonation attempts.",
            )
        }

        val lowerMessage = message.lowercase(Locale.US)
        val lowerBio = bio.lowercase(Locale.US)
        val combinedText = lowerMessage + " " + lowerBio
        val messageHits = MESSAGE_KEYWORDS.filter { combinedText.contains(it) }
        if (messageHits.isNotEmpty()) {
            issues += ScanIssue(
                id = "message_phish",
                title = "Scam-like messaging",
                severity = IssueSeverity.CRITICAL,
                description = messageHits.joinToString(),
            )
        }

        if (combinedText.contains("http://") || combinedText.contains("bit.ly") || combinedText.contains("tinyurl")) {
            issues += ScanIssue(
                id = "external_link",
                title = "Links to external site",
                severity = IssueSeverity.HIGH,
                description = "Scammers often send external links to harvest credentials.",
            )
        }

        if (followerCount != null && followerCount < 50 && issues.isNotEmpty()) {
            issues += ScanIssue(
                id = "low_followers",
                title = "Very low follower count",
                severity = IssueSeverity.LOW,
                description = "New or low-trust accounts are frequently used for scams.",
            )
        }

        val riskScore = issues.sumOf { SEVERITY_WEIGHTS[it.severity] ?: 0.0 }
            .coerceIn(0.0, 1.0)
        val status = when {
            riskScore >= 0.75 -> VerdictStatus.MALICIOUS
            riskScore >= 0.5 -> VerdictStatus.SUSPICIOUS
            riskScore >= 0.25 -> VerdictStatus.UNKNOWN
            else -> VerdictStatus.CLEAN
        }

        val report = InstagramScanReport(
            handle = if (normalizedHandle.isNotBlank()) "@$normalizedHandle" else raw,
            displayName = displayName,
            followerCount = followerCount,
            suspiciousSignals = suspiciousSignals.distinct(),
            issues = issues,
            riskScore = riskScore,
        )
        val verdict = VendorVerdict(
            provider = Provider.INSTAGRAM,
            status = status,
            score = riskScore,
            details = mapOf(
                "handle" to report.handle,
                "issueCount" to issues.size.takeIf { it > 0 }?.toString(),
            ).filterValues { it != null },
        )
        return Result(report = report, verdict = verdict)
    }

    private fun extractHandle(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        var handle = trimmed
            .removePrefix("https://www.instagram.com/")
            .removePrefix("http://www.instagram.com/")
            .removePrefix("https://instagram.com/")
            .removePrefix("http://instagram.com/")
            .removePrefix("@")
        handle = handle.substringBefore('?')
        handle = handle.substringBefore('/')
        return handle.filter { it.isLetterOrDigit() || it == '.' || it == '_' }
    }
}
