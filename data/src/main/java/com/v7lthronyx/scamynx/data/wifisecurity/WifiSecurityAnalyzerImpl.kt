package com.v7lthronyx.scamynx.data.wifisecurity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.domain.model.WifiEncryptionType
import com.v7lthronyx.scamynx.domain.model.WifiSecurityAssessment
import com.v7lthronyx.scamynx.domain.service.WifiSecurityAnalyzer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiSecurityAnalyzerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : WifiSecurityAnalyzer {

    private val wifiManager: WifiManager? = ContextCompat.getSystemService(context, WifiManager::class.java)
    private val connectivityManager: ConnectivityManager? = ContextCompat.getSystemService(
        context,
        ConnectivityManager::class.java,
    )

    override suspend fun analyzeCurrentNetwork(): WifiSecurityAssessment? = withContext(dispatcher) {
        val cm = connectivityManager ?: return@withContext null

        @Suppress("DEPRECATION")
        val wifiInfo = wifiManager?.connectionInfo ?: return@withContext null
        if (wifiInfo.networkId == INVALID_NETWORK_ID) return@withContext null
        val activeNetwork: Network = cm.activeNetwork ?: return@withContext null
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return@withContext null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return@withContext null

        val ssid = wifiInfo.ssid?.trim('"')
        val bssid = wifiInfo.bssid
        val signalLevel = wifiInfo.rssi.takeIf { it != INVALID_RSSI }
        val linkSpeed = wifiInfo.linkSpeed.takeIf { it > 0 }
        val isMetered = cm.isActiveNetworkMetered
        val captivePortal = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) ||
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val encryptionType = resolveEncryptionType(wifiInfo)
        val arpFindings = detectArpAnomalies()

        val snapshot = WifiNetworkSnapshot(
            ssid = ssid,
            bssid = bssid,
            encryptionType = encryptionType,
            captivePortalSuspected = captivePortal,
            signalLevelDbm = signalLevel,
            linkSpeedMbps = linkSpeed,
            isMetered = isMetered,
            arpIndicators = arpFindings,
        )
        evaluateWifiSnapshot(snapshot)
    }

    private fun resolveEncryptionType(wifiInfo: WifiInfo): WifiEncryptionType {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return when (wifiInfo.currentSecurityType) {
                WifiInfo.SECURITY_TYPE_OPEN -> WifiEncryptionType.OPEN
                WifiInfo.SECURITY_TYPE_WEP -> WifiEncryptionType.WEP
                WifiInfo.SECURITY_TYPE_PSK,
                WifiInfo.SECURITY_TYPE_EAP,
                WifiInfo.SECURITY_TYPE_PASSPOINT_R1_R2,
                -> WifiEncryptionType.WPA2
                WifiInfo.SECURITY_TYPE_SAE,
                WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE,
                -> WifiEncryptionType.WPA3
                WifiInfo.SECURITY_TYPE_OWE -> WifiEncryptionType.OPEN
                else -> WifiEncryptionType.UNKNOWN
            }
        }
        @Suppress("DEPRECATION")
        return when {
            wifiInfo.ssid?.contains("open", ignoreCase = true) == true -> WifiEncryptionType.OPEN
            wifiInfo.ssid?.contains("wpa3", ignoreCase = true) == true -> WifiEncryptionType.WPA3
            else -> WifiEncryptionType.UNKNOWN
        }
    }

    private fun detectArpAnomalies(): List<String> {
        val arpFile = File("/proc/net/arp")
        if (!arpFile.exists()) return emptyList()
        return runCatching {
            parseArpTable(arpFile).groupBy { it.ipAddress }
                .mapNotNull { (ip, entries) ->
                    val macs = entries.map { it.macAddress }.distinct()
                    when {
                        entries.any { it.macAddress == "00:00:00:00:00:00" } ->
                            "نشانگر MAC نامعتبر برای IP $ip"
                        macs.size > 1 ->
                            "چندین دستگاه با IP مشترک ($ip) مشاهده شد"
                        else -> null
                    }
                }
        }.getOrDefault(emptyList())
    }

    private fun parseArpTable(file: File): List<ArpEntry> {
        if (!file.canRead()) return emptyList()
        val result = mutableListOf<ArpEntry>()
        BufferedReader(FileReader(file)).use { reader ->
            reader.lineSequence()
                .drop(1)
                .forEach { line ->
                    val cols = line.split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (cols.size >= 6) {
                        result += ArpEntry(
                            ipAddress = cols[0],
                            hwType = cols[1],
                            flags = cols[2],
                            macAddress = cols[3].lowercase(),
                            mask = cols[4],
                            device = cols[5],
                        )
                    }
                }
        }
        return result
    }

    private data class ArpEntry(
        val ipAddress: String,
        val hwType: String,
        val flags: String,
        val macAddress: String,
        val mask: String,
        val device: String,
    )
    companion object {
        private const val INVALID_NETWORK_ID = -1
        private const val INVALID_RSSI = -127
    }
}
