package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.AntiPhishingAnalysis

interface AntiPhishingAnalyzer {
    suspend fun analyze(input: String): AntiPhishingAnalysis
}
