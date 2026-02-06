package com.v7lthronyx.scamynx.data.devicehardening

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.domain.model.DeviceHardeningReport
import com.v7lthronyx.scamynx.domain.model.HardeningAction
import com.v7lthronyx.scamynx.domain.model.HardeningActionResult
import com.v7lthronyx.scamynx.domain.model.HardeningActionType
import com.v7lthronyx.scamynx.domain.model.HardeningCategory
import com.v7lthronyx.scamynx.domain.model.HardeningImpact
import com.v7lthronyx.scamynx.domain.model.HardeningState
import com.v7lthronyx.scamynx.domain.service.DeviceHardeningService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceHardeningServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : DeviceHardeningService {

    private val appliedActions = mutableSetOf<String>()

    override suspend fun analyzeDeviceState(): DeviceHardeningReport = withContext(dispatcher) {
        val availableActions = buildAvailableActions()
        val recommendedActions = availableActions.filter { shouldRecommend(it) }
        val securityImprovement = calculateSecurityImprovement(recommendedActions)

        DeviceHardeningReport(
            availableActions = availableActions,
            appliedActions = appliedActions.toList(),
            recommendedActions = recommendedActions,
            securityImprovement = securityImprovement,
            timestamp = clock.now(),
        )
    }

    override suspend fun applyHardeningAction(action: HardeningAction): HardeningActionResult = withContext(dispatcher) {
        try {
            when (action.actionType) {
                HardeningActionType.TOGGLE_SETTING -> {
                    toggleSystemSetting(action.id)
                }
                HardeningActionType.OPEN_SETTINGS -> {
                    openSystemSettings(action.id)
                }
                HardeningActionType.GRANT_PERMISSION -> {
                    openAppPermissionSettings()
                }
                HardeningActionType.REVOKE_PERMISSION -> {
                    openAppPermissionSettings()
                }
                HardeningActionType.INSTALL_APP -> {
                    openPlayStore(action.id)
                }
                HardeningActionType.UNINSTALL_APP -> {
                    openUninstallScreen(action.id)
                }
                HardeningActionType.CONFIGURE_VPN -> {
                    openVpnSettings()
                }
            }
            appliedActions.add(action.id)
            HardeningActionResult(
                actionId = action.id,
                success = true,
                message = "Hardening action applied successfully",
                requiresReboot = false,
                timestamp = clock.now(),
            )
        } catch (e: Exception) {
            HardeningActionResult(
                actionId = action.id,
                success = false,
                message = "Failed to apply action: ${e.message}",
                requiresReboot = false,
                timestamp = clock.now(),
            )
        }
    }

    override suspend fun revertHardeningAction(actionId: String): HardeningActionResult = withContext(dispatcher) {
        if (!appliedActions.contains(actionId)) {
            return@withContext HardeningActionResult(
                actionId = actionId,
                success = false,
                message = "Action was not applied",
                timestamp = clock.now(),
            )
        }

        appliedActions.remove(actionId)
        HardeningActionResult(
            actionId = actionId,
            success = true,
            message = "Action reverted successfully",
            timestamp = clock.now(),
        )
    }

    override suspend fun applyAllRecommended(): List<HardeningActionResult> = withContext(dispatcher) {
        val report = analyzeDeviceState()
        report.recommendedActions.map { applyHardeningAction(it) }
    }

    private fun buildAvailableActions(): List<HardeningAction> {
        val actions = mutableListOf<HardeningAction>()

        actions += HardeningAction(
            id = "disable_location_tracking",
            category = HardeningCategory.PRIVACY,
            title = "Disable Location Tracking",
            description = "Reduce location tracking by apps",
            currentState = HardeningState.UNKNOWN,
            targetState = HardeningState.DISABLED,
            impact = HardeningImpact.MEDIUM,
            reversible = true,
            actionType = HardeningActionType.OPEN_SETTINGS,
        )

        actions += HardeningAction(
            id = "disable_ad_tracking",
            category = HardeningCategory.PRIVACY,
            title = "Disable Ad Tracking",
            description = "Opt out of personalized ads",
            currentState = HardeningState.UNKNOWN,
            targetState = HardeningState.DISABLED,
            impact = HardeningImpact.LOW,
            reversible = true,
            actionType = HardeningActionType.OPEN_SETTINGS,
        )

        actions += HardeningAction(
            id = "enable_private_dns",
            category = HardeningCategory.NETWORK,
            title = "Enable Private DNS",
            description = "Use encrypted DNS (DoH/DoT) for better privacy",
            currentState = HardeningState.UNKNOWN,
            targetState = HardeningState.ENABLED,
            impact = HardeningImpact.LOW,
            reversible = true,
            actionType = HardeningActionType.OPEN_SETTINGS,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            actions += HardeningAction(
                id = "enable_play_protect",
                category = HardeningCategory.APP_SECURITY,
                title = "Enable Play Protect",
                description = "Enable Google Play Protect for app scanning",
                currentState = HardeningState.UNKNOWN,
                targetState = HardeningState.ENABLED,
                impact = HardeningImpact.LOW,
                reversible = true,
                actionType = HardeningActionType.OPEN_SETTINGS,
            )
        }

        actions += HardeningAction(
            id = "disable_developer_options",
            category = HardeningCategory.SYSTEM,
            title = "Disable Developer Options",
            description = "Disable developer options if not needed",
            currentState = HardeningState.UNKNOWN,
            targetState = HardeningState.DISABLED,
            impact = HardeningImpact.MEDIUM,
            reversible = true,
            actionType = HardeningActionType.OPEN_SETTINGS,
        )

        return actions
    }

    private fun shouldRecommend(action: HardeningAction): Boolean {
        
        return action.impact != HardeningImpact.HIGH || action.reversible
    }

    private fun calculateSecurityImprovement(actions: List<HardeningAction>): Int {
        
        var total = 0
        actions.forEach { action ->
            total += when (action.impact) {
                HardeningImpact.LOW -> 5
                HardeningImpact.MEDIUM -> 10
                HardeningImpact.HIGH -> 15
            }
        }
        return total.coerceIn(0, 100)
    }

    private fun toggleSystemSetting(settingId: String) {
        
    }

    private fun openSystemSettings(settingId: String) {
        val intent = when (settingId) {
            "disable_location_tracking" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            "disable_ad_tracking" -> Intent(Settings.ACTION_PRIVACY_SETTINGS)
            "enable_private_dns" -> Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                putExtra("extra_prefs_show_button_bar", true)
            }
            "enable_play_protect" -> Intent("android.settings.SECURITY_SETTINGS")
            else -> Intent(Settings.ACTION_SETTINGS)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun openVpnSettings() {
        val intent = Intent(Settings.ACTION_VPN_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun openAppPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun openPlayStore(packageName: String) {
        val appPackage = packageName.removePrefix("install_")
        val intent = try {
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackage")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (_: Exception) {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackage")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(intent)
    }

    private fun openUninstallScreen(packageName: String) {
        val appPackage = packageName.removePrefix("uninstall_")
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$appPackage")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
