@file:Suppress("unused")

package com.v7lthronyx.scamynx.data.privacyradar.source.impl

import android.app.AppOpsManager
import android.content.Context
import androidx.core.content.ContextCompat
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEvent
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventSourceId
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyResourceType
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOpsPrivacyEventSource @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PrivacyEventSource {

    override val id: PrivacyEventSourceId = PrivacyEventSourceId("app_ops")
    override val supportedResources: Set<PrivacyResourceType> = setOf(
        PrivacyResourceType.CAMERA,
        PrivacyResourceType.MICROPHONE,
        PrivacyResourceType.LOCATION,
        PrivacyResourceType.CONTACTS,
        PrivacyResourceType.CLIPBOARD,
    )

    private val appOpsManager: AppOpsManager? = ContextCompat.getSystemService(
        context,
        AppOpsManager::class.java,
    )
    private var scope: CoroutineScope? = null
    private val _events = MutableSharedFlow<PrivacyEvent>(extraBufferCapacity = 32)
    private val _status = MutableStateFlow(PrivacySourceStatus.STOPPED)

    override val events: Flow<PrivacyEvent> = _events.asSharedFlow()
    override val status = _status.asStateFlow()

    override suspend fun start() {
        if (_status.value == PrivacySourceStatus.RUNNING) return
        _status.value = PrivacySourceStatus.STARTING
        if (appOpsManager == null) {
            _status.value = PrivacySourceStatus.ERROR
            return
        }
        scope = CoroutineScope(SupervisorJob() + dispatcher)
        
        _status.value = PrivacySourceStatus.RUNNING
    }

    override suspend fun stop() {
        scope?.cancel()
        scope = null
        _status.value = PrivacySourceStatus.STOPPED
    }
}
