package com.v7lthronyx.scamynx.data.qrcode

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.domain.model.ConfidenceLevel
import com.v7lthronyx.scamynx.domain.model.ImageSource
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.QRActionType
import com.v7lthronyx.scamynx.domain.model.QRCodeContent
import com.v7lthronyx.scamynx.domain.model.QRCodeHistoryEntry
import com.v7lthronyx.scamynx.domain.model.QRCodeMetadata
import com.v7lthronyx.scamynx.domain.model.QRCodeScanResult
import com.v7lthronyx.scamynx.domain.model.QRCodeStatistics
import com.v7lthronyx.scamynx.domain.model.QRCodeType
import com.v7lthronyx.scamynx.domain.model.QRSafeAction
import com.v7lthronyx.scamynx.domain.model.QRScanSource
import com.v7lthronyx.scamynx.domain.model.QRThreat
import com.v7lthronyx.scamynx.domain.model.QRThreatAssessment
import com.v7lthronyx.scamynx.domain.model.QRThreatType
import com.v7lthronyx.scamynx.domain.model.QRWarning
import com.v7lthronyx.scamynx.domain.model.QRWarningType
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.StatisticsPeriod
import com.v7lthronyx.scamynx.domain.model.WifiSecurityType
import com.v7lthronyx.scamynx.domain.service.QRCodeScannerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of QRCodeScannerService using Google ML Kit for barcode detection.
 * Supports camera-based scanning, image file scanning, and raw content parsing.
 */
@Singleton
class QRCodeScannerServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val qrCodeParser: QRCodeParser,
    private val qrCodeThreatAnalyzer: QRCodeThreatAnalyzer,
    private val qrCodeHistoryDao: QRCodeHistoryDao,
    private val qrCodePreferences: QRCodePreferences,
) : QRCodeScannerService {

    private val barcodeScanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE, Barcode.FORMAT_DATA_MATRIX)
            .build()
        BarcodeScanning.getClient(options)
    }

    private val isCameraScanning = AtomicBoolean(false)
    private val scanResultsFlow = MutableSharedFlow<QRCodeScanResult>(replay = 1)

    // region Scanning Methods

    override suspend fun scanQRCode(
        rawContent: String,
        source: QRScanSource,
    ): QRCodeScanResult = withContext(dispatcher) {
        val startTime = System.currentTimeMillis()
        val (contentType, parsedContent) = qrCodeParser.parse(rawContent)
        val scanDuration = System.currentTimeMillis() - startTime

        val threatAssessment = qrCodeThreatAnalyzer.analyze(rawContent, contentType, parsedContent)
        val isSafe = threatAssessment.riskLevel == RiskCategory.MINIMAL ||
            threatAssessment.riskLevel == RiskCategory.LOW

        val result = QRCodeScanResult(
            id = UUID.randomUUID().toString(),
            rawContent = rawContent,
            contentType = contentType,
            parsedContent = parsedContent,
            isSafe = isSafe,
            threatAssessment = threatAssessment,
            metadata = QRCodeMetadata(
                format = "QR_CODE",
                characterCount = rawContent.length,
                scanDuration = scanDuration,
                imageSource = null,
            ),
            scannedAt = clock.now(),
            source = source,
        )

        // Store in history
        qrCodeHistoryDao.insertScan(result)
        scanResultsFlow.emit(result)

        result
    }

    override suspend fun scanQRCodeFromImage(
        imageBytes: ByteArray,
        source: QRScanSource,
    ): QRCodeScanResult? = withContext(dispatcher) {
        val startTime = System.currentTimeMillis()

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return@withContext null

        val image = InputImage.fromBitmap(bitmap, 0)
        val barcodes = scanImage(image)

        if (barcodes.isEmpty()) return@withContext null

        val barcode = barcodes.first()
        val rawContent = barcode.rawValue ?: return@withContext null

        val (contentType, parsedContent) = qrCodeParser.parse(rawContent)
        val scanDuration = System.currentTimeMillis() - startTime

        val threatAssessment = qrCodeThreatAnalyzer.analyze(rawContent, contentType, parsedContent)
        val isSafe = threatAssessment.riskLevel == RiskCategory.MINIMAL ||
            threatAssessment.riskLevel == RiskCategory.LOW

        val result = QRCodeScanResult(
            id = UUID.randomUUID().toString(),
            rawContent = rawContent,
            contentType = contentType,
            parsedContent = parsedContent,
            isSafe = isSafe,
            threatAssessment = threatAssessment,
            metadata = QRCodeMetadata(
                format = mapBarcodeFormat(barcode.format),
                errorCorrectionLevel = null,
                version = null,
                characterCount = rawContent.length,
                scanDuration = scanDuration,
                imageSource = ImageSource.GALLERY,
            ),
            scannedAt = clock.now(),
            source = source,
        )

        qrCodeHistoryDao.insertScan(result)
        scanResultsFlow.emit(result)

        result
    }

    override suspend fun scanQRCodeFromUri(
        imageUri: String,
        source: QRScanSource,
    ): QRCodeScanResult? = withContext(dispatcher) {
        val startTime = System.currentTimeMillis()

        val uri = Uri.parse(imageUri)
        val image = InputImage.fromFilePath(context, uri)
        val barcodes = scanImage(image)

        if (barcodes.isEmpty()) return@withContext null

        val barcode = barcodes.first()
        val rawContent = barcode.rawValue ?: return@withContext null

        val (contentType, parsedContent) = qrCodeParser.parse(rawContent)
        val scanDuration = System.currentTimeMillis() - startTime

        val threatAssessment = qrCodeThreatAnalyzer.analyze(rawContent, contentType, parsedContent)
        val isSafe = threatAssessment.riskLevel == RiskCategory.MINIMAL ||
            threatAssessment.riskLevel == RiskCategory.LOW

        val result = QRCodeScanResult(
            id = UUID.randomUUID().toString(),
            rawContent = rawContent,
            contentType = contentType,
            parsedContent = parsedContent,
            isSafe = isSafe,
            threatAssessment = threatAssessment,
            metadata = QRCodeMetadata(
                format = mapBarcodeFormat(barcode.format),
                characterCount = rawContent.length,
                scanDuration = scanDuration,
                imageSource = determineImageSource(uri),
            ),
            scannedAt = clock.now(),
            source = source,
        )

        qrCodeHistoryDao.insertScan(result)
        scanResultsFlow.emit(result)

        result
    }

    override suspend fun parseQRCode(rawContent: String): Pair<QRCodeType, Any?> =
        withContext(dispatcher) {
            qrCodeParser.parse(rawContent).let { (type, content) ->
                type to content
            }
        }

    // endregion

    // region Camera Scanning

    override fun startCameraScanning(): Flow<QRCodeScanResult> = callbackFlow {
        isCameraScanning.set(true)

        // This flow is used by the UI to collect scan results
        // The actual camera preview and analysis is handled by CameraX in the UI layer
        // This method provides the ImageAnalysis.Analyzer callback results
        val subscription = scanResultsFlow.subscribeToResults { result ->
            trySend(result)
        }

        awaitClose {
            isCameraScanning.set(false)
            subscription.cancel()
        }
    }

    override suspend fun stopCameraScanning() {
        isCameraScanning.set(false)
    }

    override fun isCameraScanningActive(): Boolean = isCameraScanning.get()

    /**
     * Creates an ImageAnalysis.Analyzer for CameraX integration.
     * The UI layer should use this analyzer with CameraX to process camera frames.
     */
    @OptIn(ExperimentalGetImage::class)
    fun createImageAnalyzer(onResult: (QRCodeScanResult) -> Unit): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.let { barcode ->
                            barcode.rawValue?.let { rawContent ->
                                val (contentType, parsedContent) = qrCodeParser.parse(rawContent)
                                val threatAssessment = qrCodeThreatAnalyzer.analyzeSync(
                                    rawContent, contentType, parsedContent
                                )
                                val isSafe = threatAssessment.riskLevel == RiskCategory.MINIMAL ||
                                    threatAssessment.riskLevel == RiskCategory.LOW

                                val result = QRCodeScanResult(
                                    id = UUID.randomUUID().toString(),
                                    rawContent = rawContent,
                                    contentType = contentType,
                                    parsedContent = parsedContent,
                                    isSafe = isSafe,
                                    threatAssessment = threatAssessment,
                                    metadata = QRCodeMetadata(
                                        format = mapBarcodeFormat(barcode.format),
                                        characterCount = rawContent.length,
                                        scanDuration = 0,
                                        imageSource = ImageSource.CAMERA,
                                    ),
                                    scannedAt = clock.now(),
                                    source = QRScanSource.AUTO,
                                )

                                onResult(result)
                            }
                        }
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }

    // endregion

    // region Actions

    override suspend fun getSafeActions(scanResult: QRCodeScanResult): List<QRSafeAction> =
        withContext(dispatcher) {
            val actions = mutableListOf<QRSafeAction>()

            // Copy text is always available
            actions.add(
                QRSafeAction(
                    id = "copy_text",
                    label = "Copy Content",
                    description = "Copy the QR code content to clipboard",
                    actionType = QRActionType.COPY_TEXT,
                    isRecommended = true,
                    requiresConfirmation = false,
                )
            )

            when (scanResult.parsedContent) {
                is QRCodeContent.Url -> {
                    if (scanResult.isSafe) {
                        actions.add(
                            QRSafeAction(
                                id = "open_url",
                                label = "Open URL",
                                description = "Open this URL in your browser",
                                actionType = QRActionType.OPEN_URL,
                                isRecommended = scanResult.isSafe,
                                requiresConfirmation = !scanResult.isSafe,
                            )
                        )
                    }
                }

                is QRCodeContent.Contact -> {
                    actions.add(
                        QRSafeAction(
                            id = "save_contact",
                            label = "Save Contact",
                            description = "Add this contact to your address book",
                            actionType = QRActionType.SAVE_CONTACT,
                            isRecommended = true,
                            requiresConfirmation = false,
                        )
                    )
                }

                is QRCodeContent.Wifi -> {
                    if (scanResult.isSafe) {
                        actions.add(
                            QRSafeAction(
                                id = "connect_wifi",
                                label = "Connect to WiFi",
                                description = "Connect to this wireless network",
                                actionType = QRActionType.CONNECT_WIFI,
                                isRecommended = true,
                                requiresConfirmation = true,
                            )
                        )
                    }
                }

                is QRCodeContent.Phone -> {
                    actions.add(
                        QRSafeAction(
                            id = "dial_phone",
                            label = "Dial Number",
                            description = "Open dialer with this number",
                            actionType = QRActionType.DIAL_PHONE,
                            isRecommended = true,
                            requiresConfirmation = false,
                        )
                    )
                }

                is QRCodeContent.Email -> {
                    actions.add(
                        QRSafeAction(
                            id = "send_email",
                            label = "Compose Email",
                            description = "Open email app with this address",
                            actionType = QRActionType.SEND_EMAIL,
                            isRecommended = true,
                            requiresConfirmation = false,
                        )
                    )
                }

                is QRCodeContent.Sms -> {
                    actions.add(
                        QRSafeAction(
                            id = "send_sms",
                            label = "Send SMS",
                            description = "Open messaging app with this number",
                            actionType = QRActionType.SEND_SMS,
                            isRecommended = true,
                            requiresConfirmation = false,
                        )
                    )
                }

                is QRCodeContent.GeoLocation -> {
                    actions.add(
                        QRSafeAction(
                            id = "open_map",
                            label = "Open in Maps",
                            description = "View this location in your maps app",
                            actionType = QRActionType.OPEN_MAP,
                            isRecommended = true,
                            requiresConfirmation = false,
                        )
                    )
                }

                is QRCodeContent.CalendarEvent -> {
                    actions.add(
                        QRSafeAction(
                            id = "add_calendar",
                            label = "Add to Calendar",
                            description = "Add this event to your calendar",
                            actionType = QRActionType.ADD_CALENDAR,
                            isRecommended = true,
                            requiresConfirmation = false,
                        )
                    )
                }

                is QRCodeContent.AppLink -> {
                    if (scanResult.isSafe) {
                        actions.add(
                            QRSafeAction(
                                id = "open_app",
                                label = "Open App",
                                description = "Open this app link",
                                actionType = QRActionType.OPEN_APP,
                                isRecommended = scanResult.isSafe,
                                requiresConfirmation = true,
                            )
                        )
                    }
                }

                else -> {
                    // Text, Unknown, etc. - copy is already available
                }
            }

            // Report action is always available
            actions.add(
                QRSafeAction(
                    id = "report",
                    label = "Report as Suspicious",
                    description = "Report this QR code for investigation",
                    actionType = QRActionType.REPORT,
                    isRecommended = !scanResult.isSafe,
                    requiresConfirmation = true,
                )
            )

            // Share action is always available
            actions.add(
                QRSafeAction(
                    id = "share",
                    label = "Share",
                    description = "Share this QR code content",
                    actionType = QRActionType.SHARE,
                    isRecommended = false,
                    requiresConfirmation = false,
                )
            )

            actions
        }

    override suspend fun executeAction(
        scanResult: QRCodeScanResult,
        actionType: QRActionType,
    ): Boolean = withContext(dispatcher) {
        try {
            when (actionType) {
                QRActionType.OPEN_URL -> openUrlSafely(
                    (scanResult.parsedContent as? QRCodeContent.Url)?.url ?: scanResult.rawContent
                )

                QRActionType.COPY_TEXT -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("QR Code", scanResult.rawContent)
                    clipboard.setPrimaryClip(clip)
                    true
                }

                QRActionType.SAVE_CONTACT -> saveContact(scanResult)
                QRActionType.CONNECT_WIFI -> connectToWifi(scanResult)
                QRActionType.DIAL_PHONE -> dialPhone(scanResult)
                QRActionType.SEND_SMS -> sendSms(scanResult)
                QRActionType.SEND_EMAIL -> sendEmail(scanResult)
                QRActionType.OPEN_MAP -> openMap(scanResult)
                QRActionType.ADD_CALENDAR -> addToCalendar(scanResult)
                QRActionType.OPEN_APP -> openApp(scanResult)
                QRActionType.REPORT -> reportMaliciousQR(scanResult, "User reported")
                QRActionType.SHARE -> shareScanResult(scanResult)
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun openUrlSafely(url: String): Boolean = withContext(dispatcher) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun saveContact(scanResult: QRCodeScanResult): Boolean =
        withContext(dispatcher) {
            val contact = scanResult.parsedContent as? QRCodeContent.Contact
                ?: return@withContext false

            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                    type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                    putExtra(
                        android.provider.ContactsContract.Intents.Insert.NAME,
                        contact.name
                    )
                    contact.phone?.let {
                        putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, it)
                    }
                    contact.email?.let {
                        putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, it)
                    }
                    contact.organization?.let {
                        putExtra(android.provider.ContactsContract.Intents.Insert.COMPANY, it)
                    }
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun connectToWifi(scanResult: QRCodeScanResult): Boolean =
        withContext(dispatcher) {
            val wifi = scanResult.parsedContent as? QRCodeContent.Wifi
                ?: return@withContext false

            try {
                // Android 10+ requires different approach for WiFi connection
                // Opening WiFi settings is the safest cross-version approach
                val intent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                false
            }
        }

    // endregion

    // region History

    override suspend fun getHistory(
        limit: Int,
        offset: Int,
        contentTypes: List<QRCodeType>?,
    ): List<QRCodeHistoryEntry> = withContext(dispatcher) {
        qrCodeHistoryDao.getHistory(limit, offset, contentTypes)
    }

    override suspend fun getScanResult(scanId: String): QRCodeScanResult? =
        withContext(dispatcher) {
            qrCodeHistoryDao.getScanById(scanId)
        }

    override suspend fun deleteHistoryEntry(scanId: String) = withContext(dispatcher) {
        qrCodeHistoryDao.deleteScan(scanId)
    }

    override suspend fun clearHistory() = withContext(dispatcher) {
        qrCodeHistoryDao.clearAll()
    }

    override fun observeScans(): Flow<QRCodeScanResult> = scanResultsFlow

    // endregion

    // region Statistics

    override suspend fun getStatistics(period: StatisticsPeriod): QRCodeStatistics =
        withContext(dispatcher) {
            qrCodeHistoryDao.getStatistics(period, clock.now())
        }

    // endregion

    // region Reporting

    override suspend fun reportMaliciousQR(
        scanResult: QRCodeScanResult,
        reason: String,
    ): Boolean = withContext(dispatcher) {
        // TODO: Implement actual reporting to backend
        qrCodeHistoryDao.markAsReported(scanResult.id, reason)
        true
    }

    // endregion

    // region Settings

    override suspend fun enableAutoScan() = withContext(dispatcher) {
        qrCodePreferences.setAutoScanEnabled(true)
    }

    override suspend fun disableAutoScan() = withContext(dispatcher) {
        qrCodePreferences.setAutoScanEnabled(false)
    }

    override suspend fun isAutoScanEnabled(): Boolean = withContext(dispatcher) {
        qrCodePreferences.isAutoScanEnabled()
    }

    override suspend fun enableSoundFeedback() = withContext(dispatcher) {
        qrCodePreferences.setSoundFeedbackEnabled(true)
    }

    override suspend fun disableSoundFeedback() = withContext(dispatcher) {
        qrCodePreferences.setSoundFeedbackEnabled(false)
    }

    override suspend fun enableVibrationFeedback() = withContext(dispatcher) {
        qrCodePreferences.setVibrationFeedbackEnabled(true)
    }

    override suspend fun disableVibrationFeedback() = withContext(dispatcher) {
        qrCodePreferences.setVibrationFeedbackEnabled(false)
    }

    // endregion

    // region Private Helpers

    private suspend fun scanImage(image: InputImage): List<Barcode> =
        suspendCancellableCoroutine { continuation ->
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    continuation.resume(barcodes)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

    private fun mapBarcodeFormat(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        Barcode.FORMAT_PDF417 -> "PDF417"
        else -> "UNKNOWN"
    }

    private fun determineImageSource(uri: Uri): ImageSource {
        val path = uri.path ?: return ImageSource.GALLERY
        return when {
            path.contains("screenshot", ignoreCase = true) -> ImageSource.SCREENSHOT
            path.contains("share", ignoreCase = true) -> ImageSource.SHARE
            else -> ImageSource.GALLERY
        }
    }

    private fun dialPhone(scanResult: QRCodeScanResult): Boolean {
        val phone = scanResult.parsedContent as? QRCodeContent.Phone
            ?: return false

        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${phone.number}")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun sendSms(scanResult: QRCodeScanResult): Boolean {
        val sms = scanResult.parsedContent as? QRCodeContent.Sms
            ?: return false

        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${sms.number}")
                sms.message?.let { putExtra("sms_body", it) }
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun sendEmail(scanResult: QRCodeScanResult): Boolean {
        val email = scanResult.parsedContent as? QRCodeContent.Email
            ?: return false

        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${email.address}")
                email.subject?.let { putExtra(android.content.Intent.EXTRA_SUBJECT, it) }
                email.body?.let { putExtra(android.content.Intent.EXTRA_TEXT, it) }
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openMap(scanResult: QRCodeScanResult): Boolean {
        val geo = scanResult.parsedContent as? QRCodeContent.GeoLocation
            ?: return false

        return try {
            val uri = Uri.parse("geo:${geo.latitude},${geo.longitude}")
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun addToCalendar(scanResult: QRCodeScanResult): Boolean {
        val event = scanResult.parsedContent as? QRCodeContent.CalendarEvent
            ?: return false

        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                data = android.provider.CalendarContract.Events.CONTENT_URI
                putExtra(android.provider.CalendarContract.Events.TITLE, event.title)
                event.description?.let {
                    putExtra(android.provider.CalendarContract.Events.DESCRIPTION, it)
                }
                event.location?.let {
                    putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, it)
                }
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openApp(scanResult: QRCodeScanResult): Boolean {
        val appLink = scanResult.parsedContent as? QRCodeContent.AppLink
            ?: return false

        return try {
            val uriBuilder = StringBuilder("${appLink.scheme}://")
            appLink.host?.let { uriBuilder.append(it) }
            appLink.path?.let { uriBuilder.append(it) }

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = Uri.parse(uriBuilder.toString())
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Try fallback URL if available
            appLink.fallbackUrl?.let { fallback ->
                try {
                    val fallbackIntent =
                        android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = Uri.parse(fallback)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    context.startActivity(fallbackIntent)
                    return true
                } catch (e2: Exception) {
                    // Fallback also failed
                }
            }
            false
        }
    }

    private fun shareScanResult(scanResult: QRCodeScanResult): Boolean {
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, scanResult.rawContent)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = android.content.Intent.createChooser(intent, "Share QR Code").apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun MutableSharedFlow<QRCodeScanResult>.subscribeToResults(
        onResult: (QRCodeScanResult) -> Unit
    ): SubscriptionHandle {
        // Simple subscription mechanism for camera scanning
        return SubscriptionHandle { /* cancel mechanism */ }
    }

    private class SubscriptionHandle(private val onCancel: () -> Unit) {
        fun cancel() = onCancel()
    }

    // endregion
}
