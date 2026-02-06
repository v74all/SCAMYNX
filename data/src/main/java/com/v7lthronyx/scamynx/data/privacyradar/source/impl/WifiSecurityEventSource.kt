@file:Suppress("unused")

package com.v7lthronyx.scamynx.data.privacyradar.source.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.ContextCompat
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEvent
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventMetadataKeys
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventPriority
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventSourceId
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventType
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyResourceType
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacySignalConfidence
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyVisibilityContext
import com.v7lthronyx.scamynx.data.privacyradar.source.PrivacyEventSource
import com.v7lthronyx.scamynx.data.privacyradar.source.PrivacySourceStatus
import com.v7lthronyx.scamynx.domain.service.WifiSecurityAnalyzer
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiSecurityEventSource @Inject constructor(
    @ApplicationContext context: Context,
    private val wifiSecurityAnalyzer: WifiSecurityAnalyzer,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PrivacyEventSource {

    override val id: PrivacyEventSourceId = PrivacyEventSourceId("wifi_security")
    override val supportedResources: Set<PrivacyResourceType> = setOf(PrivacyResourceType.WIFI_NETWORK)

    private val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
    private val appPackageName = context.packageName
    private var scope: CoroutineScope? = null
    private val _events = MutableSharedFlow<PrivacyEvent>(extraBufferCapacity = 16)
    private val _status = MutableStateFlow(PrivacySourceStatus.STOPPED)
    private val networkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope?.launch { emitAssessment() }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            scope?.launch { emitAssessment() }
        }
    }

    override val events: Flow<PrivacyEvent> = _events.asSharedFlow()
    override val status = _status.asStateFlow()

    override suspend fun start() {
        if (_status.value == PrivacySourceStatus.RUNNING) return
        _status.value = PrivacySourceStatus.STARTING
        val manager = connectivityManager
        if (manager == null) {
            _status.value = PrivacySourceStatus.ERROR
            return
        }
        val newScope = CoroutineScope(SupervisorJob() + dispatcher)
        scope = newScope
        runCatching { manager.registerNetworkCallback(networkRequest, networkCallback) }
            .onFailure {
                _status.value = PrivacySourceStatus.ERROR
                return
            }
        newScope.launch { emitAssessment() }
        _status.value = PrivacySourceStatus.RUNNING
    }

    override suspend fun stop() {
        scope?.cancel()
        scope = null
        runCatching { connectivityManager?.unregisterNetworkCallback(networkCallback) }
        _status.value = PrivacySourceStatus.STOPPED
    }

    private suspend fun emitAssessment() {
        val assessment = wifiSecurityAnalyzer.analyzeCurrentNetwork() ?: return
        val metadata = buildMap {
            assessment.ssid?.let { put(PrivacyEventMetadataKeys.WIFI_SSID, it) }
            assessment.bssid?.let { put(PrivacyEventMetadataKeys.WIFI_BSSID, it) }
            put(PrivacyEventMetadataKeys.WIFI_ENCRYPTION, assessment.encryptionType.name.lowercase())
            put(PrivacyEventMetadataKeys.WIFI_CAPTIVE_PORTAL, assessment.captivePortalSuspected.toString())
            if (assessment.arpMitmIndicators.isNotEmpty()) {
                put(PrivacyEventMetadataKeys.WIFI_ARP_INDICATORS, assessment.arpMitmIndicators.joinToString("|"))
            }
            if (assessment.recommendations.isNotEmpty()) {
                put(PrivacyEventMetadataKeys.WIFI_RECOMMENDATIONS, assessment.recommendations.joinToString("|"))
            }
            put(PrivacyEventMetadataKeys.THREAT_SCORE, assessment.riskScore.toString())
            put(PrivacyEventMetadataKeys.THREAT_LEVEL, assessment.riskCategory.name.lowercase())
        }
        val priority = if (assessment.riskScore >= 0.6) {
            PrivacyEventPriority.HIGH
        } else {
            PrivacyEventPriority.NORMAL
        }
        val event = PrivacyEvent(
            packageName = appPackageName,
            sourceId = id,
            type = PrivacyEventType.RESOURCE_ACCESS,
            resourceType = PrivacyResourceType.WIFI_NETWORK,
            timestamp = assessment.timestamp,
            visibilityContext = PrivacyVisibilityContext.UNKNOWN,
            confidence = PrivacySignalConfidence.DIRECT,
            priority = priority,
            metadata = metadata,
        )
        _events.emit(event)
    }
}
