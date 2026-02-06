package com.v7lthronyx.scamynx.data.networkmonitor

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.util.Log
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.di.ThreatIntelJson
import com.v7lthronyx.scamynx.domain.model.ActiveConnection
import com.v7lthronyx.scamynx.domain.model.AnomalyType
import com.v7lthronyx.scamynx.domain.model.AppNetworkUsage
import com.v7lthronyx.scamynx.domain.model.AppTrustLevel
import com.v7lthronyx.scamynx.domain.model.ConnectionDirection
import com.v7lthronyx.scamynx.domain.model.ConnectionState
import com.v7lthronyx.scamynx.domain.model.DataUsage
import com.v7lthronyx.scamynx.domain.model.DnsQuery
import com.v7lthronyx.scamynx.domain.model.DnsQueryType
import com.v7lthronyx.scamynx.domain.model.DnsResponseCode
import com.v7lthronyx.scamynx.domain.model.DnsStatistics
import com.v7lthronyx.scamynx.domain.model.DomainStats
import com.v7lthronyx.scamynx.domain.model.FirewallAction
import com.v7lthronyx.scamynx.domain.model.FirewallConfig
import com.v7lthronyx.scamynx.domain.model.FirewallRule
import com.v7lthronyx.scamynx.domain.model.HostCategory
import com.v7lthronyx.scamynx.domain.model.HostUsage
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.NetworkAlert
import com.v7lthronyx.scamynx.domain.model.NetworkAlertAction
import com.v7lthronyx.scamynx.domain.model.NetworkAlertType
import com.v7lthronyx.scamynx.domain.model.NetworkAnomaly
import com.v7lthronyx.scamynx.domain.model.NetworkInsights
import com.v7lthronyx.scamynx.domain.model.NetworkMonitorState
import com.v7lthronyx.scamynx.domain.model.NetworkMonitorStatus
import com.v7lthronyx.scamynx.domain.model.NetworkProtocol
import com.v7lthronyx.scamynx.domain.model.NetworkRecommendation
import com.v7lthronyx.scamynx.domain.model.PrivacyConcern
import com.v7lthronyx.scamynx.domain.model.PrivacyConcernType
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.StatisticsPeriod
import com.v7lthronyx.scamynx.domain.service.NetworkMonitorService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NetworkMonitorService for network traffic monitoring.
 * Provides network connection monitoring, DNS query tracking, and firewall functionality.
 */
@Singleton
class NetworkMonitorServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ThreatIntelJson private val json: Json,
) : NetworkMonitorService {

    companion object {
        private const val TAG = "NetworkMonitorService"
        private const val PROC_NET_TCP = "/proc/net/tcp"
        private const val PROC_NET_TCP6 = "/proc/net/tcp6"
        private const val PROC_NET_UDP = "/proc/net/udp"
        private const val PROC_NET_UDP6 = "/proc/net/udp6"
    }

    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val isMonitoring = AtomicBoolean(false)
    private val isVpnMode = AtomicBoolean(false)

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val packageManager = context.packageManager

    // State flows
    private val _monitorState = MutableStateFlow(createInitialState())
    override val monitorState: StateFlow<NetworkMonitorState> = _monitorState.asStateFlow()

    // Event flows
    private val _connectionFlow = MutableSharedFlow<ActiveConnection>(replay = 0, extraBufferCapacity = 100)
    private val _suspiciousConnectionFlow = MutableSharedFlow<ActiveConnection>(replay = 0, extraBufferCapacity = 50)
    private val _dnsQueryFlow = MutableSharedFlow<DnsQuery>(replay = 0, extraBufferCapacity = 100)
    private val _alertFlow = MutableSharedFlow<NetworkAlert>(replay = 0, extraBufferCapacity = 50)

    // In-memory storage
    private val activeConnections = mutableListOf<ActiveConnection>()
    private val dnsQueries = mutableListOf<DnsQuery>()
    private val alerts = mutableListOf<NetworkAlert>()
    private val blockedDomains = mutableSetOf<String>()
    private val blockedApps = mutableSetOf<String>()
    private val firewallRules = mutableListOf<FirewallRule>()
    private var firewallConfig = createDefaultFirewallConfig()

    private fun createInitialState(): NetworkMonitorState {
        return NetworkMonitorState(
            status = NetworkMonitorStatus.DISABLED,
            isVpnModeActive = false,
            connectionsMonitored = 0,
            suspiciousConnections = 0,
            blockedConnections = 0,
            dataUsageToday = createEmptyDataUsage(),
            activeConnections = emptyList(),
            recentAlerts = emptyList(),
            topApps = emptyList(),
            lastUpdated = Clock.System.now(),
        )
    }

    private fun createEmptyDataUsage(): DataUsage {
        return DataUsage(
            sent = 0,
            received = 0,
            total = 0,
            formattedSent = "0 B",
            formattedReceived = "0 B",
            formattedTotal = "0 B",
        )
    }

    private fun createDefaultFirewallConfig(): FirewallConfig {
        return FirewallConfig(
            isEnabled = false,
            defaultAction = FirewallAction.ALLOW,
            blockOnCellular = false,
            blockOnRoaming = true,
            allowSystemApps = true,
            blockAds = false,
            blockTrackers = false,
            blockMalware = true,
            customRules = emptyList(),
            whitelistedApps = emptyList(),
            blacklistedApps = emptyList(),
        )
    }

    // ==================== Monitoring Control ====================

    override suspend fun getStatus(): NetworkMonitorStatus {
        return _monitorState.value.status
    }

    override suspend fun startMonitoring(useVpn: Boolean) {
        if (isMonitoring.getAndSet(true)) {
            Log.d(TAG, "Monitoring already active")
            return
        }

        isVpnMode.set(useVpn)
        Log.i(TAG, "Starting network monitoring (VPN mode: $useVpn)")

        _monitorState.update { state ->
            state.copy(
                status = NetworkMonitorStatus.ACTIVE,
                isVpnModeActive = useVpn,
                lastUpdated = Clock.System.now(),
            )
        }

        // Start monitoring loop
        scope.launch {
            while (isMonitoring.get()) {
                try {
                    refreshNetworkState()
                    kotlinx.coroutines.delay(5000) // Update every 5 seconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error in monitoring loop", e)
                }
            }
        }
    }

    override suspend fun stopMonitoring() {
        if (!isMonitoring.getAndSet(false)) {
            Log.d(TAG, "Monitoring not active")
            return
        }

        Log.i(TAG, "Stopping network monitoring")

        _monitorState.update { state ->
            state.copy(
                status = NetworkMonitorStatus.DISABLED,
                isVpnModeActive = false,
                lastUpdated = Clock.System.now(),
            )
        }
    }

    override fun isMonitoringActive(): Boolean = isMonitoring.get()

    private suspend fun refreshNetworkState() = withContext(ioDispatcher) {
        try {
            val connections = readActiveConnections()
            activeConnections.clear()
            activeConnections.addAll(connections)

            val suspiciousCount = connections.count { it.isSuspicious }
            val blockedCount = connections.count { isConnectionBlocked(it) }

            // Emit new connections
            connections.forEach { connection ->
                _connectionFlow.emit(connection)
                if (connection.isSuspicious) {
                    _suspiciousConnectionFlow.emit(connection)
                }
            }

            // Calculate data usage
            val dataUsage = calculateDataUsage()

            // Get top apps
            val topApps = getTopAppsByUsage(5, StatisticsPeriod.TODAY)

            _monitorState.update { state ->
                state.copy(
                    connectionsMonitored = connections.size,
                    suspiciousConnections = suspiciousCount,
                    blockedConnections = blockedCount,
                    dataUsageToday = dataUsage,
                    activeConnections = connections.take(20),
                    topApps = topApps,
                    recentAlerts = alerts.take(10),
                    lastUpdated = Clock.System.now(),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh network state", e)
        }
    }

    // ==================== Connection Management ====================

    override suspend fun getActiveConnections(): List<ActiveConnection> = withContext(ioDispatcher) {
        readActiveConnections()
    }

    override suspend fun getConnectionsForApp(packageName: String): List<ActiveConnection> = withContext(ioDispatcher) {
        readActiveConnections().filter { it.packageName == packageName }
    }

    override fun observeConnections(): Flow<ActiveConnection> = _connectionFlow.asSharedFlow()

    override fun observeSuspiciousConnections(): Flow<ActiveConnection> = _suspiciousConnectionFlow.asSharedFlow()

    override suspend fun blockConnection(connectionId: String) = withContext(ioDispatcher) {
        val connection = activeConnections.find { it.id == connectionId }
        if (connection != null) {
            blockedDomains.add(connection.remoteDomain ?: connection.remoteAddress)
            createAlert(
                type = NetworkAlertType.SUSPICIOUS_CONNECTION,
                severity = IssueSeverity.HIGH,
                title = "Connection Blocked",
                description = "Blocked connection to ${connection.remoteDomain ?: connection.remoteAddress}",
                packageName = connection.packageName,
                appName = connection.appName,
                remoteHost = connection.remoteDomain,
                remoteIp = connection.remoteAddress,
                action = NetworkAlertAction.BLOCKED,
            )
        }
    }

    private fun readActiveConnections(): List<ActiveConnection> {
        val connections = mutableListOf<ActiveConnection>()
        val now = Clock.System.now()

        // Read TCP connections
        connections.addAll(parseConnectionsFromProc(PROC_NET_TCP, NetworkProtocol.TCP))
        connections.addAll(parseConnectionsFromProc(PROC_NET_TCP6, NetworkProtocol.TCP))

        // Read UDP connections
        connections.addAll(parseConnectionsFromProc(PROC_NET_UDP, NetworkProtocol.UDP))
        connections.addAll(parseConnectionsFromProc(PROC_NET_UDP6, NetworkProtocol.UDP))

        return connections
    }

    private fun parseConnectionsFromProc(path: String, protocol: NetworkProtocol): List<ActiveConnection> {
        val connections = mutableListOf<ActiveConnection>()
        val now = Clock.System.now()

        try {
            val process = Runtime.getRuntime().exec("cat $path")
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readLine() // Skip header

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    try {
                        val parts = line!!.trim().split("\\s+".toRegex())
                        if (parts.size < 10) continue

                        val localAddress = parseHexAddress(parts[1])
                        val remoteAddress = parseHexAddress(parts[2])
                        val state = parseConnectionState(parts[3])
                        val uid = parts[7].toIntOrNull() ?: continue

                        if (remoteAddress.first == "0.0.0.0" || remoteAddress.first == "127.0.0.1") continue

                        val packageName = getPackageNameFromUid(uid)
                        val appName = getAppName(packageName)
                        val isSuspicious = checkIfSuspicious(remoteAddress.first, remoteAddress.second)

                        connections.add(
                            ActiveConnection(
                                id = UUID.randomUUID().toString(),
                                packageName = packageName,
                                appName = appName,
                                protocol = protocol,
                                localAddress = localAddress.first,
                                localPort = localAddress.second,
                                remoteAddress = remoteAddress.first,
                                remotePort = remoteAddress.second,
                                remoteDomain = null,
                                state = state,
                                direction = ConnectionDirection.OUTBOUND,
                                bytesSent = 0,
                                bytesReceived = 0,
                                startTime = now,
                                isSuspicious = isSuspicious,
                                threatLevel = if (isSuspicious) RiskCategory.MEDIUM else RiskCategory.MINIMAL,
                            ),
                        )
                    } catch (e: Exception) {
                        // Skip malformed lines
                    }
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read $path", e)
        }

        return connections
    }

    private fun parseHexAddress(hexAddr: String): Pair<String, Int> {
        return try {
            val parts = hexAddr.split(":")
            val ip = parts[0].chunked(2).reversed().joinToString(".") { it.toInt(16).toString() }
            val port = parts[1].toInt(16)
            ip to port
        } catch (e: Exception) {
            "0.0.0.0" to 0
        }
    }

    private fun parseConnectionState(hexState: String): ConnectionState {
        return when (hexState.uppercase()) {
            "01" -> ConnectionState.ESTABLISHED
            "02" -> ConnectionState.SYN_SENT
            "03" -> ConnectionState.SYN_RECEIVED
            "04" -> ConnectionState.FIN_WAIT
            "05" -> ConnectionState.FIN_WAIT
            "06" -> ConnectionState.TIME_WAIT
            "07" -> ConnectionState.CLOSED
            "08" -> ConnectionState.CLOSE_WAIT
            "0A" -> ConnectionState.LISTEN
            else -> ConnectionState.ESTABLISHED
        }
    }

    private fun getPackageNameFromUid(uid: Int): String {
        return try {
            val packages = packageManager.getPackagesForUid(uid)
            packages?.firstOrNull() ?: "uid:$uid"
        } catch (e: Exception) {
            "uid:$uid"
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
    }

    private fun checkIfSuspicious(ip: String, port: Int): Boolean {
        // Check for suspicious ports
        val suspiciousPorts = setOf(4444, 5555, 6666, 1337, 31337, 12345)
        if (port in suspiciousPorts) return true

        // Check for known malicious IP ranges (simplified example)
        // In production, this would check against a threat feed
        if (blockedDomains.contains(ip)) return true

        return false
    }

    private fun isConnectionBlocked(connection: ActiveConnection): Boolean {
        if (blockedDomains.contains(connection.remoteAddress)) return true
        if (blockedDomains.contains(connection.remoteDomain)) return true
        if (blockedApps.contains(connection.packageName)) return true
        return false
    }

    // ==================== App Network Usage ====================

    override suspend fun getAppNetworkUsage(period: StatisticsPeriod): List<AppNetworkUsage> = withContext(ioDispatcher) {
        val usageList = mutableListOf<AppNetworkUsage>()
        val now = Clock.System.now()

        try {
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            for (appInfo in packages.take(50)) {
                try {
                    val uid = appInfo.uid
                    val rxBytes = TrafficStats.getUidRxBytes(uid)
                    val txBytes = TrafficStats.getUidTxBytes(uid)

                    if (rxBytes > 0 || txBytes > 0) {
                        val total = rxBytes + txBytes
                        val appName = packageManager.getApplicationLabel(appInfo).toString()

                        usageList.add(
                            AppNetworkUsage(
                                packageName = appInfo.packageName,
                                appName = appName,
                                dataUsage = DataUsage(
                                    sent = txBytes,
                                    received = rxBytes,
                                    total = total,
                                    formattedSent = formatBytes(txBytes),
                                    formattedReceived = formatBytes(rxBytes),
                                    formattedTotal = formatBytes(total),
                                ),
                                connectionCount = 0,
                                suspiciousConnections = 0,
                                blockedConnections = 0,
                                uniqueHosts = 0,
                                topHosts = emptyList(),
                                lastActivity = now,
                                trustLevel = determineTrustLevel(appInfo.packageName),
                            ),
                        )
                    }
                } catch (e: Exception) {
                    // Skip apps with issues
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get app network usage", e)
        }

        usageList.sortedByDescending { it.dataUsage.total }
    }

    override suspend fun getAppUsage(packageName: String, period: StatisticsPeriod): AppNetworkUsage? =
        getAppNetworkUsage(period).find { it.packageName == packageName }

    override suspend fun getTopAppsByUsage(limit: Int, period: StatisticsPeriod): List<AppNetworkUsage> =
        getAppNetworkUsage(period).take(limit)

    private fun determineTrustLevel(packageName: String): AppTrustLevel {
        return when {
            packageName.startsWith("com.android.") -> AppTrustLevel.SYSTEM
            packageName.startsWith("com.google.") -> AppTrustLevel.VERIFIED
            blockedApps.contains(packageName) -> AppTrustLevel.BLOCKED
            else -> AppTrustLevel.NEUTRAL
        }
    }

    // ==================== DNS Management ====================

    override suspend fun getDnsQueries(limit: Int, packageName: String?): List<DnsQuery> = withContext(ioDispatcher) {
        val filtered = if (packageName != null) {
            dnsQueries.filter { it.packageName == packageName }
        } else {
            dnsQueries
        }
        filtered.takeLast(limit).reversed()
    }

    override fun observeDnsQueries(): Flow<DnsQuery> = _dnsQueryFlow.asSharedFlow()

    override suspend fun getDnsStatistics(period: StatisticsPeriod): DnsStatistics = withContext(ioDispatcher) {
        val now = Clock.System.now()
        val totalQueries = dnsQueries.size
        val blockedQueries = dnsQueries.count { it.isBlocked }
        val uniqueDomains = dnsQueries.map { it.domain }.distinct().size
        val averageLatency = if (dnsQueries.isNotEmpty()) dnsQueries.map { it.latency }.average().toLong() else 0L

        val queriesByType = dnsQueries.groupingBy { it.queryType }.eachCount()
        val domainCounts = dnsQueries.groupingBy { it.domain }.eachCount()

        val topDomains = domainCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { DomainStats(it.key, it.value, HostCategory.UNKNOWN, false) }

        val topBlockedDomains = dnsQueries
            .filter { it.isBlocked }
            .groupingBy { it.domain }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { DomainStats(it.key, it.value, HostCategory.UNKNOWN, true) }

        DnsStatistics(
            totalQueries = totalQueries,
            blockedQueries = blockedQueries,
            uniqueDomains = uniqueDomains,
            averageLatency = averageLatency,
            queriesByType = queriesByType,
            topDomains = topDomains,
            topBlockedDomains = topBlockedDomains,
            period = period,
            generatedAt = now,
        )
    }

    override suspend fun blockDomain(domain: String) {
        blockedDomains.add(domain.lowercase())
        Log.i(TAG, "Blocked domain: $domain")
    }

    override suspend fun unblockDomain(domain: String) {
        blockedDomains.remove(domain.lowercase())
        Log.i(TAG, "Unblocked domain: $domain")
    }

    override suspend fun getBlockedDomains(): List<String> = blockedDomains.toList()

    // ==================== Alerts ====================

    override suspend fun getAlerts(limit: Int, includeAcknowledged: Boolean): List<NetworkAlert> = withContext(ioDispatcher) {
        val filtered = if (includeAcknowledged) alerts else alerts.filter { !it.isAcknowledged }
        filtered.takeLast(limit).reversed()
    }

    override fun observeAlerts(): Flow<NetworkAlert> = _alertFlow.asSharedFlow()

    override suspend fun acknowledgeAlert(alertId: String) {
        val index = alerts.indexOfFirst { it.id == alertId }
        if (index >= 0) {
            alerts[index] = alerts[index].copy(isAcknowledged = true)
        }
    }

    override suspend fun acknowledgeAllAlerts() {
        alerts.replaceAll { it.copy(isAcknowledged = true) }
    }

    override suspend fun clearOldAlerts(olderThanDays: Int) {
        val cutoff = Clock.System.now().minus(kotlin.time.Duration.parse("${olderThanDays}d"))
        alerts.removeAll { it.timestamp < cutoff }
    }

    private suspend fun createAlert(
        type: NetworkAlertType,
        severity: IssueSeverity,
        title: String,
        description: String,
        packageName: String? = null,
        appName: String? = null,
        remoteHost: String? = null,
        remoteIp: String? = null,
        action: NetworkAlertAction,
    ) {
        val alert = NetworkAlert(
            id = UUID.randomUUID().toString(),
            type = type,
            severity = severity,
            title = title,
            description = description,
            packageName = packageName,
            appName = appName,
            remoteHost = remoteHost,
            remoteIp = remoteIp,
            action = action,
            timestamp = Clock.System.now(),
        )
        alerts.add(alert)
        _alertFlow.emit(alert)
    }

    // ==================== Firewall ====================

    override suspend fun getFirewallConfig(): FirewallConfig = firewallConfig

    override suspend fun updateFirewallConfig(config: FirewallConfig) {
        firewallConfig = config
        Log.i(TAG, "Firewall config updated")
    }

    override suspend fun enableFirewall() {
        firewallConfig = firewallConfig.copy(isEnabled = true)
        Log.i(TAG, "Firewall enabled")
    }

    override suspend fun disableFirewall() {
        firewallConfig = firewallConfig.copy(isEnabled = false)
        Log.i(TAG, "Firewall disabled")
    }

    override suspend fun addFirewallRule(rule: FirewallRule) {
        firewallRules.add(rule)
        Log.i(TAG, "Added firewall rule: ${rule.name}")
    }

    override suspend fun removeFirewallRule(ruleId: String) {
        firewallRules.removeAll { it.id == ruleId }
        Log.i(TAG, "Removed firewall rule: $ruleId")
    }

    override suspend fun updateFirewallRule(rule: FirewallRule) {
        val index = firewallRules.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            firewallRules[index] = rule
        }
    }

    override suspend fun getFirewallRules(): List<FirewallRule> = firewallRules.toList()

    override suspend fun toggleFirewallRule(ruleId: String, enabled: Boolean) {
        val index = firewallRules.indexOfFirst { it.id == ruleId }
        if (index >= 0) {
            firewallRules[index] = firewallRules[index].copy(isEnabled = enabled)
        }
    }

    // ==================== App Blocking ====================

    override suspend fun blockAppNetwork(packageName: String, blockWifi: Boolean, blockCellular: Boolean) {
        blockedApps.add(packageName)
        Log.i(TAG, "Blocked app network: $packageName")
    }

    override suspend fun unblockAppNetwork(packageName: String) {
        blockedApps.remove(packageName)
        Log.i(TAG, "Unblocked app network: $packageName")
    }

    override suspend fun getBlockedApps(): List<String> = blockedApps.toList()

    override suspend fun setAppBackgroundData(packageName: String, allowBackground: Boolean) {
        Log.i(TAG, "Set background data for $packageName: $allowBackground")
        // This would require device admin privileges on most Android versions
    }

    // ==================== Network Insights ====================

    override suspend fun getNetworkInsights(): NetworkInsights = analyzeAnomalies()

    override suspend fun analyzeAnomalies(): NetworkInsights = withContext(ioDispatcher) {
        val now = Clock.System.now()
        val anomalies = mutableListOf<NetworkAnomaly>()
        val privacyConcerns = mutableListOf<PrivacyConcern>()
        val recommendations = mutableListOf<NetworkRecommendation>()

        // Check for unusual connection patterns
        val suspiciousConnections = activeConnections.filter { it.isSuspicious }
        if (suspiciousConnections.isNotEmpty()) {
            anomalies.add(
                NetworkAnomaly(
                    type = AnomalyType.UNUSUAL_CONNECTION_PATTERN,
                    severity = IssueSeverity.MEDIUM,
                    description = "${suspiciousConnections.size} suspicious connections detected",
                    detectedAt = now,
                ),
            )
        }

        // Check for potential tracking
        val trackingDomains = dnsQueries.filter {
            it.domain.contains("analytics") || it.domain.contains("tracking") ||
                it.domain.contains("metrics") || it.domain.contains("telemetry")
        }
        if (trackingDomains.isNotEmpty()) {
            privacyConcerns.add(
                PrivacyConcern(
                    type = PrivacyConcernType.CROSS_APP_TRACKING,
                    severity = IssueSeverity.LOW,
                    description = "Apps are connecting to tracking/analytics services",
                    affectedApps = trackingDomains.mapNotNull { it.packageName }.distinct(),
                    dataTypes = listOf("Usage data", "Device identifiers"),
                    recommendation = "Consider enabling tracker blocking",
                ),
            )
        }

        // Generate recommendations
        if (!firewallConfig.isEnabled) {
            recommendations.add(
                NetworkRecommendation(
                    id = "enable_firewall",
                    priority = 1,
                    title = "Enable Firewall",
                    description = "Enabling the firewall provides additional protection against unwanted connections.",
                    actionType = "enable_firewall",
                ),
            )
        }

        val riskScore = calculateRiskScore(anomalies, privacyConcerns)

        NetworkInsights(
            anomalies = anomalies,
            privacyConcerns = privacyConcerns,
            recommendations = recommendations,
            riskScore = riskScore,
            analysisTimestamp = now,
        )
    }

    private fun calculateRiskScore(anomalies: List<NetworkAnomaly>, concerns: List<PrivacyConcern>): Double {
        var score = 0.0
        anomalies.forEach { anomaly ->
            score += when (anomaly.severity) {
                IssueSeverity.CRITICAL -> 0.4
                IssueSeverity.HIGH -> 0.25
                IssueSeverity.MEDIUM -> 0.15
                IssueSeverity.LOW -> 0.05
            }
        }
        concerns.forEach { concern ->
            score += when (concern.severity) {
                IssueSeverity.CRITICAL -> 0.3
                IssueSeverity.HIGH -> 0.2
                IssueSeverity.MEDIUM -> 0.1
                IssueSeverity.LOW -> 0.05
            }
        }
        return score.coerceIn(0.0, 1.0)
    }

    // ==================== Export ====================

    override suspend fun exportLogs(period: StatisticsPeriod, format: String): ByteArray = withContext(ioDispatcher) {
        val data = mapOf(
            "connections" to activeConnections,
            "alerts" to alerts,
            "dnsQueries" to dnsQueries.takeLast(1000),
            "exportedAt" to Clock.System.now().toString(),
        )
        json.encodeToString(data).toByteArray()
    }

    override suspend fun exportConnectionHistory(packageName: String?, format: String): ByteArray = withContext(ioDispatcher) {
        val filtered = if (packageName != null) {
            activeConnections.filter { it.packageName == packageName }
        } else {
            activeConnections
        }
        json.encodeToString(filtered).toByteArray()
    }

    // ==================== Utilities ====================

    private fun calculateDataUsage(): DataUsage {
        val rxBytes = TrafficStats.getTotalRxBytes()
        val txBytes = TrafficStats.getTotalTxBytes()
        val total = rxBytes + txBytes

        return DataUsage(
            sent = txBytes,
            received = rxBytes,
            total = total,
            formattedSent = formatBytes(txBytes),
            formattedReceived = formatBytes(rxBytes),
            formattedTotal = formatBytes(total),
        )
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
