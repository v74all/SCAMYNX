package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class PermissionRiskLevel {
    @SerialName("safe")
    SAFE,

    @SerialName("low")
    LOW,

    @SerialName("medium")
    MEDIUM,

    @SerialName("high")
    HIGH,

    @SerialName("dangerous")
    DANGEROUS,
}

@Serializable
enum class PermissionGroup {
    @SerialName("location")
    LOCATION,

    @SerialName("camera")
    CAMERA,

    @SerialName("microphone")
    MICROPHONE,

    @SerialName("storage")
    STORAGE,

    @SerialName("contacts")
    CONTACTS,

    @SerialName("phone")
    PHONE,

    @SerialName("sms")
    SMS,

    @SerialName("calendar")
    CALENDAR,

    @SerialName("call_log")
    CALL_LOG,

    @SerialName("sensors")
    SENSORS,

    @SerialName("activity_recognition")
    ACTIVITY_RECOGNITION,

    @SerialName("nearby_devices")
    NEARBY_DEVICES,

    @SerialName("notifications")
    NOTIFICATIONS,

    @SerialName("media")
    MEDIA,

    @SerialName("health")
    HEALTH,

    @SerialName("other")
    OTHER,
}


@Serializable
data class PermissionDetail(
    val name: String,
    val label: String,
    val description: String,
    val group: PermissionGroup,
    val riskLevel: PermissionRiskLevel,
    val protectionLevel: PermissionProtectionLevel,
    val isRuntime: Boolean,
    val privacyImplications: List<String>,
    val commonAbuses: List<String>,
)

@Serializable
enum class PermissionProtectionLevel {
    @SerialName("normal")
    NORMAL,

    @SerialName("dangerous")
    DANGEROUS,

    @SerialName("signature")
    SIGNATURE,

    @SerialName("privileged")
    PRIVILEGED,
}


@Serializable
data class AppPermissionState(
    val permission: String,
    val permissionDetail: PermissionDetail,
    val isGranted: Boolean,
    val isDenied: Boolean,
    val isPermanentlyDenied: Boolean,
    val grantTime: Instant? = null,
    val usageCount: Int = 0,
    val lastUsed: Instant? = null,
    val isBackgroundAllowed: Boolean = false,
    val flags: List<PermissionFlag> = emptyList(),
)

@Serializable
enum class PermissionFlag {
    @SerialName("one_time")
    ONE_TIME,

    @SerialName("foreground_only")
    FOREGROUND_ONLY,

    @SerialName("user_set")
    USER_SET,

    @SerialName("policy_fixed")
    POLICY_FIXED,

    @SerialName("system_fixed")
    SYSTEM_FIXED,

    @SerialName("auto_revoked")
    AUTO_REVOKED,
}


@Serializable
data class AppPermissionAudit(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val installTime: Instant,
    val lastUpdateTime: Instant,

    val permissions: List<AppPermissionState>,
    val grantedPermissions: Int,
    val deniedPermissions: Int,
    val dangerousPermissions: Int,
    val unusedPermissions: List<String>,
    val excessivePermissions: List<ExcessivePermission>,

    val riskScore: Double,
    val riskLevel: PermissionRiskLevel,
    val privacyScore: Int,
    val concerns: List<PermissionConcern>,
    val recommendations: List<PermissionRecommendation>,

    val backgroundUsage: BackgroundUsageStats? = null,
    val permissionUsageHistory: List<PermissionUsageEvent>,

    val auditedAt: Instant,
)

@Serializable
data class ExcessivePermission(
    val permission: String,
    val reason: String,
    val suggestedAction: String,
    val severity: IssueSeverity,
)

@Serializable
data class PermissionConcern(
    val type: ConcernType,
    val severity: IssueSeverity,
    val title: String,
    val description: String,
    val affectedPermissions: List<String>,
    val recommendation: String,
)

@Serializable
enum class ConcernType {
    @SerialName("excessive_permissions")
    EXCESSIVE_PERMISSIONS,

    @SerialName("background_access")
    BACKGROUND_ACCESS,

    @SerialName("location_always")
    LOCATION_ALWAYS,

    @SerialName("camera_microphone")
    CAMERA_MICROPHONE,

    @SerialName("contacts_access")
    CONTACTS_ACCESS,

    @SerialName("storage_access")
    STORAGE_ACCESS,

    @SerialName("phone_access")
    PHONE_ACCESS,

    @SerialName("sms_access")
    SMS_ACCESS,

    @SerialName("unused_permissions")
    UNUSED_PERMISSIONS,

    @SerialName("rare_permission")
    RARE_PERMISSION,

    @SerialName("permission_combination")
    PERMISSION_COMBINATION,
}

@Serializable
data class PermissionRecommendation(
    val id: String,
    val priority: Int,
    val title: String,
    val description: String,
    val actionType: PermissionActionType,
    val affectedPermissions: List<String>,
    val privacyImpact: String,
    val canAutoApply: Boolean = false,
)

@Serializable
enum class PermissionActionType {
    @SerialName("revoke")
    REVOKE,

    @SerialName("restrict_background")
    RESTRICT_BACKGROUND,

    @SerialName("set_foreground_only")
    SET_FOREGROUND_ONLY,

    @SerialName("review")
    REVIEW,

    @SerialName("uninstall")
    UNINSTALL,

    @SerialName("monitor")
    MONITOR,
}


@Serializable
data class BackgroundUsageStats(
    val totalBackgroundTime: Long,
    val backgroundStarts: Int,
    val lastBackgroundStart: Instant? = null,
    val backgroundDataUsage: Long,
    val batteryUsage: Double,
    val wakelockTime: Long,
)

@Serializable
data class PermissionUsageEvent(
    val permission: String,
    val timestamp: Instant,
    val duration: Long? = null,
    val wasBackground: Boolean,
    val proxyApp: String? = null,
)


@Serializable
data class DevicePermissionAudit(
    val totalApps: Int,
    val appsWithDangerousPermissions: Int,
    val appsWithExcessivePermissions: Int,
    val appsWithBackgroundAccess: Int,

    val permissionDistribution: Map<PermissionGroup, PermissionGroupStats>,

    val topConcerns: List<PermissionConcern>,
    val topRecommendations: List<PermissionRecommendation>,

    val overallRiskScore: Double,
    val overallPrivacyScore: Int,
    val riskTrend: RiskTrend,

    val highRiskApps: List<AppPermissionSummary>,

    val recentlyGranted: List<RecentPermissionChange>,
    val recentlyRevoked: List<RecentPermissionChange>,

    val auditedAt: Instant,
)

@Serializable
data class PermissionGroupStats(
    val group: PermissionGroup,
    val totalApps: Int,
    val grantedCount: Int,
    val deniedCount: Int,
    val backgroundCount: Int,
    val avgRiskLevel: PermissionRiskLevel,
)

@Serializable
data class AppPermissionSummary(
    val packageName: String,
    val appName: String,
    val riskScore: Double,
    val riskLevel: PermissionRiskLevel,
    val dangerousPermissions: Int,
    val topConcern: String? = null,
)

@Serializable
data class RecentPermissionChange(
    val packageName: String,
    val appName: String,
    val permission: String,
    val permissionLabel: String,
    val changeType: PermissionChangeType,
    val timestamp: Instant,
)

@Serializable
enum class PermissionChangeType {
    @SerialName("granted")
    GRANTED,

    @SerialName("revoked")
    REVOKED,

    @SerialName("upgraded")
    UPGRADED,

    @SerialName("downgraded")
    DOWNGRADED,
}

@Serializable
enum class RiskTrend {
    @SerialName("improving")
    IMPROVING,

    @SerialName("stable")
    STABLE,

    @SerialName("worsening")
    WORSENING,
}


@Serializable
data class BatchPermissionAction(
    val actions: List<SinglePermissionAction>,
    val reason: String,
    val requiresConfirmation: Boolean = true,
)

@Serializable
data class SinglePermissionAction(
    val packageName: String,
    val permission: String,
    val action: PermissionActionType,
)

@Serializable
data class PermissionActionResult(
    val packageName: String,
    val permission: String,
    val action: PermissionActionType,
    val success: Boolean,
    val message: String? = null,
    val requiresUserAction: Boolean = false,
    val timestamp: Instant,
)
