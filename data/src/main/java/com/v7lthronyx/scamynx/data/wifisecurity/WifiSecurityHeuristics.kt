package com.v7lthronyx.scamynx.data.wifisecurity

import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.WifiEncryptionType
import com.v7lthronyx.scamynx.domain.model.WifiSecurityAssessment
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.min

internal data class WifiNetworkSnapshot(
    val ssid: String?,
    val bssid: String?,
    val encryptionType: WifiEncryptionType,
    val captivePortalSuspected: Boolean,
    val signalLevelDbm: Int?,
    val linkSpeedMbps: Int?,
    val isMetered: Boolean,
    val arpIndicators: List<String>,
)

internal fun evaluateWifiSnapshot(
    snapshot: WifiNetworkSnapshot,
    timestamp: Instant = Clock.System.now(),
): WifiSecurityAssessment {
    var score = baseScoreForEncryption(snapshot.encryptionType)
    val recommendations = mutableListOf<String>()

    if (snapshot.encryptionType == WifiEncryptionType.OPEN || snapshot.encryptionType == WifiEncryptionType.WEP) {
        recommendations += "اجتناب از ورود به حساب‌های حساس روی این شبکه"
        recommendations += "روشن کردن VPN برای رمزگذاری ترافیک"
    }

    if (snapshot.captivePortalSuspected) {
        score += 0.1
        recommendations += "قبل از وارد کردن اطلاعات حساس از معتبر بودن پورتال اطمینان حاصل کنید"
    }

    if (snapshot.signalLevelDbm != null && snapshot.signalLevelDbm < -75) {
        score += 0.05
        recommendations += "قدرت سیگنال ضعیف است؛ ممکن است نقطه دسترسی دور یا دارای تداخل باشد"
    }

    if (snapshot.linkSpeedMbps != null && snapshot.linkSpeedMbps < 10) {
        score += 0.05
    }

    if (snapshot.isMetered) {
        score += 0.05
    }

    if (snapshot.arpIndicators.isNotEmpty()) {
        score += 0.25
        recommendations += "نشانه‌های حمله ARP/MITM مشاهده شد؛ اتصال را قطع و شبکه را فراموش کنید"
    }

    val finalScore = min(score, 1.0)
    val category = when {
        finalScore >= 0.7 -> RiskCategory.HIGH
        finalScore >= 0.45 -> RiskCategory.MEDIUM
        else -> RiskCategory.LOW
    }

    return WifiSecurityAssessment(
        ssid = snapshot.ssid,
        bssid = snapshot.bssid,
        encryptionType = snapshot.encryptionType,
        captivePortalSuspected = snapshot.captivePortalSuspected,
        arpMitmIndicators = snapshot.arpIndicators,
        recommendations = recommendations.distinct(),
        riskScore = finalScore,
        riskCategory = category,
        timestamp = timestamp,
    )
}

private fun baseScoreForEncryption(type: WifiEncryptionType): Double = when (type) {
    WifiEncryptionType.OPEN -> 0.8
    WifiEncryptionType.WEP -> 0.7
    WifiEncryptionType.WPA -> 0.55
    WifiEncryptionType.WPA2 -> 0.35
    WifiEncryptionType.WPA3 -> 0.15
    WifiEncryptionType.UNKNOWN -> 0.45
}
