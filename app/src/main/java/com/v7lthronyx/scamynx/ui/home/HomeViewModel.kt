package com.v7lthronyx.scamynx.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.data.util.ApiCredentials
import com.v7lthronyx.scamynx.domain.model.ScanRequest
import com.v7lthronyx.scamynx.domain.model.ScanStage
import com.v7lthronyx.scamynx.domain.model.ScanState
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.usecase.RunScanUseCase
import com.v7lthronyx.scamynx.work.ScanWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelectedFile(
    val name: String,
    val sizeBytes: Long?,
    val mimeType: String?,
    val base64: String,
)

data class ApiProviderStatus(
    val labelRes: Int,
    val isConfigured: Boolean,
)

data class HomeUiState(
    val selectedTarget: ScanTargetType = ScanTargetType.URL,
    val currentUrl: String = "",
    val vpnConfig: String = "",
    val vpnProfileLabel: String = "",
    val instagramHandle: String = "",
    val instagramDisplayName: String = "",
    val instagramFollowerCount: String = "",
    val instagramMessage: String = "",
    val instagramBio: String = "",
    val selectedFile: SelectedFile? = null,
    val isScanning: Boolean = false,
    val sessionId: String? = null,
    val errorResId: Int? = null,
    val progressPercent: Int = 0,
    val progressStage: ScanStage? = null,
    val progressMessage: String? = null,
    val providerStatuses: List<ApiProviderStatus> = emptyList(),
)

sealed class HomeEvent {
    data class ScanCompleted(val sessionId: String) : HomeEvent()
    data class ScanFailed(val message: String) : HomeEvent()
    object ScanCancelled : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val runScanUseCase: RunScanUseCase,
    private val scanWorkScheduler: ScanWorkScheduler,
    private val apiCredentials: ApiCredentials,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private var activeScanJob: Job? = null
    private var activeWorkId: UUID? = null

    private val stageProgressValues: Map<ScanStage, Int> = mapOf(
        ScanStage.INITIALIZING to 5,
        ScanStage.NORMALIZING to 12,
        ScanStage.FETCHING_THREAT_INTEL to 42,
        ScanStage.ANALYZING_NETWORK_SECURITY to 58,
        ScanStage.RUNNING_ML to 72,
        ScanStage.ANALYZING_FILE to 68,
        ScanStage.ANALYZING_VPN_CONFIG to 68,
        ScanStage.ANALYZING_INSTAGRAM to 68,
        ScanStage.AGGREGATING to 88,
        ScanStage.COMPLETED to 100,
        ScanStage.FAILED to 0,
    )

    init {
        refreshProviderStatuses()
    }

    fun onScanTargetChanged(targetType: ScanTargetType) {
        _state.update { it.copy(selectedTarget = targetType, errorResId = null) }
    }

    fun onUrlChanged(url: String) {
        _state.update { it.copy(currentUrl = url, errorResId = null) }
    }

    fun onVpnConfigChanged(config: String) {
        _state.update { it.copy(vpnConfig = config, errorResId = null) }
    }

    fun onVpnProfileLabelChanged(label: String) {
        _state.update { it.copy(vpnProfileLabel = label) }
    }

    fun onInstagramHandleChanged(handle: String) {
        _state.update { it.copy(instagramHandle = handle, errorResId = null) }
    }

    fun onInstagramDisplayNameChanged(displayName: String) {
        _state.update { it.copy(instagramDisplayName = displayName) }
    }

    fun onInstagramFollowersChanged(value: String) {
        _state.update { it.copy(instagramFollowerCount = value.filter { char -> char.isDigit() }) }
    }

    fun onInstagramMessageChanged(message: String) {
        _state.update { it.copy(instagramMessage = message) }
    }

    fun onInstagramBioChanged(bio: String) {
        _state.update { it.copy(instagramBio = bio) }
    }

    fun onFileSelected(file: SelectedFile) {
        _state.update { it.copy(selectedFile = file, errorResId = null) }
    }

    fun onFileCleared() {
        _state.update { it.copy(selectedFile = null) }
    }

    fun onScanRequested() {
        val sessionId = UUID.randomUUID().toString()
        val request = buildScanRequest() ?: return

        cancelActiveScan()
        val workId = scanWorkScheduler.enqueueScan(request)
        activeWorkId = workId

        _state.update {
            it.copy(
                isScanning = true,
                sessionId = sessionId,
                errorResId = null,
                progressPercent = 0,
                progressStage = null,
                progressMessage = null,
            )
        }
        updateProgress(ScanStage.INITIALIZING, null)

        activeScanJob = viewModelScope.launch {
            runCatching {
                runScanUseCase(request).collect { scanState ->
                    when (scanState) {
                        is ScanState.Progress -> updateProgress(scanState.stage, scanState.message)
                        is ScanState.Success -> {
                            activeWorkId = null
                            _state.update {
                                it.copy(
                                    isScanning = false,
                                    sessionId = scanState.result.sessionId,
                                    progressPercent = 0,
                                    progressStage = null,
                                    progressMessage = null,
                                )
                            }
                            _events.emit(HomeEvent.ScanCompleted(scanState.result.sessionId))
                        }
                        is ScanState.Failure -> handleScanFailure(scanState.throwable)
                    }
                }
            }.onFailure { throwable ->
                handleScanFailure(throwable)
            }
        }

        activeScanJob?.invokeOnCompletion {
            activeScanJob = null
        }
    }

    fun onCancelScan() {
        if (state.value.isScanning) {
            cancelActiveScan()
            _state.update {
                it.copy(
                    isScanning = false,
                    progressPercent = 0,
                    progressStage = null,
                    progressMessage = null,
                    errorResId = null,
                    sessionId = null,
                )
            }
            viewModelScope.launch {
                _events.emit(HomeEvent.ScanCancelled)
            }
        }
    }

    private fun buildScanRequest(): ScanRequest? {
        val currentState = state.value
        return when (currentState.selectedTarget) {
            ScanTargetType.URL -> {
                val url = currentState.currentUrl.trim()
                if (url.isBlank()) {
                    _state.update { it.copy(errorResId = R.string.home_error_empty_url) }
                    null
                } else {
                    ScanRequest(
                        targetType = ScanTargetType.URL,
                        rawInput = url,
                    )
                }
            }
            ScanTargetType.FILE -> {
                val file = currentState.selectedFile
                if (file == null) {
                    _state.update { it.copy(errorResId = R.string.home_error_missing_file) }
                    null
                } else {
                    val metadata = buildMap {
                        put("fileName", file.name)
                        file.mimeType?.let { put("mimeType", it) }
                        file.sizeBytes?.let { put("sizeBytes", it.toString()) }
                    }
                    ScanRequest(
                        targetType = ScanTargetType.FILE,
                        rawInput = file.base64,
                        metadata = metadata,
                    )
                }
            }
            ScanTargetType.VPN_CONFIG -> {
                val config = currentState.vpnConfig.trim()
                if (config.isBlank()) {
                    _state.update { it.copy(errorResId = R.string.home_error_empty_vpn_config) }
                    null
                } else {
                    val metadata = buildMap {
                        currentState.vpnProfileLabel.takeIf { it.isNotBlank() }?.let { put("profileName", it) }
                    }
                    ScanRequest(
                        targetType = ScanTargetType.VPN_CONFIG,
                        rawInput = config,
                        metadata = metadata,
                    )
                }
            }
            ScanTargetType.INSTAGRAM -> {
                val handle = currentState.instagramHandle.trim()
                if (handle.isBlank()) {
                    _state.update { it.copy(errorResId = R.string.home_error_empty_instagram_handle) }
                    null
                } else {
                    val metadata = buildMap {
                        currentState.instagramDisplayName.takeIf { it.isNotBlank() }?.let { put("displayName", it) }
                        currentState.instagramBio.takeIf { it.isNotBlank() }?.let { put("bio", it) }
                        currentState.instagramMessage.takeIf { it.isNotBlank() }?.let { put("message", it) }
                        currentState.instagramFollowerCount.takeIf { it.isNotBlank() }?.let { put("followerCount", it) }
                    }
                    ScanRequest(
                        targetType = ScanTargetType.INSTAGRAM,
                        rawInput = handle,
                        metadata = metadata,
                    )
                }
            }
        }
    }

    private fun updateProgress(stage: ScanStage, message: String?) {
        val progressValue = stageProgressValues[stage] ?: 0
        _state.update { current ->
            val coercedPercent = progressValue.coerceAtLeast(current.progressPercent).coerceIn(0, 100)
            current.copy(
                isScanning = true,
                progressStage = stage,
                progressPercent = coercedPercent,
                progressMessage = message,
            )
        }
    }

    private fun cancelActiveScan() {
        activeScanJob?.cancel()
        activeScanJob = null
        activeWorkId?.let { workId ->
            scanWorkScheduler.cancelScan(workId)
        }
        activeWorkId = null
    }

    private fun handleScanFailure(throwable: Throwable?) {
        activeWorkId = null
        val message = throwable?.message.orEmpty()
        _state.update {
            it.copy(
                isScanning = false,
                errorResId = R.string.home_error_scan_failed,
                progressPercent = 0,
                progressStage = null,
                progressMessage = message.ifBlank { null },
            )
        }
        viewModelScope.launch {
            _events.emit(HomeEvent.ScanFailed(message))
        }
    }

    private fun refreshProviderStatuses() {
        val statuses = listOf(
            ApiProviderStatus(
                labelRes = R.string.provider_virustotal,
                isConfigured = !apiCredentials.virusTotalApiKey.isNullOrBlank(),
            ),
            ApiProviderStatus(
                labelRes = R.string.provider_google_safe_browsing,
                isConfigured = !apiCredentials.googleSafeBrowsingApiKey.isNullOrBlank(),
            ),
            ApiProviderStatus(
                labelRes = R.string.provider_urlscan,
                isConfigured = !apiCredentials.urlScanApiKey.isNullOrBlank(),
            ),
        )
        _state.update { it.copy(providerStatuses = statuses) }
    }
}
