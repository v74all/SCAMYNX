package com.v7lthronyx.scamynx.ui.networkmonitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class NetworkMonitorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(NetworkMonitorUiState())
    val state: StateFlow<NetworkMonitorUiState> = _state.asStateFlow()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            checkNetworkStatus()
            fetchPublicIp()
            measureLatency()
        }
    }

    private fun checkNetworkStatus() {
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }

        val (connectionType, networkName) = when {
            capabilities == null -> ConnectionType.DISCONNECTED to null
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                @Suppress("DEPRECATION")
                val wifiInfo = wifiManager?.connectionInfo
                val ssid = wifiInfo?.ssid?.replace("\"", "") ?: "Unknown WiFi"
                ConnectionType.WIFI to ssid
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                ConnectionType.CELLULAR to "Mobile Data"
            }
            else -> ConnectionType.DISCONNECTED to null
        }

        val securityScore = calculateSecurityScore(capabilities)

        _state.update { current ->
            current.copy(
                connectionType = connectionType,
                networkName = networkName,
                securityScore = securityScore,
                gateway = getGatewayAddress(),
                dnsServer = getDnsServer(),
            )
        }

        fetchPublicIp()
    }

    private fun calculateSecurityScore(capabilities: NetworkCapabilities?): Int {
        if (capabilities == null) return 0

        var score = 50

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            score += 30
        }

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            
            score += 10
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            
            score += 20
        }

        if (_state.value.dnsProtectionEnabled) {
            score += 20
        }

        return score.coerceIn(0, 100)
    }

    private fun getGatewayAddress(): String? {
        return try {
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager?.dhcpInfo
            wifiInfo?.gateway?.let { formatIpAddress(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun getDnsServer(): String? {
        return try {
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager?.dhcpInfo
            wifiInfo?.dns1?.let { formatIpAddress(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatIpAddress(ip: Int): String {
        return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
    }

    private fun fetchPublicIp() {
        viewModelScope.launch {
            try {
                val publicIp = withContext(Dispatchers.IO) {
                    fetchIpFromService("https://api.ipify.org")
                        ?: fetchIpFromService("https://api.my-ip.io/ip")
                        ?: fetchIpFromService("https://icanhazip.com")
                }
                _state.update { it.copy(publicIp = publicIp ?: "Unavailable") }
            } catch (e: Exception) {
                _state.update { it.copy(publicIp = "Unavailable") }
            }
        }
    }

    private fun fetchIpFromService(urlString: String): String? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readLine()?.trim()
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun measureLatency() {
        viewModelScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val address = InetAddress.getByName("8.8.8.8")
                val reachable = address.isReachable(3000)
                val latency = if (reachable) {
                    (System.currentTimeMillis() - startTime).toInt()
                } else {
                    -1
                }

                _state.update { it.copy(latencyMs = latency) }
            } catch (e: Exception) {
                _state.update { it.copy(latencyMs = -1) }
            }
        }
    }

    fun toggleDnsProtection() {
        viewModelScope.launch {
            val newState = !_state.value.dnsProtectionEnabled
            _state.update { current ->
                current.copy(
                    dnsProtectionEnabled = newState,
                    securityScore = if (newState) {
                        (current.securityScore + 20).coerceAtMost(100)
                    } else {
                        (current.securityScore - 20).coerceAtLeast(0)
                    },
                )
            }

        }
    }
}
