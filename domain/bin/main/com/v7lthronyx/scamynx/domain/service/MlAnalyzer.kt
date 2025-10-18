package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.MlReport

interface MlAnalyzer {
    suspend fun evaluate(url: String, htmlSnapshot: String? = null): MlReport
}
