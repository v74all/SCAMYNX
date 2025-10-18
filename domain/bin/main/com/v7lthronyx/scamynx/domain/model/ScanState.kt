package com.v7lthronyx.scamynx.domain.model

sealed class ScanState {
    data class Progress(
        val sessionId: String,
        val stage: ScanStage,
        val message: String? = null,
    ) : ScanState()

    data class Success(
        val sessionId: String,
        val result: ScanResult,
    ) : ScanState()

    data class Failure(
        val sessionId: String,
        val throwable: Throwable,
    ) : ScanState()
}
