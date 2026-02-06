package com.v7lthronyx.scamynx.domain.repository

import com.v7lthronyx.scamynx.domain.model.ThreatFeedSyncResult

/**
 * Result of a threat lookup operation
 */
data class ThreatLookupResult(
    val isMalicious: Boolean,
    val threatType: String?,
    val sources: List<String>,
)

interface ThreatFeedRepository {
    suspend fun refresh(): ThreatFeedSyncResult
    
    /**
     * Sync threat feeds from remote sources
     */
    suspend fun syncThreatFeeds()
    
    /**
     * Lookup a URL in threat intelligence database
     */
    suspend fun lookupUrl(url: String): ThreatLookupResult
}
