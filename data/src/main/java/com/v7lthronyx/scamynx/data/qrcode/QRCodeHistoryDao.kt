package com.v7lthronyx.scamynx.data.qrcode

import com.v7lthronyx.scamynx.data.db.QRCodeScanEntity
import com.v7lthronyx.scamynx.data.di.ThreatIntelJson
import com.v7lthronyx.scamynx.domain.model.QRCodeContent
import com.v7lthronyx.scamynx.domain.model.QRCodeHistoryEntry
import com.v7lthronyx.scamynx.domain.model.QRCodeMetadata
import com.v7lthronyx.scamynx.domain.model.QRCodeScanResult
import com.v7lthronyx.scamynx.domain.model.QRCodeStatistics
import com.v7lthronyx.scamynx.domain.model.QRCodeType
import com.v7lthronyx.scamynx.domain.model.QRScanSource
import com.v7lthronyx.scamynx.domain.model.QRThreatAssessment
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.StatisticsPeriod
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class QRCodeHistoryDao @Inject constructor(
    private val roomDao: QRCodeHistoryRoomDao,
    @ThreatIntelJson private val json: Json,
) {

    suspend fun insertScan(result: QRCodeScanResult) {
        roomDao.insertScan(result.toEntity(json))
    }

    suspend fun getScanById(scanId: String): QRCodeScanResult? {
        return roomDao.getScan(scanId)?.toDomain(json)
    }

    suspend fun getHistory(
        limit: Int,
        offset: Int,
        contentTypes: List<QRCodeType>?,
    ): List<QRCodeHistoryEntry> {
        val typeFilters = contentTypes?.map { it.name }.orEmpty()
        val entities = if (typeFilters.isEmpty()) {
            roomDao.getHistory(limit, offset)
        } else {
            roomDao.getHistoryFiltered(limit, offset, typeFilters)
        }

        return entities
            .mapNotNull { it.toDomain(json) }
            .map { scan ->
                QRCodeHistoryEntry(
                    id = scan.id,
                    contentType = scan.contentType,
                    displayLabel = createDisplayLabel(scan),
                    riskLevel = scan.threatAssessment?.riskLevel ?: RiskCategory.MINIMAL,
                    wasBlocked = scan.threatAssessment?.shouldBlock ?: false,
                    scannedAt = scan.scannedAt,
                    source = scan.source,
                )
            }
    }

    suspend fun deleteScan(scanId: String) {
        roomDao.deleteScan(scanId)
    }

    suspend fun clearAll() {
        roomDao.clearAll()
    }

    suspend fun markAsReported(scanId: String, reason: String) {
        roomDao.markAsReported(scanId, reason)
    }

    suspend fun getStatistics(period: StatisticsPeriod, now: Instant): QRCodeStatistics {
        val startTime = calculatePeriodStart(period, now)
        val scansInPeriod = roomDao.getScansSince(startTime.toEpochMilliseconds())
            .mapNotNull { it.toDomain(json) }

        val totalScans = scansInPeriod.size
        val safeScans = scansInPeriod.count { it.isSafe }
        val blockedScans = scansInPeriod.count { it.threatAssessment?.shouldBlock == true }

        val scansByType = scansInPeriod
            .groupBy { it.contentType }
            .mapValues { it.value.size }

        val threatsByType = scansInPeriod
            .flatMap { scan -> scan.threatAssessment?.threats ?: emptyList() }
            .groupBy { it.type }
            .mapValues { it.value.size }

        return QRCodeStatistics(
            totalScans = totalScans,
            safeScans = safeScans,
            blockedScans = blockedScans,
            scansByType = scansByType,
            threatsByType = threatsByType,
            period = period,
            generatedAt = now,
        )
    }

    private fun calculatePeriodStart(period: StatisticsPeriod, now: Instant): Instant {
        val tz = TimeZone.currentSystemDefault()
        return when (period) {
            StatisticsPeriod.TODAY -> now.minus(1, DateTimeUnit.DAY, tz)
            StatisticsPeriod.WEEK -> now.minus(7, DateTimeUnit.DAY, tz)
            StatisticsPeriod.MONTH -> now.minus(30, DateTimeUnit.DAY, tz)
            StatisticsPeriod.YEAR -> now.minus(365, DateTimeUnit.DAY, tz)
            StatisticsPeriod.ALL_TIME -> Instant.DISTANT_PAST
        }
    }

    private fun createDisplayLabel(scan: QRCodeScanResult): String {
        return when (val content = scan.parsedContent) {
            is QRCodeContent.Url -> content.domain
            is QRCodeContent.Text -> {
                content.text.take(50).let { if (content.text.length > 50) "$it..." else it }
            }
            is QRCodeContent.Email -> content.address
            is QRCodeContent.Phone -> content.number
            is QRCodeContent.Sms -> "SMS: ${content.number}"
            is QRCodeContent.Wifi -> "WiFi: ${content.ssid}"
            is QRCodeContent.Contact -> content.name ?: "Contact"
            is QRCodeContent.GeoLocation -> content.label ?: "Location: ${content.latitude}, ${content.longitude}"
            is QRCodeContent.CalendarEvent -> content.title
            is QRCodeContent.CryptoPayment -> "${content.currency}: ${content.address.take(20)}..."
            is QRCodeContent.UpiPayment -> "UPI: ${content.payeeName ?: content.payeeAddress}"
            is QRCodeContent.AppLink -> "${content.scheme}://${content.host ?: ""}"
            is QRCodeContent.Unknown -> scan.rawContent.take(30).let { if (scan.rawContent.length > 30) "$it..." else it }
        }
    }
}

private fun QRCodeScanResult.toEntity(json: Json): QRCodeScanEntity = QRCodeScanEntity(
    id = id,
    rawContent = rawContent,
    contentType = contentType.name,
    parsedContentJson = json.encodeToString(QRCodeContent.serializer(), parsedContent),
    isSafe = isSafe,
    threatAssessmentJson = threatAssessment?.let { json.encodeToString(QRThreatAssessment.serializer(), it) },
    metadataJson = json.encodeToString(QRCodeMetadata.serializer(), metadata),
    scannedAtEpoch = scannedAt.toEpochMilliseconds(),
    source = source.name,
    wasBlocked = threatAssessment?.shouldBlock ?: false,
    reportedReason = null,
)

private fun QRCodeScanEntity.toDomain(json: Json): QRCodeScanResult? = try {
    QRCodeScanResult(
        id = id,
        rawContent = rawContent,
        contentType = QRCodeType.valueOf(contentType),
        parsedContent = json.decodeFromString(QRCodeContent.serializer(), parsedContentJson),
        isSafe = isSafe,
        threatAssessment = threatAssessmentJson?.let { json.decodeFromString(QRThreatAssessment.serializer(), it) },
        metadata = json.decodeFromString(QRCodeMetadata.serializer(), metadataJson),
        scannedAt = Instant.fromEpochMilliseconds(scannedAtEpoch),
        source = QRScanSource.valueOf(source),
    )
} catch (e: Exception) {
    null
}
