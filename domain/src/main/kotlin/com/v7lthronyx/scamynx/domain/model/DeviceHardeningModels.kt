package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class HardeningAction(
    val id: String,
    val category: HardeningCategory,
    val title: String,
    val description: String,
    val currentState: HardeningState,
    val targetState: HardeningState,
    val impact: HardeningImpact,
    val reversible: Boolean,
    val requiresRoot: Boolean = false,
    val actionType: HardeningActionType,
)

@Serializable
enum class HardeningCategory {
    PRIVACY,
    NETWORK,
    APP_SECURITY,
    SYSTEM,
    DEVELOPER_OPTIONS,
}

@Serializable
enum class HardeningState {
    ENABLED,
    DISABLED,
    UNKNOWN,
    NOT_APPLICABLE,
}

@Serializable
enum class HardeningImpact {
    LOW,
    MEDIUM,
    HIGH,
}

@Serializable
enum class HardeningActionType {
    TOGGLE_SETTING,
    OPEN_SETTINGS,
    GRANT_PERMISSION,
    REVOKE_PERMISSION,
    INSTALL_APP,
    UNINSTALL_APP,
    CONFIGURE_VPN,
}

@Serializable
data class HardeningActionResult(
    val actionId: String,
    val success: Boolean,
    val message: String? = null,
    val requiresReboot: Boolean = false,
    @Serializable(with = InstantIso8601Serializer::class)
    val timestamp: Instant,
)

@Serializable
data class DeviceHardeningReport(
    val availableActions: List<HardeningAction>,
    val appliedActions: List<String>,
    val recommendedActions: List<HardeningAction>,
    val securityImprovement: Int,
    @Serializable(with = InstantIso8601Serializer::class)
    val timestamp: Instant,
)
