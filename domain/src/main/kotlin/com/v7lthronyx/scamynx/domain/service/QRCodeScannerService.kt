package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.QRActionType
import com.v7lthronyx.scamynx.domain.model.QRCodeHistoryEntry
import com.v7lthronyx.scamynx.domain.model.QRCodeScanResult
import com.v7lthronyx.scamynx.domain.model.QRCodeStatistics
import com.v7lthronyx.scamynx.domain.model.QRCodeType
import com.v7lthronyx.scamynx.domain.model.QRSafeAction
import com.v7lthronyx.scamynx.domain.model.QRScanSource
import com.v7lthronyx.scamynx.domain.model.StatisticsPeriod
import kotlinx.coroutines.flow.Flow

interface QRCodeScannerService {


    suspend fun scanQRCode(
        rawContent: String,
        source: QRScanSource = QRScanSource.MANUAL,
    ): QRCodeScanResult

    suspend fun scanQRCodeFromImage(
        imageBytes: ByteArray,
        source: QRScanSource = QRScanSource.MANUAL,
    ): QRCodeScanResult?

    suspend fun scanQRCodeFromUri(
        imageUri: String,
        source: QRScanSource = QRScanSource.MANUAL,
    ): QRCodeScanResult?

    suspend fun parseQRCode(rawContent: String): Pair<QRCodeType, Any?>


    fun startCameraScanning(): Flow<QRCodeScanResult>

    suspend fun stopCameraScanning()

    fun isCameraScanningActive(): Boolean


    suspend fun getSafeActions(scanResult: QRCodeScanResult): List<QRSafeAction>

    suspend fun executeAction(
        scanResult: QRCodeScanResult,
        actionType: QRActionType,
    ): Boolean

    suspend fun openUrlSafely(url: String): Boolean

    suspend fun saveContact(scanResult: QRCodeScanResult): Boolean

    suspend fun connectToWifi(scanResult: QRCodeScanResult): Boolean


    suspend fun getHistory(
        limit: Int = 50,
        offset: Int = 0,
        contentTypes: List<QRCodeType>? = null,
    ): List<QRCodeHistoryEntry>

    suspend fun getScanResult(scanId: String): QRCodeScanResult?

    suspend fun deleteHistoryEntry(scanId: String)

    suspend fun clearHistory()

    fun observeScans(): Flow<QRCodeScanResult>


    suspend fun getStatistics(period: StatisticsPeriod): QRCodeStatistics


    suspend fun reportMaliciousQR(
        scanResult: QRCodeScanResult,
        reason: String,
    ): Boolean


    suspend fun enableAutoScan()

    suspend fun disableAutoScan()

    suspend fun isAutoScanEnabled(): Boolean

    suspend fun enableSoundFeedback()

    suspend fun disableSoundFeedback()

    suspend fun enableVibrationFeedback()

    suspend fun disableVibrationFeedback()
}
