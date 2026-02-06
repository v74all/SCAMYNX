package com.v7lthronyx.scamynx.ui.permissionaudit

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
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
import javax.inject.Inject

@HiltViewModel
class PermissionAuditViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionAuditUiState())
    val state: StateFlow<PermissionAuditUiState> = _state.asStateFlow()

    private val packageManager = context.packageManager

    private val dangerousPermissions = setOf(
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.GET_ACCOUNTS",
        "android.permission.READ_CALENDAR",
        "android.permission.WRITE_CALENDAR",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.CAMERA",
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.RECORD_AUDIO",
        "android.permission.READ_PHONE_STATE",
        "android.permission.CALL_PHONE",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.ANSWER_PHONE_CALLS",
        "android.permission.ADD_VOICEMAIL",
        "android.permission.USE_SIP",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_WAP_PUSH",
        "android.permission.RECEIVE_MMS",
        "android.permission.BODY_SENSORS",
        "android.permission.ACTIVITY_RECOGNITION",
    )

    private val permissionCategories = mapOf(
        "Location" to listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
        ),
        "Camera" to listOf("android.permission.CAMERA"),
        "Microphone" to listOf("android.permission.RECORD_AUDIO"),
        "Contacts" to listOf(
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.GET_ACCOUNTS",
        ),
        "Storage" to listOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
            "android.permission.READ_MEDIA_AUDIO",
        ),
        "Phone" to listOf(
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
        ),
        "SMS" to listOf(
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_SMS",
        ),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val apps = withContext(Dispatchers.IO) {
                analyzeInstalledApps()
            }

            val permissionStats = calculatePermissionStats(apps)
            val highRiskApps = apps.filter { it.riskLevel == RiskLevel.HIGH }
            val totalDangerous = apps.sumOf { app ->
                app.permissions.count { it.isDangerous }
            }
            val overallRiskScore = calculateOverallRisk(apps)

            _state.update { current ->
                current.copy(
                    isLoading = false,
                    overallRiskScore = overallRiskScore,
                    totalApps = apps.size,
                    dangerousPermissions = totalDangerous,
                    permissionStats = permissionStats,
                    highRiskApps = highRiskApps,
                    allApps = apps.filter { it.riskLevel != RiskLevel.HIGH },
                )
            }
        }
    }

    private fun analyzeInstalledApps(): List<AppPermissionUiModel> {
        val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }

        return installedApps
            .filter { packageInfo ->
                
                val isUserApp = packageInfo.applicationInfo?.let { appInfo ->
                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
                } ?: false
                isUserApp || hasDangerousPermissions(packageInfo)
            }
            .map { packageInfo ->
                analyzeApp(packageInfo)
            }
            .sortedByDescending { it.riskLevel.ordinal }
    }

    private fun hasDangerousPermissions(packageInfo: PackageInfo): Boolean {
        return packageInfo.requestedPermissions?.any { it in dangerousPermissions } ?: false
    }

    private fun analyzeApp(packageInfo: PackageInfo): AppPermissionUiModel {
        val appName = packageInfo.applicationInfo?.let { appInfo ->
            packageManager.getApplicationLabel(appInfo).toString()
        } ?: packageInfo.packageName

        val permissions = packageInfo.requestedPermissions?.map { permission ->
            val simpleName = permission.substringAfterLast(".")
                .replace("_", " ")
                .lowercase()
                .replaceFirstChar { it.uppercase() }

            PermissionUiModel(
                name = simpleName,
                isDangerous = permission in dangerousPermissions,
            )
        } ?: emptyList()

        val dangerousCount = permissions.count { it.isDangerous }
        val riskLevel = when {
            dangerousCount >= 5 -> RiskLevel.HIGH
            dangerousCount >= 2 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        return AppPermissionUiModel(
            packageName = packageInfo.packageName,
            name = appName,
            riskLevel = riskLevel,
            permissions = permissions,
        )
    }

    private fun calculatePermissionStats(apps: List<AppPermissionUiModel>): List<PermissionStat> {
        val totalApps = apps.size.coerceAtLeast(1)

        return permissionCategories.map { (category, permissions) ->
            val appsWithPermission = apps.count { app ->
                app.permissions.any { perm ->
                    permissions.any { it.endsWith(perm.name.uppercase().replace(" ", "_")) }
                }
            }

            PermissionStat(
                category = category,
                appCount = appsWithPermission,
                percentage = (appsWithPermission.toFloat() / totalApps) * 100,
            )
        }.sortedByDescending { it.appCount }
    }

    private fun calculateOverallRisk(apps: List<AppPermissionUiModel>): Int {
        if (apps.isEmpty()) return 0

        val highRiskCount = apps.count { it.riskLevel == RiskLevel.HIGH }
        val mediumRiskCount = apps.count { it.riskLevel == RiskLevel.MEDIUM }

        val riskScore = (highRiskCount * 3 + mediumRiskCount * 1).toFloat() / apps.size * 100
        return riskScore.toInt().coerceIn(0, 100)
    }

    fun toggleAppExpanded(packageName: String) {
        _state.update { current ->
            current.copy(
                expandedAppId = if (current.expandedAppId == packageName) null else packageName,
            )
        }
    }
}
