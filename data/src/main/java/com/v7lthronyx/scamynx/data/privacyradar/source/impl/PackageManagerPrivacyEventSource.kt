@file:Suppress("unused")

package com.v7lthronyx.scamynx.data.privacyradar.source.impl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEvent
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventMetadataKeys
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventSourceId
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventType
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyResourceType
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyVisibilityContext
import com.v7lthronyx.scamynx.data.privacyradar.source.PrivacyEventSource
import com.v7lthronyx.scamynx.data.privacyradar.source.PrivacySourceStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageManagerPrivacyEventSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PrivacyEventSource {

    override val id: PrivacyEventSourceId = PrivacyEventSourceId("package_manager")
    override val supportedResources: Set<PrivacyResourceType> = setOf(PrivacyResourceType.PERMISSION)

    private val packageManager: PackageManager = context.packageManager
    private var scope: CoroutineScope? = null
    private val _events = MutableSharedFlow<PrivacyEvent>(
        extraBufferCapacity = 128,
    )
    private val _status = MutableStateFlow(PrivacySourceStatus.STOPPED)
    private val snapshot = mutableMapOf<PermissionKey, Boolean>()
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val packageName = intent?.data?.schemeSpecificPart
            scope?.launch { refreshSnapshot(packageName) }
        }
    }
    private var receiverRegistered = false
    private val clock = Clock.System

    override val events: Flow<PrivacyEvent> = _events.asSharedFlow()
    override val status = _status.asStateFlow()

    override suspend fun start() {
        if (_status.value == PrivacySourceStatus.RUNNING) return
        _status.value = PrivacySourceStatus.STARTING
        scope = CoroutineScope(SupervisorJob() + dispatcher)
        registerReceiver()
        refreshSnapshot(targetPackage = null)
        _status.value = PrivacySourceStatus.RUNNING
    }

    override suspend fun stop() {
        scope?.cancel()
        scope = null
        unregisterReceiver()
        snapshot.clear()
        _status.value = PrivacySourceStatus.STOPPED
    }

    private suspend fun refreshSnapshot(targetPackage: String?) {
        val currentScope = scope ?: return
        withContext(currentScope.coroutineContext) {
            val packages = when {
                targetPackage.isNullOrBlank() -> packageManager.getInstalledPackagesCompat()
                else -> listOfNotNull(packageManager.getPackageInfoCompat(targetPackage))
            }
            packages.forEach { pkgInfo ->
                val permissions = pkgInfo.requestedPermissions ?: return@forEach
                val flags = pkgInfo.requestedPermissionsFlags ?: IntArray(permissions.size)
                permissions.forEachIndexed { index, permission ->
                    val granted = (flags.getOrNull(index) ?: 0) and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0
                    val key = PermissionKey(pkgInfo.packageName, permission)
                    val previous = snapshot[key]
                    if (previous == null || previous != granted) {
                        snapshot[key] = granted
                        emitPermissionEvent(
                            packageName = pkgInfo.packageName,
                            permissionName = permission,
                            granted = granted,
                        )
                    }
                }
            }
        }
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun unregisterReceiver() {
        if (!receiverRegistered) return
        runCatching { context.unregisterReceiver(receiver) }
        receiverRegistered = false
    }

    private fun emitPermissionEvent(
        packageName: String,
        permissionName: String,
        granted: Boolean,
    ) {
        val metadata = mapOf(
            PrivacyEventMetadataKeys.GRANT_STATE to if (granted) VALUE_GRANTED else VALUE_REVOKED,
            PrivacyEventMetadataKeys.PERMISSION_NAME to permissionName,
        )
        val event = PrivacyEvent(
            packageName = packageName,
            sourceId = id,
            type = PrivacyEventType.PERMISSION_DELTA,
            resourceType = PrivacyResourceType.PERMISSION,
            timestamp = clock.now(),
            visibilityContext = PrivacyVisibilityContext.UNKNOWN,
            metadata = metadata,
        )
        _events.tryEmit(event)
    }

    private fun PackageManager.getInstalledPackagesCompat(): List<PackageInfo> = if (Build.VERSION.SDK_INT >= 33) {
        getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getInstalledPackages(PackageManager.GET_PERMISSIONS)
    }

    private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        }
    }.getOrNull()

    private data class PermissionKey(
        val packageName: String,
        val permissionName: String,
    )

    companion object {
        private const val VALUE_GRANTED = "granted"
        private const val VALUE_REVOKED = "revoked"
    }
}
