package com.v7lthronyx.scamynx.ui.history

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.ScanResult
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

data class HistoryItemUi(
    val sessionId: String,
    val targetLabel: String,
    val targetType: ScanTargetType,
    val riskScore: Double,
    val riskCategory: RiskCategory,
    val createdAt: Instant,
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val items: List<HistoryItemUi> = emptyList(),
    @StringRes val errorMessageRes: Int? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        observeHistory()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessageRes = null) }
            runCatching { scanRepository.getHistory(limit = 200) }
                .onSuccess { results ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            items = results.map(::mapToHistoryItem),
                            errorMessageRes = null,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = R.string.history_error_generic,
                        )
                    }
                }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            scanRepository.observeHistory(limit = 200)
                .onStart {
                    _state.update { it.copy(isLoading = true, errorMessageRes = null) }
                }
                .catch {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = R.string.history_error_generic,
                        )
                    }
                }
                .collect { results ->
                    _state.value = HistoryUiState(
                        isLoading = false,
                        items = results.map(::mapToHistoryItem),
                        errorMessageRes = null,
                    )
                }
        }
    }

    private fun mapToHistoryItem(result: ScanResult): HistoryItemUi {
        return HistoryItemUi(
            sessionId = result.sessionId,
            targetLabel = result.targetLabel,
            targetType = result.targetType,
            riskScore = result.risk,
            riskCategory = result.primaryRiskCategory(),
            createdAt = result.createdAt,
        )
    }
}

private fun ScanResult.primaryRiskCategory(): RiskCategory {
    val topFromBreakdown = breakdown.categories
        .maxByOrNull { it.value }
        ?.takeIf { it.value >= 0.2 }
        ?.key

    if (topFromBreakdown != null) {
        return topFromBreakdown
    }

    return when {
        risk >= 4.5 -> RiskCategory.CRITICAL
        risk >= 3.5 -> RiskCategory.HIGH
        risk >= 2.0 -> RiskCategory.MEDIUM
        risk >= 1.0 -> RiskCategory.LOW
        else -> RiskCategory.MINIMAL
    }
}
