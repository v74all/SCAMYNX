package com.v7lthronyx.scamynx.domain.usecase

import com.v7lthronyx.scamynx.domain.model.ScanRequest
import com.v7lthronyx.scamynx.domain.model.ScanState
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.repository.ScanRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class RunScanUseCase @Inject constructor(
    private val scanRepository: ScanRepository,
) {
    operator fun invoke(url: String): Flow<ScanState> = invoke(
        ScanRequest(
            targetType = ScanTargetType.URL,
            rawInput = url,
        ),
    )

    operator fun invoke(request: ScanRequest): Flow<ScanState> = scanRepository.analyze(request)
}
