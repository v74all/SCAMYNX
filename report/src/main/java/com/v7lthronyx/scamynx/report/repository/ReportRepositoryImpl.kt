package com.v7lthronyx.scamynx.report.repository

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.v7lthronyx.scamynx.domain.model.GeneratedReport
import com.v7lthronyx.scamynx.domain.model.ReportFormat
import com.v7lthronyx.scamynx.domain.model.ScanResult
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.repository.ReportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class ReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReportRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    override suspend fun generate(reportFormat: ReportFormat, result: ScanResult): GeneratedReport = withContext(Dispatchers.IO) {
        when (reportFormat) {
            ReportFormat.JSON -> createJsonReport(result)
            ReportFormat.PDF -> createPdfReport(result)
        }
    }

    private fun createJsonReport(result: ScanResult): GeneratedReport {
        val (file, displayName) = createOutputFile(extension = "json")
        file.writeText(json.encodeToString(ScanResult.serializer(), result))
        val uri = file.toContentUri()
        return GeneratedReport(
            format = ReportFormat.JSON,
            uri = uri.toString(),
            sizeBytes = file.length(),
            fileName = displayName,
            mimeType = "application/json",
        )
    }

    private fun createPdfReport(result: ScanResult): GeneratedReport {
        val (file, displayName) = createOutputFile(extension = "pdf")
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val titlePaint = Paint().apply {
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint().apply {
                textSize = 12f
            }
            var cursorY = 60f
            canvas.drawText("SCAMYNX Threat Report", 40f, cursorY, titlePaint)
            cursorY += 28f

            val lines = buildList {
                add("Target: ${result.targetLabel}")
                add("Type: ${targetTypeLabel(result.targetType)}")
                result.normalizedUrl?.let { add("Normalized: $it") }
                add("Generated: ${formatInstant(result.createdAt)}")
                add("Risk Score: ${"%.2f".format(result.risk)} / 5")
                add("Vendors:")
                result.vendors.forEach { vendor ->
                    add(" - ${vendor.provider}: ${vendor.status} (${"%.2f".format(vendor.score)})")
                }
                result.file?.let { file ->
                    add("File size: ${file.sizeBytes?.let(::formatBytes) ?: "unknown"}")
                    file.sha256?.let { add("File SHA-256: $it") }
                    if (file.issues.isNotEmpty()) {
                        add("File flags: ${file.issues.joinToString { it.title }}")
                    }
                }
                result.vpn?.let { vpn ->
                    add("VPN server: ${vpn.serverAddress ?: "unknown"}")
                    vpn.port?.let { add("VPN port: $it") }
                    add("VPN TLS: ${vpn.tlsEnabled ?: "unknown"}")
                    if (vpn.issues.isNotEmpty()) {
                        add("VPN flags: ${vpn.issues.joinToString { it.title }}")
                    }
                }
                result.instagram?.let { insta ->
                    add("Instagram handle: ${insta.handle}")
                    insta.displayName?.let { add("Display name: $it") }
                    insta.followerCount?.let { add("Followers: $it") }
                    if (insta.issues.isNotEmpty()) {
                        add("Instagram flags: ${insta.issues.joinToString { it.title }}")
                    }
                }
                result.network?.let { network ->
                    add("Network TLS: ${network.tlsVersion ?: "unknown"}")
                    add("Certificate valid: ${network.certValid ?: "unknown"}")
                }
                result.ml?.let { ml ->
                    add("ML Probability: ${ml.probability}")
                }
            }
            lines.forEach { line ->
                canvas.drawText(line, 40f, cursorY, bodyPaint)
                cursorY += 16f
            }

            document.finishPage(page)
            file.outputStream().use { output ->
                document.writeTo(output)
            }
        } catch (ioException: IOException) {
            file.delete()
            throw ioException
        } finally {
            document.close()
        }
        val uri = file.toContentUri()
        return GeneratedReport(
            format = ReportFormat.PDF,
            uri = uri.toString(),
            sizeBytes = file.length(),
            fileName = displayName,
            mimeType = "application/pdf",
        )
    }

    private fun createOutputFile(extension: String): Pair<File, String> {
        val directory = File(context.filesDir, "reports").apply { mkdirs() }
        val timestamp = formatInstant(Clock.System.now()).replace(Regex("[^0-9_]") , "")
        val fileName = "report_${timestamp}.$extension"
        val file = File(directory, fileName)
        return file to fileName
    }

    private fun targetTypeLabel(type: ScanTargetType): String = when (type) {
        ScanTargetType.URL -> "URL scan"
        ScanTargetType.FILE -> "File scan"
        ScanTargetType.VPN_CONFIG -> "VPN configuration"
        ScanTargetType.INSTAGRAM -> "Instagram investigation"
    }

    private fun File.toContentUri(): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        this,
    )
}

private fun formatBytes(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = sizeBytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex++
    }
    return "%.1f %s".format(size, units[unitIndex])
}

private fun formatInstant(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "%04d-%02d-%02d %02d:%02d:%02d".format(
        local.year,
        local.monthNumber,
        local.dayOfMonth,
        local.hour,
        local.minute,
        local.second,
    )
}
