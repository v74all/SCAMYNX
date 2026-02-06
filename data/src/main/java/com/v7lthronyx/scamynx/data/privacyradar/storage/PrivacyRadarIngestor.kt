package com.v7lthronyx.scamynx.data.privacyradar.storage

import android.util.Log
import com.v7lthronyx.scamynx.data.db.PrivacyEventDao
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.di.ThreatIntelJson
import com.v7lthronyx.scamynx.data.privacyradar.coordinator.PrivacyRadarCoordinator
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyRadarIngestor @Inject constructor(
    private val coordinator: PrivacyRadarCoordinator,
    private val privacyEventDao: PrivacyEventDao,
    private val baselineRefresher: PrivacyBaselineRefresher,
    @ThreatIntelJson private val json: Json,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) {

    private val tag = "PrivacyRadarIngestor"
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val started = AtomicBoolean(false)
    private var ingestionJob: Job? = null

    fun start() {
        if (!started.compareAndSet(false, true)) return
        coordinator.start()
        ingestionJob = scope.launch {
            coordinator.timelineEvents.collect { event ->
                persistEvent(event)
            }
        }
    }

    suspend fun stop() {
        if (!started.compareAndSet(true, false)) return
        ingestionJob?.cancel()
        scope.coroutineContext.cancelChildren()
        coordinator.stop()
    }

    private suspend fun persistEvent(event: PrivacyEvent) {
        runCatching {
            privacyEventDao.insert(event.toEntity(json))
            baselineRefresher.track(event)
        }.onFailure { throwable ->
            Log.w(tag, "Failed to persist privacy event for ${event.packageName}", throwable)
        }
    }
}
