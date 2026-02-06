@file:Suppress("unused")

package com.v7lthronyx.scamynx.data.privacyradar.coordinator

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacySessionContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifecycleSessionContextProvider @Inject constructor() :
    SessionContextProvider,
    DefaultLifecycleObserver {

    private val processLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get()
    private val _sessionContexts = MutableStateFlow<PrivacySessionContext?>(null)

    override val sessionContexts: Flow<PrivacySessionContext?> = _sessionContexts.asStateFlow()

    init {
        processLifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        _sessionContexts.update { current ->
            val session = current ?: PrivacySessionContext(sessionId = newSessionId())
            session.copy(isScreenOn = true)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        _sessionContexts.update { current ->
            val session = current ?: PrivacySessionContext(sessionId = newSessionId())
            session.copy(isScreenOn = false)
        }
    }

    override fun current(): PrivacySessionContext? = _sessionContexts.value

    fun updateScreenContext(
        screenName: String?,
        activityClassName: String?,
    ) {
        _sessionContexts.update { current ->
            val sessionId = current?.sessionId ?: newSessionId()
            PrivacySessionContext(
                sessionId = sessionId,
                screenName = screenName,
                activityClassName = activityClassName,
                isScreenOn = current?.isScreenOn ?: true,
            )
        }
    }

    private fun newSessionId(): String = UUID.randomUUID().toString()
}
