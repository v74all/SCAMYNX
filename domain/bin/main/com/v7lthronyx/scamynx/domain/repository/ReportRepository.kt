package com.v7lthronyx.scamynx.domain.repository

import com.v7lthronyx.scamynx.domain.model.GeneratedReport
import com.v7lthronyx.scamynx.domain.model.ReportFormat
import com.v7lthronyx.scamynx.domain.model.ScanResult

interface ReportRepository {
    suspend fun generate(reportFormat: ReportFormat, result: ScanResult): GeneratedReport
}
