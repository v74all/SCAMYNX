package com.v7lthronyx.scamynx.domain.service

import com.v7lthronyx.scamynx.domain.model.BreachCheckResult
import com.v7lthronyx.scamynx.domain.model.BreachMonitoringReport

interface BreachMonitoringService {
    suspend fun checkEmail(email: String): BreachCheckResult

    suspend fun checkPhoneNumber(phoneNumber: String): BreachCheckResult

    suspend fun checkUsername(username: String): BreachCheckResult

    suspend fun generateMonitoringReport(): BreachMonitoringReport

    suspend fun enableMonitoring(
        emails: List<String> = emptyList(),
        phoneNumbers: List<String> = emptyList(),
        usernames: List<String> = emptyList(),
    )

    suspend fun disableMonitoring()

    suspend fun isMonitoringEnabled(): Boolean
}
