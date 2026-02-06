package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.ActiveConnection
import com.v7lthronyx.scamynx.domain.model.AppNetworkUsage
import com.v7lthronyx.scamynx.domain.model.DnsQuery
import com.v7lthronyx.scamynx.domain.model.DnsStatistics
import com.v7lthronyx.scamynx.domain.model.FirewallConfig
import com.v7lthronyx.scamynx.domain.model.FirewallRule
import com.v7lthronyx.scamynx.domain.model.NetworkAlert
import com.v7lthronyx.scamynx.domain.model.NetworkInsights
import com.v7lthronyx.scamynx.domain.model.NetworkMonitorState
import com.v7lthronyx.scamynx.domain.model.NetworkMonitorStatus
import com.v7lthronyx.scamynx.domain.model.StatisticsPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitorService {


    val monitorState: StateFlow<NetworkMonitorState>

    suspend fun getStatus(): NetworkMonitorStatus

    suspend fun startMonitoring(useVpn: Boolean = false)

    suspend fun stopMonitoring()

    fun isMonitoringActive(): Boolean


    suspend fun getActiveConnections(): List<ActiveConnection>

    suspend fun getConnectionsForApp(packageName: String): List<ActiveConnection>

    fun observeConnections(): Flow<ActiveConnection>

    fun observeSuspiciousConnections(): Flow<ActiveConnection>

    suspend fun blockConnection(connectionId: String)


    suspend fun getAppNetworkUsage(period: StatisticsPeriod): List<AppNetworkUsage>

    suspend fun getAppUsage(
        packageName: String,
        period: StatisticsPeriod,
    ): AppNetworkUsage?

    suspend fun getTopAppsByUsage(
        limit: Int = 10,
        period: StatisticsPeriod = StatisticsPeriod.TODAY,
    ): List<AppNetworkUsage>


    suspend fun getDnsQueries(
        limit: Int = 100,
        packageName: String? = null,
    ): List<DnsQuery>

    fun observeDnsQueries(): Flow<DnsQuery>

    suspend fun getDnsStatistics(period: StatisticsPeriod): DnsStatistics

    suspend fun blockDomain(domain: String)

    suspend fun unblockDomain(domain: String)

    suspend fun getBlockedDomains(): List<String>


    suspend fun getAlerts(
        limit: Int = 50,
        includeAcknowledged: Boolean = false,
    ): List<NetworkAlert>

    fun observeAlerts(): Flow<NetworkAlert>

    suspend fun acknowledgeAlert(alertId: String)

    suspend fun acknowledgeAllAlerts()

    suspend fun clearOldAlerts(olderThanDays: Int = 30)


    suspend fun getFirewallConfig(): FirewallConfig

    suspend fun updateFirewallConfig(config: FirewallConfig)

    suspend fun enableFirewall()

    suspend fun disableFirewall()

    suspend fun addFirewallRule(rule: FirewallRule)

    suspend fun removeFirewallRule(ruleId: String)

    suspend fun updateFirewallRule(rule: FirewallRule)

    suspend fun getFirewallRules(): List<FirewallRule>

    suspend fun toggleFirewallRule(ruleId: String, enabled: Boolean)


    suspend fun blockAppNetwork(
        packageName: String,
        blockWifi: Boolean = true,
        blockCellular: Boolean = true,
    )

    suspend fun unblockAppNetwork(packageName: String)

    suspend fun getBlockedApps(): List<String>

    suspend fun setAppBackgroundData(packageName: String, allowBackground: Boolean)


    suspend fun getNetworkInsights(): NetworkInsights

    suspend fun analyzeAnomalies(): NetworkInsights


    suspend fun exportLogs(
        period: StatisticsPeriod,
        format: String = "json",
    ): ByteArray

    suspend fun exportConnectionHistory(
        packageName: String? = null,
        format: String = "json",
    ): ByteArray
}
