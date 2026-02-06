package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WifiEncryptionType {
    @SerialName("open")
    OPEN,

    @SerialName("wep")
    WEP,

    @SerialName("wpa")
    WPA,

    @SerialName("wpa2")
    WPA2,

    @SerialName("wpa3")
    WPA3,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
data class WifiSecurityAssessment(
    val ssid: String?,
    val bssid: String?,
    val encryptionType: WifiEncryptionType,
    val captivePortalSuspected: Boolean,
    val arpMitmIndicators: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val riskScore: Double,
    val riskCategory: RiskCategory,
    val timestamp: Instant,
)
