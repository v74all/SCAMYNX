package com.v7lthronyx.scamynx.data.analyzer

import com.v7lthronyx.scamynx.domain.model.FileScanReport
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.ScanIssue
import com.v7lthronyx.scamynx.domain.model.VendorVerdict
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val HIGH_RISK_EXTENSIONS = setOf(
    "apk",
    "exe",
    "bat",
    "cmd",
    "jar",
    "scr",
    "msi",
    "js",
    "vbs",
    "ps1",
    "docm",
    "xlsm",
)

private val SUSPICIOUS_KEYWORDS = listOf(
    "eval(",
    "document.write",
    "powershell",
    "cmd.exe",
    "System.Net.WebClient",
    "Invoke-Expression",
    "base64_decode",
    "fromBase64String",
    "vless://",
    "vmess://",
    "trojan://",
    "instagram.com/password",
    "two_factor",
    "checkpoint_required",
    "free followers",
    "crypto wallet",
    "seed phrase",
)

private val SEVERITY_WEIGHTS = mapOf(
    IssueSeverity.LOW to 0.15,
    IssueSeverity.MEDIUM to 0.4,
    IssueSeverity.HIGH to 0.7,
    IssueSeverity.CRITICAL to 0.9,
)

@Singleton
class FileStaticAnalyzer @Inject constructor() {

    data class Input(
        val content: ByteArray?,
        val fileName: String?,
        val mimeType: String?,
        val sizeBytes: Long?,
    )

    data class Result(
        val report: FileScanReport,
        val verdict: VendorVerdict,
    )

    fun analyze(input: Input): Result {
        val normalizedName = input.fileName.orEmpty()
        val extension = normalizedName.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.US)
        val issues = mutableListOf<ScanIssue>()
        val suspiciousMatches = linkedSetOf<String>()

        if (extension in HIGH_RISK_EXTENSIONS) {
            issues += ScanIssue(
                id = "high_risk_extension",
                title = "High risk file extension",
                severity = IssueSeverity.HIGH,
                description = "Files with .$extension can execute code and are often abused in scams.",
            )
        }

        val contentText = input.content?.let { bytes ->
            buildString {
                bytes.take(4096).forEach { append(if (it in 32..126) it.toInt().toChar() else ' ') }
            }.lowercase(Locale.US)
        }.orEmpty()

        SUSPICIOUS_KEYWORDS.forEach { keyword ->
            if (contentText.contains(keyword.lowercase(Locale.US))) {
                suspiciousMatches += keyword
            }
        }

        if (suspiciousMatches.isNotEmpty()) {
            issues += ScanIssue(
                id = "suspicious_strings",
                title = "Suspicious strings detected",
                severity = IssueSeverity.MEDIUM,
                description = suspiciousMatches.joinToString(),
            )
        }

        if (contentText.contains("http://") && !contentText.contains("https://")) {
            issues += ScanIssue(
                id = "insecure_links",
                title = "Insecure links found",
                severity = IssueSeverity.MEDIUM,
                description = "File references unencrypted HTTP resources.",
            )
        }

        if (contentText.count { it.isLetterOrDigit() } < contentText.length / 2 && contentText.isNotBlank()) {
            issues += ScanIssue(
                id = "obfuscation",
                title = "Possible obfuscation",
                severity = IssueSeverity.MEDIUM,
                description = "Large portions of the file look obfuscated or binary encoded.",
            )
        }

        if (input.sizeBytes != null && input.sizeBytes == 0L) {
            issues += ScanIssue(
                id = "empty_file",
                title = "Empty file",
                severity = IssueSeverity.LOW,
                description = "Empty files are often placeholders used to mislead victims.",
            )
        }

        val hasContent = input.content != null && input.content.isNotEmpty()
        val sha256 = if (hasContent) hashSha256(input.content!!) else null

        val cumulativeScore = issues.sumOf { issue -> SEVERITY_WEIGHTS[issue.severity] ?: 0.0 }
        val riskScore = cumulativeScore.coerceIn(0.0, 1.0)
        val status = when {
            riskScore >= 0.75 -> VerdictStatus.MALICIOUS
            riskScore >= 0.45 -> VerdictStatus.SUSPICIOUS
            riskScore >= 0.2 -> VerdictStatus.UNKNOWN
            else -> VerdictStatus.CLEAN
        }

        val report = FileScanReport(
            fileName = normalizedName.ifBlank { "Unlabeled file" },
            mimeType = input.mimeType,
            sizeBytes = input.sizeBytes,
            sha256 = sha256,
            suspiciousStrings = suspiciousMatches.toList(),
            issues = issues,
            riskScore = riskScore,
        )
        val verdict = VendorVerdict(
            provider = Provider.FILE_STATIC,
            status = status,
            score = riskScore,
            details = mapOf(
                "fileName" to report.fileName,
                "extension" to extension.ifBlank { null },
                "matchCount" to suspiciousMatches.size.toString(),
            ).filterValues { it != null },
        )
        return Result(report = report, verdict = verdict)
    }

    private fun hashSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString(separator = "") { "%02x".format(it) }
    }
}
