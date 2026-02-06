package com.v7lthronyx.scamynx.ui.devicehardening

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.domain.model.DeviceHardeningReport
import com.v7lthronyx.scamynx.domain.model.HardeningAction
import com.v7lthronyx.scamynx.domain.model.HardeningActionResult
import com.v7lthronyx.scamynx.domain.service.DeviceHardeningService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceHardeningUiState(
    val isLoading: Boolean = true,
    val report: DeviceHardeningReport? = null,
    @StringRes val errorRes: Int? = null,
    val applyingActionId: String? = null,
    val lastActionResult: HardeningActionResult? = null,
)

@HiltViewModel
class DeviceHardeningViewModel @Inject constructor(
    private val deviceHardeningService: DeviceHardeningService,
) : ViewModel() {

    private val _state = MutableStateFlow(DeviceHardeningUiState())
    val state: StateFlow<DeviceHardeningUiState> = _state.asStateFlow()

    init {
        analyzeDevice()
    }

    fun analyzeDevice() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorRes = null) }
            try {
                val report = deviceHardeningService.analyzeDeviceState()
                _state.update { it.copy(isLoading = false, report = report) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorRes = R.string.device_hardening_error_generic,
                    )
                }
            }
        }
    }

    fun applyAction(action: HardeningAction) {
        viewModelScope.launch {
            _state.update { it.copy(applyingActionId = action.id) }
            try {
                val result = deviceHardeningService.applyHardeningAction(action)
                _state.update {
                    it.copy(
                        applyingActionId = null,
                        lastActionResult = result,
                    )
                }
                if (result.success) {
                    analyzeDevice()
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        applyingActionId = null,
                        errorRes = R.string.device_hardening_error_apply,
                    )
                }
            }
        }
    }

    fun applyAllRecommended() {
        viewModelScope.launch {
            val report = _state.value.report ?: return@launch
            _state.update { it.copy(applyingActionId = "all") }
            try {
                val results = deviceHardeningService.applyAllRecommended()
                _state.update { it.copy(applyingActionId = null) }
                analyzeDevice()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        applyingActionId = null,
                        errorRes = R.string.device_hardening_error_apply,
                    )
                }
            }
        }
    }
}
