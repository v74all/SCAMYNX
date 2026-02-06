package com.v7lthronyx.scamynx.data.wifisecurity

import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.WifiEncryptionType
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiSecurityHeuristicsTest {

    @Test
    fun `open network triggers high risk`() {
        val snapshot = WifiNetworkSnapshot(
            ssid = "Cafe WiFi",
            bssid = "00:11:22:33:44:55",
            encryptionType = WifiEncryptionType.OPEN,
            captivePortalSuspected = true,
            signalLevelDbm = -80,
            linkSpeedMbps = 5,
            isMetered = true,
            arpIndicators = emptyList(),
        )
        val assessment = evaluateWifiSnapshot(snapshot, Instant.DISTANT_PAST)
        assertEquals(RiskCategory.HIGH, assessment.riskCategory)
        assertTrue(assessment.riskScore >= 0.7)
    }

    @Test
    fun `wpa3 secure network remains low risk`() {
        val snapshot = WifiNetworkSnapshot(
            ssid = "Home",
            bssid = "aa:bb:cc:dd:ee:ff",
            encryptionType = WifiEncryptionType.WPA3,
            captivePortalSuspected = false,
            signalLevelDbm = -55,
            linkSpeedMbps = 150,
            isMetered = false,
            arpIndicators = emptyList(),
        )
        val assessment = evaluateWifiSnapshot(snapshot, Instant.DISTANT_PAST)
        assertEquals(RiskCategory.LOW, assessment.riskCategory)
        assertTrue(assessment.riskScore < 0.45)
    }

    @Test
    fun `arp indicators escalate recommendations`() {
        val snapshot = WifiNetworkSnapshot(
            ssid = "Airport",
            bssid = "ff:ee:dd:cc:bb:aa",
            encryptionType = WifiEncryptionType.WPA2,
            captivePortalSuspected = false,
            signalLevelDbm = -60,
            linkSpeedMbps = 20,
            isMetered = false,
            arpIndicators = listOf("duplicate"),
        )
        val assessment = evaluateWifiSnapshot(snapshot, Instant.DISTANT_PAST)
        assertEquals(RiskCategory.MEDIUM, assessment.riskCategory)
        assertTrue(assessment.recommendations.isNotEmpty())
    }
}
