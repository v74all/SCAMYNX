package com.v7lthronyx.scamynx.data.privacyradar.storage

import com.v7lthronyx.scamynx.data.db.PrivacyEventEntity
import com.v7lthronyx.scamynx.data.privacyradar.model.PrivacyEvent
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal fun PrivacyEvent.toEntity(json: Json): PrivacyEventEntity {
    val metadataPayload = metadata.takeIf { it.isNotEmpty() }?.let {
        json.encodeToString(
            MapSerializer(String.serializer(), String.serializer()),
            it,
        )
    }
    val sessionContext = sessionContext
    return PrivacyEventEntity(
        packageName = packageName,
        sourceId = sourceId.value,
        eventType = type.name,
        resourceType = resourceType.name,
        timestampEpochMillis = timestamp.toEpochMilliseconds(),
        durationMs = duration?.inWholeMilliseconds,
        visibilityContext = visibilityContext.name,
        sessionId = sessionContext?.sessionId,
        screenName = sessionContext?.screenName,
        activityClassName = sessionContext?.activityClassName,
        confidence = confidence.name,
        priority = priority.name,
        metadataJson = metadataPayload,
    )
}
