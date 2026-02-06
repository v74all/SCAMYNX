package com.v7lthronyx.scamynx.data.privacyradar.model

import kotlinx.datetime.Instant
import kotlin.time.Duration

data class PrivacyEvent(
    val packageName: String,
    val sourceId: PrivacyEventSourceId,
    val type: PrivacyEventType,
    val resourceType: PrivacyResourceType,
    val timestamp: Instant,
    val duration: Duration? = null,
    val visibilityContext: PrivacyVisibilityContext = PrivacyVisibilityContext.UNKNOWN,
    val sessionContext: PrivacySessionContext? = null,
    val confidence: PrivacySignalConfidence = PrivacySignalConfidence.UNKNOWN,
    val priority: PrivacyEventPriority = PrivacyEventPriority.NORMAL,
    val metadata: Map<String, String> = emptyMap(),
)

@JvmInline
value class PrivacyEventSourceId(val value: String)

enum class PrivacyEventType {
    PERMISSION_DELTA,
    RESOURCE_ACCESS,
    SENSOR_PRIVACY_TOGGLE,
    FOREGROUND_STATE,
    BASELINE_SNAPSHOT,
    UNKNOWN,
}

enum class PrivacyResourceType {
    PERMISSION,
    CAMERA,
    MICROPHONE,
    LOCATION,
    CONTACTS,
    CLIPBOARD,
    PHISHING_URL,
    WIFI_NETWORK,
    FOREGROUND_SERVICE,
    SENSOR_PRIVACY_SWITCH,
    OTHER,
}

enum class PrivacyVisibilityContext {
    FOREGROUND,
    BACKGROUND,
    SCREEN_OFF,
    UNKNOWN,
}

enum class PrivacySignalConfidence {
    DIRECT,
    INFERRED,
    LOW,
    UNKNOWN,
}

enum class PrivacyEventPriority {
    HIGH,
    NORMAL,
    LOW,
}

data class PrivacySessionContext(
    val sessionId: String? = null,
    val screenName: String? = null,
    val activityClassName: String? = null,
    val isScreenOn: Boolean = true,
)
