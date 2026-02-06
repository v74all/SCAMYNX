package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.SocialEngineeringReport

interface SocialEngineeringAnalyzer {
    suspend fun analyze(message: String): SocialEngineeringReport
}
