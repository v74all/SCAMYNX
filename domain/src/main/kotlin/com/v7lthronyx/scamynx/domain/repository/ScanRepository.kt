package com.v7lthronyx.scamynx.domain.repository

import com.v7lthronyx.scamynx.domain.model.ScanResult
import com.v7lthronyx.scamynx.domain.model.ScanState
import com.v7lthronyx.scamynx.domain.model.ScanRequest
import kotlinx.coroutines.flow.Flow

interface ScanRepository {
    fun analyze(request: ScanRequest): Flow<ScanState>
    fun observeHistory(limit: Int = 100): Flow<List<ScanResult>>
    suspend fun getHistory(limit: Int = 100, offset: Int = 0): List<ScanResult>
    suspend fun getScan(sessionId: String): ScanResult?
    suspend fun clearHistory()
}
