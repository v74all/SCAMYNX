package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class QRCodeType {
    @SerialName("url")
    URL,

    @SerialName("text")
    TEXT,

    @SerialName("email")
    EMAIL,

    @SerialName("phone")
    PHONE,

    @SerialName("sms")
    SMS,

    @SerialName("wifi")
    WIFI,

    @SerialName("vcard")
    VCARD,

    @SerialName("mecard")
    MECARD,

    @SerialName("geo")
    GEO,

    @SerialName("calendar")
    CALENDAR,

    @SerialName("crypto")
    CRYPTO,

    @SerialName("upi")
    UPI,

    @SerialName("app_link")
    APP_LINK,

    @SerialName("unknown")
    UNKNOWN,
}


@Serializable
data class QRCodeScanResult(
    val id: String,
    val rawContent: String,
    val contentType: QRCodeType,
    val parsedContent: QRCodeContent,
    val isSafe: Boolean,
    val threatAssessment: QRThreatAssessment? = null,
    val metadata: QRCodeMetadata,
    val scannedAt: Instant,
    val source: QRScanSource,
)

@Serializable
sealed class QRCodeContent {
    @Serializable
    @SerialName("url")
    data class Url(
        val url: String,
        val domain: String,
        val isHttps: Boolean,
        val redirectsTo: String? = null,
    ) : QRCodeContent()

    @Serializable
    @SerialName("text")
    data class Text(
        val text: String,
        val containsUrls: Boolean = false,
        val extractedUrls: List<String> = emptyList(),
    ) : QRCodeContent()

    @Serializable
    @SerialName("email")
    data class Email(
        val address: String,
        val subject: String? = null,
        val body: String? = null,
    ) : QRCodeContent()

    @Serializable
    @SerialName("phone")
    data class Phone(
        val number: String,
        val countryCode: String? = null,
    ) : QRCodeContent()

    @Serializable
    @SerialName("sms")
    data class Sms(
        val number: String,
        val message: String? = null,
    ) : QRCodeContent()

    @Serializable
    @SerialName("wifi")
    data class Wifi(
        val ssid: String,
        val password: String? = null,
        val securityType: WifiSecurityType,
        val isHidden: Boolean = false,
    ) : QRCodeContent()

    @Serializable
    @SerialName("contact")
    data class Contact(
        val name: String? = null,
        val phone: String? = null,
        val email: String? = null,
        val organization: String? = null,
        val address: String? = null,
        val website: String? = null,
    ) : QRCodeContent()

    @Serializable
    @SerialName("geo")
    data class GeoLocation(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double? = null,
        val label: String? = null,
    ) : QRCodeContent()

    @Serializable
    @SerialName("calendar")
    data class CalendarEvent(
        val title: String,
        val startDate: String? = null,
        val endDate: String? = null,
        val location: String? = null,
        val description: String? = null,
    ) : QRCodeContent()

    @Serializable
    @SerialName("crypto")
    data class CryptoPayment(
        val currency: String,
        val address: String,
        val amount: String? = null,
        val label: String? = null,
        val message: String? = null,
    ) : QRCodeContent()

    @Serializable
    @SerialName("upi")
    data class UpiPayment(
        val payeeAddress: String,
        val payeeName: String? = null,
        val amount: String? = null,
        val transactionNote: String? = null,
        val merchantCode: String? = null,
    ) : QRCodeContent()

    @Serializable
    @SerialName("app_link")
    data class AppLink(
        val scheme: String,
        val host: String? = null,
        val path: String? = null,
        val packageName: String? = null,
        val fallbackUrl: String? = null,
    ) : QRCodeContent()

    @Serializable
    @SerialName("unknown")
    data class Unknown(
        val raw: String,
    ) : QRCodeContent()
}


@Serializable
data class QRThreatAssessment(
    val riskLevel: RiskCategory,
    val riskScore: Double,
    val threats: List<QRThreat>,
    val warnings: List<QRWarning>,
    val recommendations: List<String>,
    val shouldBlock: Boolean,
    val confidence: ConfidenceLevel,
)

@Serializable
data class QRThreat(
    val type: QRThreatType,
    val severity: IssueSeverity,
    val description: String,
    val evidence: String? = null,
)

@Serializable
enum class QRThreatType {
    @SerialName("phishing_url")
    PHISHING_URL,

    @SerialName("malware_url")
    MALWARE_URL,

    @SerialName("scam_payment")
    SCAM_PAYMENT,

    @SerialName("suspicious_redirect")
    SUSPICIOUS_REDIRECT,

    @SerialName("url_shortener")
    URL_SHORTENER,

    @SerialName("typosquatting")
    TYPOSQUATTING,

    @SerialName("homograph_attack")
    HOMOGRAPH_ATTACK,

    @SerialName("data_harvesting")
    DATA_HARVESTING,

    @SerialName("malicious_app_link")
    MALICIOUS_APP_LINK,

    @SerialName("fake_wifi")
    FAKE_WIFI,

    @SerialName("premium_sms")
    PREMIUM_SMS,

    @SerialName("crypto_scam")
    CRYPTO_SCAM,

    @SerialName("suspicious_contact")
    SUSPICIOUS_CONTACT,
}

@Serializable
data class QRWarning(
    val type: QRWarningType,
    val message: String,
)

@Serializable
enum class QRWarningType {
    @SerialName("new_domain")
    NEW_DOMAIN,

    @SerialName("no_https")
    NO_HTTPS,

    @SerialName("unknown_sender")
    UNKNOWN_SENDER,

    @SerialName("hidden_redirect")
    HIDDEN_REDIRECT,

    @SerialName("long_url")
    LONG_URL,

    @SerialName("ip_address_url")
    IP_ADDRESS_URL,

    @SerialName("suspicious_tld")
    SUSPICIOUS_TLD,

    @SerialName("open_wifi")
    OPEN_WIFI,

    @SerialName("high_amount")
    HIGH_AMOUNT,

    @SerialName("external_app")
    EXTERNAL_APP,
}


@Serializable
data class QRCodeMetadata(
    val format: String,
    val errorCorrectionLevel: String? = null,
    val version: Int? = null,
    val characterCount: Int,
    val scanDuration: Long,
    val imageSource: ImageSource? = null,
)

@Serializable
enum class ImageSource {
    @SerialName("camera")
    CAMERA,

    @SerialName("gallery")
    GALLERY,

    @SerialName("screenshot")
    SCREENSHOT,

    @SerialName("share")
    SHARE,
}

@Serializable
enum class QRScanSource {
    @SerialName("manual")
    MANUAL,

    @SerialName("auto")
    AUTO,

    @SerialName("share")
    SHARE,

    @SerialName("notification")
    NOTIFICATION,
}


@Serializable
data class QRCodeHistoryEntry(
    val id: String,
    val contentType: QRCodeType,
    val displayLabel: String,
    val riskLevel: RiskCategory,
    val wasBlocked: Boolean,
    val scannedAt: Instant,
    val source: QRScanSource,
)

@Serializable
data class QRCodeStatistics(
    val totalScans: Int,
    val safeScans: Int,
    val blockedScans: Int,
    val scansByType: Map<QRCodeType, Int>,
    val threatsByType: Map<QRThreatType, Int>,
    val period: StatisticsPeriod,
    val generatedAt: Instant,
)


@Serializable
data class QRSafeAction(
    val id: String,
    val label: String,
    val description: String,
    val actionType: QRActionType,
    val isRecommended: Boolean,
    val requiresConfirmation: Boolean,
)

@Serializable
enum class QRActionType {
    @SerialName("open_url")
    OPEN_URL,

    @SerialName("copy_text")
    COPY_TEXT,

    @SerialName("save_contact")
    SAVE_CONTACT,

    @SerialName("connect_wifi")
    CONNECT_WIFI,

    @SerialName("dial_phone")
    DIAL_PHONE,

    @SerialName("send_sms")
    SEND_SMS,

    @SerialName("send_email")
    SEND_EMAIL,

    @SerialName("open_map")
    OPEN_MAP,

    @SerialName("add_calendar")
    ADD_CALENDAR,

    @SerialName("open_app")
    OPEN_APP,

    @SerialName("report")
    REPORT,

    @SerialName("share")
    SHARE,
}
