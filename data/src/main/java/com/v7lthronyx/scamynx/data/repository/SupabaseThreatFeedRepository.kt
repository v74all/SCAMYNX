package com.v7lthronyx.scamynx.data.repository

import com.v7lthronyx.scamynx.data.db.ThreatFeedDao
import com.v7lthronyx.scamynx.data.db.ThreatIndicatorEntity
import com.v7lthronyx.scamynx.data.di.IoDispatcher
import com.v7lthronyx.scamynx.data.di.ThreatIntelJson
import com.v7lthronyx.scamynx.data.network.supabase.SupabaseThreatFeedService
import com.v7lthronyx.scamynx.domain.model.ThreatFeedSyncResult
import com.v7lthronyx.scamynx.domain.repository.ThreatFeedRepository
import com.v7lthronyx.scamynx.domain.repository.ThreatLookupResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class SupabaseThreatFeedRepository @Inject constructor(
    private val supabaseThreatFeedService: SupabaseThreatFeedService,
    private val threatFeedDao: ThreatFeedDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ThreatIntelJson private val json: Json,
) : ThreatFeedRepository {

    override suspend fun refresh(): ThreatFeedSyncResult = withContext(ioDispatcher) {
        val response = supabaseThreatFeedService.fetchLatest() ?: return@withContext ThreatFeedSyncResult(
            fetchedCount = 0,
            skipped = true,
            fetchedAt = Clock.System.now(),
        )

        if (response.indicators.isEmpty()) {
            return@withContext ThreatFeedSyncResult(
                fetchedCount = 0,
                skipped = false,
                fetchedAt = Clock.System.now(),
            )
        }

        val fetchedAt = parseInstant(response.generatedAt) ?: Clock.System.now()

        val tagSerializer = ListSerializer(String.serializer())

        val entities = response.indicators.map { dto ->
            ThreatIndicatorEntity(
                indicatorId = dto.id,
                url = dto.url,
                riskScore = dto.riskScore,
                tagsJson = runCatching { json.encodeToString(tagSerializer, dto.tags) }.getOrElse { "[]" },
                lastSeenEpochMillis = dto.lastSeen?.let(::parseInstant)?.let { it.toEpochMilliseconds() },
                source = dto.source,
                fetchedAtEpochMillis = fetchedAt.toEpochMilliseconds(),
            )
        }

        threatFeedDao.clearAll()
        threatFeedDao.insertAll(entities)

        ThreatFeedSyncResult(
            fetchedCount = entities.size,
            skipped = false,
            fetchedAt = fetchedAt,
        )
    }

    override suspend fun syncThreatFeeds() {
        refresh()
    }

    override suspend fun lookupUrl(url: String): ThreatLookupResult = withContext(ioDispatcher) {
        
        val normalizedUrl = normalizeUrl(url)
        val localMatches = threatFeedDao.findByUrl(normalizedUrl)
        val localMatch = localMatches.firstOrNull()

        if (localMatch != null) {
            return@withContext ThreatLookupResult(
                isMalicious = localMatch.riskScore >= 0.7,
                threatType = determineThreatType(localMatch.tagsJson),
                sources = listOf(localMatch.source ?: "Local Threat Feed"),
            )
        }

        val host = extractHost(url)
        if (host != null) {
            val hostMatches = threatFeedDao.findByHost(host, 1)
            val hostMatch = hostMatches.firstOrNull()
            if (hostMatch != null) {
                return@withContext ThreatLookupResult(
                    isMalicious = hostMatch.riskScore >= 0.7,
                    threatType = determineThreatType(hostMatch.tagsJson),
                    sources = listOf(hostMatch.source ?: "Local Threat Feed"),
                )
            }
        }

        ThreatLookupResult(
            isMalicious = false,
            threatType = null,
            sources = emptyList(),
        )
    }

    private fun extractHost(url: String): String? {
        return try {
            val normalized = normalizeUrl(url)
            normalized.substringBefore("/").substringBefore("?")
        } catch (e: Exception) {
            null
        }
    }

    private fun normalizeUrl(url: String): String {
        return url.lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .removeSuffix("/")
    }

    private fun determineThreatType(tagsJson: String?): String? {
        if (tagsJson.isNullOrBlank() || tagsJson == "[]") return "Unknown"
        return try {
            val tags = json.decodeFromString(ListSerializer(String.serializer()), tagsJson)
            tags.firstOrNull() ?: "Malware"
        } catch (e: Exception) {
            "Malware"
        }
    }

    private fun parseInstant(value: String?): Instant? = value
        ?.takeIf { it.isNotBlank() }
        ?.runCatching { Instant.parse(this) }
        ?.getOrNull()
        ?: value?.toDoubleOrNull()?.let { seconds ->
            runCatching { Instant.fromEpochMilliseconds((seconds * 1000).roundToLong()) }.getOrNull()
        }
}
