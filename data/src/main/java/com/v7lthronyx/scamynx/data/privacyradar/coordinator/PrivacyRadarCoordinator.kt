package com.v7lthronyx.scamynx.data.privacyradar.coordinator

import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEvent
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyResourceType
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacySessionContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PrivacyRadarCoordinator {
    val hotLaneEvents: Flow<PrivacyEvent>

    val timelineEvents: Flow<PrivacyEvent>

    val status: StateFlow<PrivacyRadarStatus>

    fun start()

    suspend fun stop()
}

interface SessionContextProvider {
    val sessionContexts: Flow<PrivacySessionContext?>

    fun current(): PrivacySessionContext?
}

data class PrivacyRadarConfig(
    val highPriorityResources: Set<PrivacyResourceType>,
    val bufferCapacity: Int = 128,
)

enum class PrivacyRadarStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR,
}
