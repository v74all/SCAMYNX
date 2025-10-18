package com.v7lthronyx.scamynx.domain.repository

import com.v7lthronyx.scamynx.domain.model.ThreatFeedSyncResult

interface ThreatFeedRepository {
    suspend fun refresh(): ThreatFeedSyncResult
}
