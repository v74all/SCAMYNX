package com.v7lthronyx.scamynx.ui.results

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.domain.model.GeneratedReport
import com.v7lthronyx.scamynx.domain.model.ReportFormat
import com.v7lthronyx.scamynx.domain.model.ScanResult
import com.v7lthronyx.scamynx.domain.repository.ReportRepository
import com.v7lthronyx.scamynx.domain.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val RESULTS_SESSION_ID_KEY = "sessionId"

data class ResultsUiState(
    val isLoading: Boolean = true,
    val scanResult: ScanResult? = null,
    val isExporting: Boolean = false,
    val lastExport: GeneratedReport? = null,
    @StringRes val errorRes: Int? = null,
    @StringRes val exportErrorRes: Int? = null,
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    private val reportRepository: ReportRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle[RESULTS_SESSION_ID_KEY])

    private val _state = MutableStateFlow(ResultsUiState())
    val state: StateFlow<ResultsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorRes = null) }
            val result = scanRepository.getScan(sessionId)
            if (result == null) {
                _state.update { it.copy(isLoading = false, errorRes = R.string.results_error_not_found) }
            } else {
                _state.update { it.copy(isLoading = false, scanResult = result, errorRes = null) }
            }
        }
    }

    fun export(format: ReportFormat) {
        val result = _state.value.scanResult ?: return
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, exportErrorRes = null) }
            runCatching { reportRepository.generate(format, result) }
                .onSuccess { report ->
                    _state.update { it.copy(isExporting = false, lastExport = report, exportErrorRes = null) }
                }
                .onFailure {
                    _state.update { it.copy(isExporting = false, exportErrorRes = R.string.results_export_failed) }
                }
        }
    }

    fun clearExportStatus() {
        _state.update { it.copy(lastExport = null, exportErrorRes = null) }
    }
}
