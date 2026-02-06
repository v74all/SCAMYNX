package com.v7lthronyx.scamynx.data.privacyradar.coordinator

import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEvent
import com.v7lthronyx.scamynx.data.privacyradar.source.PrivacyEventSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPrivacyRadarCoordinator @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards PrivacyEventSource>,
    private val sessionContextProvider: SessionContextProvider,
    private val config: PrivacyRadarConfig,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : PrivacyRadarCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val started = AtomicBoolean(false)
    private val _hotLane = MutableSharedFlow<PrivacyEvent>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _timeline = MutableSharedFlow<PrivacyEvent>(
        replay = 0,
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _status = MutableStateFlow(PrivacyRadarStatus.STOPPED)

    override val hotLaneEvents: Flow<PrivacyEvent> = _hotLane.asSharedFlow()
    override val timelineEvents: Flow<PrivacyEvent> = _timeline.asSharedFlow()
    override val status = _status.asStateFlow()

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        _status.value = PrivacyRadarStatus.STARTING
        sources.forEach { source ->
            scope.launch {
                runCatching { source.start() }
                    .onFailure {
                    }
                source.events.collect { rawEvent ->
                    val sessionContext = rawEvent.sessionContext ?: sessionContextProvider.current()
                    val enriched = if (sessionContext == null) rawEvent else rawEvent.copy(sessionContext = sessionContext)
                    _timeline.emit(enriched)
                    if (enriched.resourceType in config.highPriorityResources) {
                        _hotLane.emit(enriched)
                    }
                }
            }
        }
        _status.value = PrivacyRadarStatus.RUNNING
    }

    override suspend fun stop() {
        if (!started.compareAndSet(true, false)) return
        sources.forEach { source ->
            runCatching { source.stop() }
        }
        scope.coroutineContext.cancelChildren()
        _status.value = PrivacyRadarStatus.STOPPED
    }
}
