package com.v7lthronyx.scamynx.data.util

import com.v7lthronyx.scamynx.domain.model.ScanResult
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanResultCache @Inject constructor() {

    private val cache = ConcurrentHashMap<String, CachedResult>()
    private val mutex = Mutex()

    private val MAX_CACHE_SIZE = 100

    private val CACHE_EXPIRATION_MS = 15 * 60 * 1000L

    private var hitCount = 0L
    private var missCount = 0L
    private var evictionCount = 0L

    suspend fun get(targetType: ScanTargetType, targetKey: String): ScanResult? = mutex.withLock {
        val cacheKey = buildCacheKey(targetType, targetKey)
        val cached = cache[cacheKey] ?: run {
            missCount++
            return@withLock null
        }

        if (System.currentTimeMillis() - cached.timestamp > CACHE_EXPIRATION_MS) {
            cache.remove(cacheKey)
            missCount++
            return@withLock null
        }

        hitCount++
        return@withLock cached.result
    }

    suspend fun put(result: ScanResult) = mutex.withLock {
        val targetKey = when (result.targetType) {
            ScanTargetType.URL -> result.normalizedUrl ?: result.targetLabel
            ScanTargetType.FILE -> result.targetLabel
            ScanTargetType.VPN_CONFIG -> result.targetLabel
            ScanTargetType.INSTAGRAM -> result.targetLabel
        }

        val cacheKey = buildCacheKey(result.targetType, targetKey)

        if (cache.size >= MAX_CACHE_SIZE) {
            evictOldest()
            evictionCount++
        }

        cache[cacheKey] = CachedResult(
            result = result,
            timestamp = System.currentTimeMillis(),
        )
    }

    suspend fun contains(targetType: ScanTargetType, targetKey: String): Boolean = mutex.withLock {
        val cacheKey = buildCacheKey(targetType, targetKey)
        val cached = cache[cacheKey] ?: return@withLock false

        if (System.currentTimeMillis() - cached.timestamp > CACHE_EXPIRATION_MS) {
            cache.remove(cacheKey)
            return@withLock false
        }

        return@withLock true
    }

    suspend fun clear() = mutex.withLock {
        cache.clear()
    }

    suspend fun remove(targetType: ScanTargetType, targetKey: String): Unit = mutex.withLock {
        val cacheKey = buildCacheKey(targetType, targetKey)
        cache.remove(cacheKey)
    }

    suspend fun getStats(): CacheStats = mutex.withLock {
        val now = System.currentTimeMillis()
        val validEntries = cache.values.count { now - it.timestamp <= CACHE_EXPIRATION_MS }
        val expiredEntries = cache.size - validEntries

        if (expiredEntries > 0) {
            cache.entries.removeAll { (_, cached) ->
                now - cached.timestamp > CACHE_EXPIRATION_MS
            }
        }

        CacheStats(
            totalEntries = cache.size,
            validEntries = validEntries,
            expiredEntries = expiredEntries,
        )
    }

    private fun buildCacheKey(targetType: ScanTargetType, targetKey: String): String {
        return "${targetType.name}:${targetKey.lowercase().trim()}"
    }

    private fun evictOldest() {
        if (cache.isEmpty()) return

        val oldest = cache.entries.minByOrNull { it.value.timestamp }
        oldest?.let { cache.remove(it.key) }
    }

    private data class CachedResult(
        val result: ScanResult,
        val timestamp: Long,
    )

    fun getDetailedStats(): DetailedCacheStats {
        val now = System.currentTimeMillis()
        val validEntries = cache.values.count { now - it.timestamp <= CACHE_EXPIRATION_MS }
        val expiredEntries = cache.size - validEntries

        if (expiredEntries > 0) {
            cache.entries.removeAll { (_, cached) ->
                now - cached.timestamp > CACHE_EXPIRATION_MS
            }
        }

        val totalRequests = hitCount + missCount
        val hitRate = if (totalRequests > 0) hitCount.toDouble() / totalRequests else 0.0

        return DetailedCacheStats(
            totalEntries = cache.size,
            validEntries = validEntries,
            expiredEntries = expiredEntries,
            hitCount = hitCount,
            missCount = missCount,
            hitRate = hitRate,
            evictionCount = evictionCount,
        )
    }

    suspend fun resetMetrics() = mutex.withLock {
        hitCount = 0L
        missCount = 0L
        evictionCount = 0L
    }

    data class CacheStats(
        val totalEntries: Int,
        val validEntries: Int,
        val expiredEntries: Int,
    )

    data class DetailedCacheStats(
        val totalEntries: Int,
        val validEntries: Int,
        val expiredEntries: Int,
        val hitCount: Long,
        val missCount: Long,
        val hitRate: Double,
        val evictionCount: Long,
    )
}
