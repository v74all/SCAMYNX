package com.v7lthronyx.scamynx.data.privacyradar.source

import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEvent
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEventSourceId
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyResourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PrivacyEventSource {
    val id: PrivacyEventSourceId

    val supportedResources: Set<PrivacyResourceType>

    val events: Flow<PrivacyEvent>

    val status: StateFlow<PrivacySourceStatus>

    suspend fun start()

    suspend fun stop()
}

enum class PrivacySourceStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR,
}
