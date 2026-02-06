package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.WifiSecurityAssessment

interface WifiSecurityAnalyzer {
    suspend fun analyzeCurrentNetwork(): WifiSecurityAssessment?
}
