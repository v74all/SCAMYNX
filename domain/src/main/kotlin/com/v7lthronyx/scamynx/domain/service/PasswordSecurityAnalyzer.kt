package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.PasswordSecurityReport

interface PasswordSecurityAnalyzer {
    suspend fun evaluate(password: String): PasswordSecurityReport
}
