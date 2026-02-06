package com.v7lthronyx.scamynx.ui.securityscore

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.domain.model.SecurityScoreReport
import com.v7lthronyx.scamynx.domain.service.SecurityScoreCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityScoreUiState(
    val isLoading: Boolean = true,
    val report: SecurityScoreReport? = null,
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class SecurityScoreViewModel @Inject constructor(
    private val securityScoreCalculator: SecurityScoreCalculator,
) : ViewModel() {

    private val _state = MutableStateFlow(SecurityScoreUiState())
    val state: StateFlow<SecurityScoreUiState> = _state.asStateFlow()

    init {
        calculateScore()
    }

    fun calculateScore() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorRes = null) }
            try {
                val report = securityScoreCalculator.calculateSecurityScore()
                _state.update { it.copy(isLoading = false, report = report) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorRes = R.string.security_score_error_generic,
                    )
                }
            }
        }
    }

    fun refresh() {
        calculateScore()
    }
}
