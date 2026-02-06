package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.SecurityScoreReport

interface SecurityScoreCalculator {
    suspend fun calculateSecurityScore(): SecurityScoreReport
}
