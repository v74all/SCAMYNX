@file:Suppress("unused")

package com.v7lthronyx.scamynx.data.privacyradar.source.impl

import android.content.ClipboardManager
import android.content.Context
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
import com.v7lthronyx.scamynx.data.privacyradar.util.ForegroundAppResolver
import com.v7lthronyx.scamynx.domain.model.LinkDisposition
import com.v7lthronyx.scamynx.domain.service.AntiPhishingAnalyzer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AntiPhishingLinkEventSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyzer: AntiPhishingAnalyzer,
    private val foregroundAppResolver: ForegroundAppResolver,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : PrivacyEventSource {

    override val id: PrivacyEventSourceId = PrivacyEventSourceId("anti_phishing_link")
    override val supportedResources: Set<PrivacyResourceType> = setOf(PrivacyResourceType.PHISHING_URL)

    private val clipboardManager: ClipboardManager? = ContextCompat.getSystemService(
        context,
        ClipboardManager::class.java,
    )
    private var scope: CoroutineScope? = null
    private val _events = MutableSharedFlow<PrivacyEvent>(extraBufferCapacity = 32)
    private val _status = MutableStateFlow(PrivacySourceStatus.STOPPED)
    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        scope?.launch { inspectClipboard() }
    }
    private val dedupeWindow = ArrayDeque<String>()
    private val dedupeSet = mutableSetOf<String>()
    private val urlPattern = Pattern.compile("((https?|ftp)://|www\\.)[^\\s]+", Pattern.CASE_INSENSITIVE)
    private val fallbackPackage = context.packageName

    override val events: Flow<PrivacyEvent> = _events.asSharedFlow()
    override val status = _status.asStateFlow()

    override suspend fun start() {
        if (_status.value == PrivacySourceStatus.RUNNING) return
        _status.value = PrivacySourceStatus.STARTING
        val manager = clipboardManager
        if (manager == null) {
            _status.value = PrivacySourceStatus.ERROR
            return
        }
        val newScope = CoroutineScope(SupervisorJob() + dispatcher)
        scope = newScope
        withContext(Dispatchers.Main.immediate) {
            manager.addPrimaryClipChangedListener(listener)
        }
        newScope.launch { inspectClipboard() }
        _status.value = PrivacySourceStatus.RUNNING
    }

    override suspend fun stop() {
        scope?.cancel()
        scope = null
        clipboardManager?.let { manager ->
            withContext(Dispatchers.Main.immediate) {
                manager.removePrimaryClipChangedListener(listener)
            }
        }
        synchronized(dedupeWindow) {
            dedupeWindow.clear()
            dedupeSet.clear()
        }
        _status.value = PrivacySourceStatus.STOPPED
    }

    private suspend fun inspectClipboard() {
        val text = clipboardManager?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: return
        val candidateUrl = extractUrl(text) ?: return
        val normalizedKey = candidateUrl.lowercase(Locale.US)
        if (!markAsSeen(normalizedKey)) return
        val analysis = analyzer.analyze(candidateUrl)
        if (analysis.disposition == LinkDisposition.SAFE) return
        val sourcePackage = foregroundAppResolver.resolveForegroundPackage()
        val metadata = mutableMapOf(
            PrivacyEventMetadataKeys.URL to analysis.url,
            PrivacyEventMetadataKeys.NORMALIZED_URL to analysis.normalizedUrl,
            PrivacyEventMetadataKeys.THREAT_SCORE to analysis.score.toString(),
            PrivacyEventMetadataKeys.THREAT_LEVEL to analysis.disposition.name.lowercase(Locale.US),
        )
        if (analysis.triggers.isNotEmpty()) {
            metadata[PrivacyEventMetadataKeys.THREAT_TRIGGERS] = analysis.triggers.joinToString(",")
        }
        if (analysis.reputationMatches.isNotEmpty()) {
            metadata[PrivacyEventMetadataKeys.REPUTATION_MATCHES] = analysis.reputationMatches.joinToString(",")
        }
        sourcePackage?.let {
            metadata[PrivacyEventMetadataKeys.SOURCE_PACKAGE] = it
        }
        val event = PrivacyEvent(
            packageName = sourcePackage ?: fallbackPackage,
            sourceId = id,
            type = PrivacyEventType.RESOURCE_ACCESS,
            resourceType = PrivacyResourceType.PHISHING_URL,
            timestamp = analysis.inspectedAt,
            visibilityContext = PrivacyVisibilityContext.UNKNOWN,
            confidence = PrivacySignalConfidence.DIRECT,
            priority = if (analysis.disposition == LinkDisposition.MALICIOUS) {
                PrivacyEventPriority.HIGH
            } else {
                PrivacyEventPriority.NORMAL
            },
            metadata = metadata,
        )
        _events.emit(event)
    }

    private fun extractUrl(text: String): String? {
        val matcher = urlPattern.matcher(text)
        if (!matcher.find()) return null
        val raw = matcher.group()?.trim() ?: return null
        return raw.takeUnless { it.isEmpty() }
    }

    private fun markAsSeen(key: String): Boolean = synchronized(dedupeWindow) {
        if (dedupeSet.contains(key)) return false
        dedupeSet.add(key)
        dedupeWindow.addLast(key)
        if (dedupeWindow.size > MAX_CACHE) {
            val oldest = dedupeWindow.removeFirst()
            dedupeSet.remove(oldest)
        }
        true
    }

    companion object {
        private const val MAX_CACHE = 32
    }
}
