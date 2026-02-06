package com.v7lthronyx.scamynx.ui.breachmonitoring

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.domain.model.BreachCheckResult
import com.v7lthronyx.scamynx.domain.model.BreachMonitoringReport
import com.v7lthronyx.scamynx.domain.service.BreachMonitoringService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BreachMonitoringUiState(
    val isLoading: Boolean = false,
    val report: BreachMonitoringReport? = null,
    val checkingEmail: Boolean = false,
    val checkingPhone: Boolean = false,
    val emailInput: String = "",
    val phoneInput: String = "",
    val emailResult: BreachCheckResult? = null,
    val phoneResult: BreachCheckResult? = null,
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class BreachMonitoringViewModel @Inject constructor(
    private val breachMonitoringService: BreachMonitoringService,
) : ViewModel() {

    private val _state = MutableStateFlow(BreachMonitoringUiState())
    val state: StateFlow<BreachMonitoringUiState> = _state.asStateFlow()

    init {
        loadMonitoringReport()
    }

    fun loadMonitoringReport() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorRes = null) }
            try {
                val report = breachMonitoringService.generateMonitoringReport()
                _state.update { it.copy(isLoading = false, report = report) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorRes = R.string.breach_monitoring_error_generic,
                    )
                }
            }
        }
    }

    fun checkEmail(email: String) {
        if (email.isBlank()) {
            _state.update { it.copy(errorRes = R.string.breach_monitoring_error_empty_email) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(checkingEmail = true, errorRes = null) }
            try {
                val result = breachMonitoringService.checkEmail(email)
                _state.update {
                    it.copy(
                        checkingEmail = false,
                        emailResult = result,
                        emailInput = email,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        checkingEmail = false,
                        errorRes = R.string.breach_monitoring_error_check_failed,
                    )
                }
            }
        }
    }

    fun checkPhoneNumber(phone: String) {
        if (phone.isBlank()) {
            _state.update { it.copy(errorRes = R.string.breach_monitoring_error_empty_phone) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(checkingPhone = true, errorRes = null) }
            try {
                val result = breachMonitoringService.checkPhoneNumber(phone)
                _state.update {
                    it.copy(
                        checkingPhone = false,
                        phoneResult = result,
                        phoneInput = phone,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        checkingPhone = false,
                        errorRes = R.string.breach_monitoring_error_check_failed,
                    )
                }
            }
        }
    }

    fun enableMonitoring(emails: List<String>, phoneNumbers: List<String>) {
        viewModelScope.launch {
            try {
                breachMonitoringService.enableMonitoring(emails, phoneNumbers, emptyList())
                loadMonitoringReport()
            } catch (e: Exception) {
                _state.update {
                    it.copy(errorRes = R.string.breach_monitoring_error_enable_failed)
                }
            }
        }
    }
}
