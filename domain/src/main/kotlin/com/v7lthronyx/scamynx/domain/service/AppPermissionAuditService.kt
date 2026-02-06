package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.AppPermissionAudit
import com.v7lthronyx.scamynx.domain.model.BatchPermissionAction
import com.v7lthronyx.scamynx.domain.model.DevicePermissionAudit
import com.v7lthronyx.scamynx.domain.model.PermissionActionResult
import com.v7lthronyx.scamynx.domain.model.PermissionDetail
import com.v7lthronyx.scamynx.domain.model.PermissionGroup
import com.v7lthronyx.scamynx.domain.model.PermissionRecommendation
import com.v7lthronyx.scamynx.domain.model.PermissionRiskLevel
import com.v7lthronyx.scamynx.domain.model.PermissionUsageEvent
import com.v7lthronyx.scamynx.domain.model.RecentPermissionChange
import kotlinx.coroutines.flow.Flow

interface AppPermissionAuditService {


    suspend fun auditDevice(includeSystemApps: Boolean = false): DevicePermissionAudit

    suspend fun getCachedDeviceAudit(): DevicePermissionAudit?

    suspend fun getDeviceRiskScore(): Double

    suspend fun getDevicePrivacyScore(): Int


    suspend fun auditApp(packageName: String): AppPermissionAudit

    suspend fun getAppsByRiskLevel(
        riskLevel: PermissionRiskLevel,
        limit: Int = 50,
    ): List<AppPermissionAudit>

    suspend fun getAppsWithPermission(
        permission: String,
        grantedOnly: Boolean = true,
    ): List<AppPermissionAudit>

    suspend fun getAppsWithExcessivePermissions(limit: Int = 20): List<AppPermissionAudit>


    suspend fun getPermissionDetail(permission: String): PermissionDetail?

    suspend fun getPermissionsInGroup(group: PermissionGroup): List<PermissionDetail>

    suspend fun getDangerousPermissions(): List<PermissionDetail>

    suspend fun searchPermissions(query: String): List<PermissionDetail>


    suspend fun getPermissionUsageHistory(
        packageName: String,
        permission: String? = null,
        limit: Int = 100,
    ): List<PermissionUsageEvent>

    suspend fun getRecentPermissionUsers(
        permission: String,
        withinHours: Int = 24,
    ): List<String>

    suspend fun getUnusedPermissions(
        packageName: String,
        unusedDays: Int = 30,
    ): List<String>


    suspend fun getRecentPermissionChanges(limit: Int = 50): List<RecentPermissionChange>

    fun observePermissionChanges(): Flow<RecentPermissionChange>


    suspend fun getTopRecommendations(limit: Int = 10): List<PermissionRecommendation>

    suspend fun getAppRecommendations(packageName: String): List<PermissionRecommendation>

    suspend fun getGroupRecommendations(group: PermissionGroup): List<PermissionRecommendation>


    suspend fun revokePermission(
        packageName: String,
        permission: String,
    ): PermissionActionResult

    suspend fun revokeAllPermissions(packageName: String): List<PermissionActionResult>

    suspend fun restrictToForeground(
        packageName: String,
        permission: String,
    ): PermissionActionResult

    suspend fun executeBatchAction(batchAction: BatchPermissionAction): List<PermissionActionResult>

    suspend fun applyRecommendation(recommendation: PermissionRecommendation): List<PermissionActionResult>

    suspend fun applyTopRecommendations(limit: Int = 5): List<PermissionActionResult>


    suspend fun getGroupStatistics(): Map<PermissionGroup, GroupStatistics>

    suspend fun revokePermissionGroup(
        packageName: String,
        group: PermissionGroup,
    ): List<PermissionActionResult>


    suspend fun enableMonitoring()

    suspend fun disableMonitoring()

    suspend fun isMonitoringEnabled(): Boolean

    fun observePermissionUsage(): Flow<PermissionUsageEvent>


    suspend fun exportAuditJson(): String

    suspend fun exportAuditPdf(): ByteArray
}

data class GroupStatistics(
    val group: PermissionGroup,
    val totalApps: Int,
    val grantedCount: Int,
    val deniedCount: Int,
    val backgroundCount: Int,
    val unusedCount: Int,
    val riskScore: Double,
)
