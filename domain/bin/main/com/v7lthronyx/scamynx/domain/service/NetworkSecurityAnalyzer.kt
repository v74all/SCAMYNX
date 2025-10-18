package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.NetworkReport

interface NetworkSecurityAnalyzer {
    suspend fun inspect(url: String): NetworkReport
}
