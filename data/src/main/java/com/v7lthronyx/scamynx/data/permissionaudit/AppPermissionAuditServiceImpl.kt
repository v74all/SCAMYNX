package com.v7lthronyx.scamynx.data.permissionaudit

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.domain.model.AppPermissionAudit
import com.v7lthronyx.scamynx.domain.model.AppPermissionState
import com.v7lthronyx.scamynx.domain.model.AppPermissionSummary
import com.v7lthronyx.scamynx.domain.model.BatchPermissionAction
import com.v7lthronyx.scamynx.domain.model.ConcernType
import com.v7lthronyx.scamynx.domain.model.DevicePermissionAudit
import com.v7lthronyx.scamynx.domain.model.ExcessivePermission
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.PermissionActionResult
import com.v7lthronyx.scamynx.domain.model.PermissionActionType
import com.v7lthronyx.scamynx.domain.model.PermissionConcern
import com.v7lthronyx.scamynx.domain.model.PermissionDetail
import com.v7lthronyx.scamynx.domain.model.PermissionFlag
import com.v7lthronyx.scamynx.domain.model.PermissionGroup
import com.v7lthronyx.scamynx.domain.model.PermissionGroupStats
import com.v7lthronyx.scamynx.domain.model.PermissionProtectionLevel
import com.v7lthronyx.scamynx.domain.model.PermissionRecommendation
import com.v7lthronyx.scamynx.domain.model.PermissionRiskLevel
import com.v7lthronyx.scamynx.domain.model.PermissionUsageEvent
import com.v7lthronyx.scamynx.domain.model.RecentPermissionChange
import com.v7lthronyx.scamynx.domain.model.RiskTrend
import com.v7lthronyx.scamynx.domain.service.AppPermissionAuditService
import com.v7lthronyx.scamynx.domain.service.GroupStatistics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPermissionAuditServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : AppPermissionAuditService {

    private val packageManager: PackageManager = context.packageManager

    private var cachedDeviceAudit: DevicePermissionAudit? = null
    private var monitoringEnabled = false

    private val permissionChangesFlow = MutableSharedFlow<RecentPermissionChange>(replay = 10)
    private val permissionUsageFlow = MutableSharedFlow<PermissionUsageEvent>(replay = 10)

    // ============== DEVICE AUDIT ==============

    override suspend fun auditDevice(includeSystemApps: Boolean): DevicePermissionAudit = withContext(dispatcher) {
        val packages = getInstalledPackages(includeSystemApps)
        val appAudits = packages.map { pkg -> auditPackage(pkg) }

        val dangerousCount = appAudits.count { it.dangerousPermissions > 0 }
        val excessiveCount = appAudits.count { it.excessivePermissions.isNotEmpty() }
        val backgroundCount = appAudits.count { audit ->
            audit.permissions.any { it.isBackgroundAllowed }
        }

        val groupDistribution = calculateGroupDistribution(appAudits)
        val topConcerns = appAudits.flatMap { it.concerns }
            .groupBy { it.type }
            .map { (_, concerns) -> concerns.maxByOrNull { it.severity.ordinal }!! }
            .sortedByDescending { it.severity.ordinal }
            .take(5)

        val topRecommendations = appAudits.flatMap { it.recommendations }
            .sortedByDescending { it.priority }
            .take(10)

        val overallRiskScore = if (appAudits.isNotEmpty()) {
            appAudits.sumOf { it.riskScore } / appAudits.size
        } else 0.0

        val overallPrivacyScore = if (appAudits.isNotEmpty()) {
            appAudits.sumOf { it.privacyScore } / appAudits.size
        } else 100

        val highRiskApps = appAudits
            .filter { it.riskLevel in listOf(PermissionRiskLevel.HIGH, PermissionRiskLevel.DANGEROUS) }
            .sortedByDescending { it.riskScore }
            .take(10)
            .map { audit ->
                AppPermissionSummary(
                    packageName = audit.packageName,
                    appName = audit.appName,
                    riskScore = audit.riskScore,
                    riskLevel = audit.riskLevel,
                    dangerousPermissions = audit.dangerousPermissions,
                    topConcern = audit.concerns.firstOrNull()?.title,
                )
            }

        val result = DevicePermissionAudit(
            totalApps = appAudits.size,
            appsWithDangerousPermissions = dangerousCount,
            appsWithExcessivePermissions = excessiveCount,
            appsWithBackgroundAccess = backgroundCount,
            permissionDistribution = groupDistribution,
            topConcerns = topConcerns,
            topRecommendations = topRecommendations,
            overallRiskScore = overallRiskScore,
            overallPrivacyScore = overallPrivacyScore,
            riskTrend = RiskTrend.STABLE,
            highRiskApps = highRiskApps,
            recentlyGranted = emptyList(),
            recentlyRevoked = emptyList(),
            auditedAt = clock.now(),
        )

        cachedDeviceAudit = result
        result
    }

    override suspend fun getCachedDeviceAudit(): DevicePermissionAudit? = cachedDeviceAudit

    override suspend fun getDeviceRiskScore(): Double = withContext(dispatcher) {
        cachedDeviceAudit?.overallRiskScore ?: auditDevice(false).overallRiskScore
    }

    override suspend fun getDevicePrivacyScore(): Int = withContext(dispatcher) {
        cachedDeviceAudit?.overallPrivacyScore ?: auditDevice(false).overallPrivacyScore
    }

    // ============== APP AUDIT ==============

    override suspend fun auditApp(packageName: String): AppPermissionAudit = withContext(dispatcher) {
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            throw IllegalArgumentException("Package not found: $packageName")
        }
        auditPackage(packageInfo)
    }

    override suspend fun getAppsByRiskLevel(
        riskLevel: PermissionRiskLevel,
        limit: Int,
    ): List<AppPermissionAudit> = withContext(dispatcher) {
        val packages = getInstalledPackages(includeSystemApps = false)
        packages.map { auditPackage(it) }
            .filter { it.riskLevel == riskLevel }
            .sortedByDescending { it.riskScore }
            .take(limit)
    }

    override suspend fun getAppsWithPermission(
        permission: String,
        grantedOnly: Boolean,
    ): List<AppPermissionAudit> = withContext(dispatcher) {
        val packages = getInstalledPackages(includeSystemApps = false)
        packages.map { auditPackage(it) }
            .filter { audit ->
                audit.permissions.any { state ->
                    state.permission == permission && (!grantedOnly || state.isGranted)
                }
            }
    }

    override suspend fun getAppsWithExcessivePermissions(limit: Int): List<AppPermissionAudit> = withContext(dispatcher) {
        val packages = getInstalledPackages(includeSystemApps = false)
        packages.map { auditPackage(it) }
            .filter { it.excessivePermissions.isNotEmpty() }
            .sortedByDescending { it.excessivePermissions.size }
            .take(limit)
    }

    // ============== PERMISSION INFO ==============

    override suspend fun getPermissionDetail(permission: String): PermissionDetail? = withContext(dispatcher) {
        PermissionDatabase.permissions[permission]
    }

    override suspend fun getPermissionsInGroup(group: PermissionGroup): List<PermissionDetail> = withContext(dispatcher) {
        PermissionDatabase.getPermissionsByGroup(group)
    }

    override suspend fun getDangerousPermissions(): List<PermissionDetail> = withContext(dispatcher) {
        PermissionDatabase.getDangerousPermissions()
    }

    override suspend fun searchPermissions(query: String): List<PermissionDetail> = withContext(dispatcher) {
        PermissionDatabase.searchPermissions(query)
    }

    // ============== USAGE HISTORY ==============

    override suspend fun getPermissionUsageHistory(
        packageName: String,
        permission: String?,
        limit: Int,
    ): List<PermissionUsageEvent> = withContext(dispatcher) {
        // Requires AppOps API which needs special permissions
        // Return empty for now - can be implemented with UsageStatsManager
        emptyList()
    }

    override suspend fun getRecentPermissionUsers(
        permission: String,
        withinHours: Int,
    ): List<String> = withContext(dispatcher) {
        // Return apps that have this permission granted
        getAppsWithPermission(permission, grantedOnly = true).map { it.packageName }
    }

    override suspend fun getUnusedPermissions(
        packageName: String,
        unusedDays: Int,
    ): List<String> = withContext(dispatcher) {
        val audit = auditApp(packageName)
        audit.unusedPermissions
    }

    // ============== PERMISSION CHANGES ==============

    override suspend fun getRecentPermissionChanges(limit: Int): List<RecentPermissionChange> = withContext(dispatcher) {
        // Would require monitoring over time - return empty for now
        emptyList()
    }

    override fun observePermissionChanges(): Flow<RecentPermissionChange> = permissionChangesFlow.asSharedFlow()

    // ============== RECOMMENDATIONS ==============

    override suspend fun getTopRecommendations(limit: Int): List<PermissionRecommendation> = withContext(dispatcher) {
        val audit = cachedDeviceAudit ?: auditDevice(false)
        audit.topRecommendations.take(limit)
    }

    override suspend fun getAppRecommendations(packageName: String): List<PermissionRecommendation> = withContext(dispatcher) {
        val audit = auditApp(packageName)
        audit.recommendations
    }

    override suspend fun getGroupRecommendations(group: PermissionGroup): List<PermissionRecommendation> = withContext(dispatcher) {
        val audit = cachedDeviceAudit ?: auditDevice(false)
        audit.topRecommendations.filter { rec ->
            rec.affectedPermissions.any { perm ->
                PermissionDatabase.getPermissionDetail(perm).group == group
            }
        }
    }

    // ============== PERMISSION ACTIONS ==============

    override suspend fun revokePermission(
        packageName: String,
        permission: String,
    ): PermissionActionResult = withContext(dispatcher) {
        // Cannot programmatically revoke permissions without root/device owner
        // Return a result indicating user action is required
        PermissionActionResult(
            packageName = packageName,
            permission = permission,
            action = PermissionActionType.REVOKE,
            success = false,
            message = "باز کردن تنظیمات برنامه برای لغو دستی اجازه",
            requiresUserAction = true,
            timestamp = clock.now(),
        )
    }

    override suspend fun revokeAllPermissions(packageName: String): List<PermissionActionResult> = withContext(dispatcher) {
        val audit = auditApp(packageName)
        audit.permissions.filter { it.isGranted }.map { state ->
            revokePermission(packageName, state.permission)
        }
    }

    override suspend fun restrictToForeground(
        packageName: String,
        permission: String,
    ): PermissionActionResult = withContext(dispatcher) {
        PermissionActionResult(
            packageName = packageName,
            permission = permission,
            action = PermissionActionType.RESTRICT_BACKGROUND,
            success = false,
            message = "نیاز به باز کردن تنظیمات برنامه",
            requiresUserAction = true,
            timestamp = clock.now(),
        )
    }

    override suspend fun executeBatchAction(batchAction: BatchPermissionAction): List<PermissionActionResult> = withContext(dispatcher) {
        batchAction.actions.map { action ->
            when (action.action) {
                PermissionActionType.REVOKE -> revokePermission(action.packageName, action.permission)
                PermissionActionType.RESTRICT_BACKGROUND -> restrictToForeground(action.packageName, action.permission)
                else -> PermissionActionResult(
                    packageName = action.packageName,
                    permission = action.permission,
                    action = action.action,
                    success = false,
                    message = "این عملیات پشتیبانی نمی‌شود",
                    timestamp = clock.now(),
                )
            }
        }
    }

    override suspend fun applyRecommendation(recommendation: PermissionRecommendation): List<PermissionActionResult> = withContext(dispatcher) {
        // Most recommendations require user action in settings
        recommendation.affectedPermissions.map { perm ->
            PermissionActionResult(
                packageName = "",
                permission = perm,
                action = recommendation.actionType,
                success = false,
                message = recommendation.description,
                requiresUserAction = true,
                timestamp = clock.now(),
            )
        }
    }

    override suspend fun applyTopRecommendations(limit: Int): List<PermissionActionResult> = withContext(dispatcher) {
        getTopRecommendations(limit).flatMap { applyRecommendation(it) }
    }

    // ============== GROUP STATISTICS ==============

    override suspend fun getGroupStatistics(): Map<PermissionGroup, GroupStatistics> = withContext(dispatcher) {
        val audit = cachedDeviceAudit ?: auditDevice(false)
        audit.permissionDistribution.map { (group, stats) ->
            group to GroupStatistics(
                group = group,
                totalApps = stats.totalApps,
                grantedCount = stats.grantedCount,
                deniedCount = stats.deniedCount,
                backgroundCount = stats.backgroundCount,
                unusedCount = 0,
                riskScore = when (stats.avgRiskLevel) {
                    PermissionRiskLevel.DANGEROUS -> 0.9
                    PermissionRiskLevel.HIGH -> 0.7
                    PermissionRiskLevel.MEDIUM -> 0.5
                    PermissionRiskLevel.LOW -> 0.3
                    PermissionRiskLevel.SAFE -> 0.1
                },
            )
        }.toMap()
    }

    override suspend fun revokePermissionGroup(
        packageName: String,
        group: PermissionGroup,
    ): List<PermissionActionResult> = withContext(dispatcher) {
        val audit = auditApp(packageName)
        val groupPermissions = audit.permissions.filter { state ->
            PermissionDatabase.getPermissionDetail(state.permission).group == group && state.isGranted
        }
        groupPermissions.map { state ->
            revokePermission(packageName, state.permission)
        }
    }

    // ============== MONITORING ==============

    override suspend fun enableMonitoring() {
        monitoringEnabled = true
    }

    override suspend fun disableMonitoring() {
        monitoringEnabled = false
    }

    override suspend fun isMonitoringEnabled(): Boolean = monitoringEnabled

    override fun observePermissionUsage(): Flow<PermissionUsageEvent> = permissionUsageFlow.asSharedFlow()

    // ============== EXPORT ==============

    override suspend fun exportAuditJson(): String = withContext(dispatcher) {
        val audit = cachedDeviceAudit ?: auditDevice(false)
        // Simple JSON export
        buildString {
            appendLine("{")
            appendLine("  \"totalApps\": ${audit.totalApps},")
            appendLine("  \"dangerousApps\": ${audit.appsWithDangerousPermissions},")
            appendLine("  \"riskScore\": ${audit.overallRiskScore},")
            appendLine("  \"privacyScore\": ${audit.overallPrivacyScore},")
            appendLine("  \"auditedAt\": \"${audit.auditedAt}\"")
            appendLine("}")
        }
    }

    override suspend fun exportAuditPdf(): ByteArray = withContext(dispatcher) {
        // PDF generation would require a library - return simple text as bytes
        val text = exportAuditJson()
        text.toByteArray(Charsets.UTF_8)
    }

    // ============== PRIVATE HELPERS ==============

    private fun getInstalledPackages(includeSystemApps: Boolean): List<PackageInfo> {
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val flags = PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            packageManager.getInstalledPackages(flags)
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }

        return if (includeSystemApps) {
            packages
        } else {
            packages.filter { pkg ->
                (pkg.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) ?: 0) == 0
            }
        }
    }

    private fun auditPackage(packageInfo: PackageInfo): AppPermissionAudit {
        val appInfo = packageInfo.applicationInfo
        val appName = appInfo?.let { packageManager.getApplicationLabel(it).toString() } ?: packageInfo.packageName
        val isSystemApp = (appInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) ?: 0) != 0

        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
        val permissionFlags = packageInfo.requestedPermissionsFlags ?: IntArray(0)

        val permissionStates = requestedPermissions.mapIndexed { index, permission ->
            val flag = permissionFlags.getOrElse(index) { 0 }
            val isGranted = (flag and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            val detail = PermissionDatabase.getPermissionDetail(permission)

            AppPermissionState(
                permission = permission,
                permissionDetail = detail,
                isGranted = isGranted,
                isDenied = !isGranted,
                isPermanentlyDenied = false,
                grantTime = null,
                usageCount = 0,
                lastUsed = null,
                isBackgroundAllowed = isBackgroundPermission(permission, isGranted),
                flags = buildPermissionFlags(flag),
            )
        }

        val grantedCount = permissionStates.count { it.isGranted }
        val deniedCount = permissionStates.count { it.isDenied }
        val dangerousCount = permissionStates.count {
            it.isGranted && it.permissionDetail.protectionLevel == PermissionProtectionLevel.DANGEROUS
        }

        val unusedPermissions = permissionStates
            .filter { it.isGranted && it.usageCount == 0 }
            .map { it.permission }

        val excessivePermissions = detectExcessivePermissions(packageInfo.packageName, permissionStates)
        val concerns = detectConcerns(permissionStates)
        val recommendations = generateRecommendations(packageInfo.packageName, permissionStates, concerns)

        val riskScore = calculateRiskScore(permissionStates)
        val riskLevel = when {
            riskScore >= 0.8 -> PermissionRiskLevel.DANGEROUS
            riskScore >= 0.6 -> PermissionRiskLevel.HIGH
            riskScore >= 0.4 -> PermissionRiskLevel.MEDIUM
            riskScore >= 0.2 -> PermissionRiskLevel.LOW
            else -> PermissionRiskLevel.SAFE
        }
        val privacyScore = ((1 - riskScore) * 100).toInt().coerceIn(0, 100)

        val installTime = Instant.fromEpochMilliseconds(packageInfo.firstInstallTime)
        val updateTime = Instant.fromEpochMilliseconds(packageInfo.lastUpdateTime)

        return AppPermissionAudit(
            packageName = packageInfo.packageName,
            appName = appName,
            versionName = packageInfo.versionName ?: "Unknown",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            },
            isSystemApp = isSystemApp,
            installTime = installTime,
            lastUpdateTime = updateTime,
            permissions = permissionStates,
            grantedPermissions = grantedCount,
            deniedPermissions = deniedCount,
            dangerousPermissions = dangerousCount,
            unusedPermissions = unusedPermissions,
            excessivePermissions = excessivePermissions,
            riskScore = riskScore,
            riskLevel = riskLevel,
            privacyScore = privacyScore,
            concerns = concerns,
            recommendations = recommendations,
            backgroundUsage = null,
            permissionUsageHistory = emptyList(),
            auditedAt = clock.now(),
        )
    }

    private fun isBackgroundPermission(permission: String, isGranted: Boolean): Boolean {
        if (!isGranted) return false
        return permission in listOf(
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        )
    }

    private fun buildPermissionFlags(flag: Int): List<PermissionFlag> {
        val flags = mutableListOf<PermissionFlag>()
        // Add flags based on the permission flag value if needed
        return flags
    }

    private fun detectExcessivePermissions(
        packageName: String,
        states: List<AppPermissionState>,
    ): List<ExcessivePermission> {
        val excessive = mutableListOf<ExcessivePermission>()

        // Check for dangerous permission combinations
        val hasCamera = states.any { it.permission == android.Manifest.permission.CAMERA && it.isGranted }
        val hasMicrophone = states.any { it.permission == android.Manifest.permission.RECORD_AUDIO && it.isGranted }
        val hasLocation = states.any { it.permission == android.Manifest.permission.ACCESS_FINE_LOCATION && it.isGranted }

        if (hasCamera && hasMicrophone && hasLocation) {
            excessive += ExcessivePermission(
                permission = "CAMERA+MICROPHONE+LOCATION",
                reason = "ترکیب خطرناک: دوربین، میکروفون و موقعیت",
                suggestedAction = "بررسی نیاز واقعی برنامه به این اجازه‌ها",
                severity = IssueSeverity.HIGH,
            )
        }

        // Check for background location
        if (states.any { it.permission == android.Manifest.permission.ACCESS_BACKGROUND_LOCATION && it.isGranted }) {
            excessive += ExcessivePermission(
                permission = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                reason = "دسترسی دائمی به موقعیت در پس‌زمینه",
                suggestedAction = "محدود کردن به فقط هنگام استفاده",
                severity = IssueSeverity.HIGH,
            )
        }

        // Check for SMS permissions
        val hasSmsRead = states.any { it.permission == android.Manifest.permission.READ_SMS && it.isGranted }
        val hasSmsSend = states.any { it.permission == android.Manifest.permission.SEND_SMS && it.isGranted }
        if (hasSmsRead && hasSmsSend) {
            excessive += ExcessivePermission(
                permission = "SMS_FULL_ACCESS",
                reason = "دسترسی کامل به پیامک‌ها - خطر سرقت OTP",
                suggestedAction = "لغو دسترسی پیامک در صورت عدم نیاز",
                severity = IssueSeverity.CRITICAL,
            )
        }

        return excessive
    }

    private fun detectConcerns(states: List<AppPermissionState>): List<PermissionConcern> {
        val concerns = mutableListOf<PermissionConcern>()

        // Background location concern
        val backgroundLocation = states.find {
            it.permission == android.Manifest.permission.ACCESS_BACKGROUND_LOCATION && it.isGranted
        }
        if (backgroundLocation != null) {
            concerns += PermissionConcern(
                type = ConcernType.LOCATION_ALWAYS,
                severity = IssueSeverity.HIGH,
                title = "موقعیت دائمی فعال",
                description = "این برنامه همیشه به موقعیت شما دسترسی دارد",
                affectedPermissions = listOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                recommendation = "محدود کردن به 'فقط هنگام استفاده'",
            )
        }

        // Camera + Microphone concern
        val hasCamera = states.any { it.permission == android.Manifest.permission.CAMERA && it.isGranted }
        val hasMicrophone = states.any { it.permission == android.Manifest.permission.RECORD_AUDIO && it.isGranted }
        if (hasCamera && hasMicrophone) {
            concerns += PermissionConcern(
                type = ConcernType.CAMERA_MICROPHONE,
                severity = IssueSeverity.MEDIUM,
                title = "دسترسی به دوربین و میکروفون",
                description = "امکان ضبط تصویر و صدا",
                affectedPermissions = listOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO,
                ),
                recommendation = "بررسی نیاز واقعی برنامه",
            )
        }

        // Contacts access
        val hasContacts = states.any { it.permission == android.Manifest.permission.READ_CONTACTS && it.isGranted }
        if (hasContacts) {
            concerns += PermissionConcern(
                type = ConcernType.CONTACTS_ACCESS,
                severity = IssueSeverity.MEDIUM,
                title = "دسترسی به مخاطبین",
                description = "برنامه می‌تواند لیست مخاطبین شما را بخواند",
                affectedPermissions = listOf(android.Manifest.permission.READ_CONTACTS),
                recommendation = "لغو در صورت عدم نیاز",
            )
        }

        return concerns
    }

    private fun generateRecommendations(
        packageName: String,
        states: List<AppPermissionState>,
        concerns: List<PermissionConcern>,
    ): List<PermissionRecommendation> {
        val recommendations = mutableListOf<PermissionRecommendation>()
        var priority = 1

        // Recommend restricting background location
        if (states.any { it.permission == android.Manifest.permission.ACCESS_BACKGROUND_LOCATION && it.isGranted }) {
            recommendations += PermissionRecommendation(
                id = "${packageName}_restrict_bg_location",
                priority = priority++,
                title = "محدود کردن موقعیت",
                description = "تغییر به 'فقط هنگام استفاده'",
                actionType = PermissionActionType.RESTRICT_BACKGROUND,
                affectedPermissions = listOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                privacyImpact = "جلوگیری از ردیابی دائمی",
                canAutoApply = false,
            )
        }

        // Recommend revoking SMS for non-messaging apps
        val hasSms = states.any {
            it.permission in listOf(
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.RECEIVE_SMS,
            ) && it.isGranted
        }
        if (hasSms) {
            recommendations += PermissionRecommendation(
                id = "${packageName}_revoke_sms",
                priority = priority++,
                title = "بررسی دسترسی پیامک",
                description = "این برنامه به پیامک‌ها دسترسی دارد",
                actionType = PermissionActionType.REVIEW,
                affectedPermissions = listOf(
                    android.Manifest.permission.READ_SMS,
                    android.Manifest.permission.SEND_SMS,
                ),
                privacyImpact = "حفاظت از کدهای OTP",
                canAutoApply = false,
            )
        }

        return recommendations
    }

    private fun calculateRiskScore(states: List<AppPermissionState>): Double {
        if (states.isEmpty()) return 0.0

        val grantedDangerous = states.filter {
            it.isGranted && it.permissionDetail.protectionLevel == PermissionProtectionLevel.DANGEROUS
        }

        if (grantedDangerous.isEmpty()) return 0.0

        val riskSum = grantedDangerous.sumOf { state ->
            when (state.permissionDetail.riskLevel) {
                PermissionRiskLevel.DANGEROUS -> 1.0
                PermissionRiskLevel.HIGH -> 0.8
                PermissionRiskLevel.MEDIUM -> 0.5
                PermissionRiskLevel.LOW -> 0.2
                PermissionRiskLevel.SAFE -> 0.0
            }
        }

        // Normalize: max score is number of permissions
        return (riskSum / grantedDangerous.size.coerceAtLeast(1)).coerceIn(0.0, 1.0)
    }

    private fun calculateGroupDistribution(audits: List<AppPermissionAudit>): Map<PermissionGroup, PermissionGroupStats> {
        val groupMap = mutableMapOf<PermissionGroup, MutableList<AppPermissionState>>()

        audits.forEach { audit ->
            audit.permissions.forEach { state ->
                val group = state.permissionDetail.group
                groupMap.getOrPut(group) { mutableListOf() }.add(state)
            }
        }

        return groupMap.map { (group, states) ->
            val grantedCount = states.count { it.isGranted }
            val deniedCount = states.count { it.isDenied }
            val backgroundCount = states.count { it.isBackgroundAllowed }
            val avgRisk = states.map { it.permissionDetail.riskLevel.ordinal }.average()

            group to PermissionGroupStats(
                group = group,
                totalApps = states.map { it.permission }.distinct().size,
                grantedCount = grantedCount,
                deniedCount = deniedCount,
                backgroundCount = backgroundCount,
                avgRiskLevel = PermissionRiskLevel.entries[avgRisk.toInt().coerceIn(0, 4)],
            )
        }.toMap()
    }
}
