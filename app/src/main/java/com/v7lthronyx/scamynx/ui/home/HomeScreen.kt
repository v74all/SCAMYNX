package com.v7lthronyx.scamynx.ui.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.common.designsystem.ScamynxGradients
import com.v7lthronyx.scamynx.common.designsystem.ScamynxTheme
import com.v7lthronyx.scamynx.common.designsystem.spacing
import com.v7lthronyx.scamynx.domain.model.ScanStage
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToResults: (String) -> Unit,
    onNavigateToThreatIntel: () -> Unit = {},
    onNavigateToQRScanner: () -> Unit = {},
    onNavigateToNetworkMonitor: () -> Unit = {},
    onNavigateToPermissionAudit: () -> Unit = {},
    onNavigateToBreachMonitoring: () -> Unit = {},
    onNavigateToSecurityScore: () -> Unit = {},
    onNavigateToDeviceHardening: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ScanCompleted -> onNavigateToResults(event.sessionId)
                is HomeEvent.ScanFailed -> {
                    val fallback = context.getString(R.string.home_error_scan_failed)
                    snackbarHostState.showSnackbar(event.message.ifBlank { fallback })
                }
                HomeEvent.ScanCancelled -> {
                    snackbarHostState.showSnackbar(context.getString(R.string.home_status_cancelled))
                }
            }
        }
    }

    HomeScreen(
        state = state,
        onScanRequested = viewModel::onScanRequested,
        onCancelScan = viewModel::onCancelScan,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateToThreatIntel = onNavigateToThreatIntel,
        onNavigateToQRScanner = onNavigateToQRScanner,
        onNavigateToNetworkMonitor = onNavigateToNetworkMonitor,
        onNavigateToPermissionAudit = onNavigateToPermissionAudit,
        onNavigateToBreachMonitoring = onNavigateToBreachMonitoring,
        onNavigateToSecurityScore = onNavigateToSecurityScore,
        onNavigateToDeviceHardening = onNavigateToDeviceHardening,
        onScanTargetChanged = viewModel::onScanTargetChanged,
        onUrlChanged = viewModel::onUrlChanged,
        onVpnConfigChanged = viewModel::onVpnConfigChanged,
        onVpnProfileLabelChanged = viewModel::onVpnProfileLabelChanged,
        onInstagramHandleChanged = viewModel::onInstagramHandleChanged,
        onInstagramDisplayNameChanged = viewModel::onInstagramDisplayNameChanged,
        onInstagramFollowersChanged = viewModel::onInstagramFollowersChanged,
        onInstagramMessageChanged = viewModel::onInstagramMessageChanged,
        onInstagramBioChanged = viewModel::onInstagramBioChanged,
        onFileSelected = viewModel::onFileSelected,
        onFileCleared = viewModel::onFileCleared,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onScanRequested: () -> Unit,
    onCancelScan: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToThreatIntel: () -> Unit,
    onNavigateToQRScanner: () -> Unit,
    onNavigateToNetworkMonitor: () -> Unit,
    onNavigateToPermissionAudit: () -> Unit,
    onNavigateToBreachMonitoring: () -> Unit,
    onNavigateToSecurityScore: () -> Unit,
    onNavigateToDeviceHardening: () -> Unit,
    onScanTargetChanged: (ScanTargetType) -> Unit,
    onUrlChanged: (String) -> Unit,
    onVpnConfigChanged: (String) -> Unit,
    onVpnProfileLabelChanged: (String) -> Unit,
    onInstagramHandleChanged: (String) -> Unit,
    onInstagramDisplayNameChanged: (String) -> Unit,
    onInstagramFollowersChanged: (String) -> Unit,
    onInstagramMessageChanged: (String) -> Unit,
    onInstagramBioChanged: (String) -> Unit,
    onFileSelected: (SelectedFile) -> Unit,
    onFileCleared: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val result = runCatching { loadSelectedFile(context, uri) }
                result.onSuccess(onFileSelected)
                    .onFailure { throwable ->
                        val message = when (throwable) {
                            is FileTooLargeException -> context.getString(
                                R.string.home_error_file_too_large,
                                MAX_FILE_BYTES / (1024 * 1024),
                            )
                            else -> context.getString(R.string.home_error_file_access)
                        }
                        snackbarHostState.showSnackbar(message)
                    }
            }
        }
    }

    val isPrimaryActionEnabled = remember(state) {
        when (state.selectedTarget) {
            ScanTargetType.URL -> state.currentUrl.isNotBlank()
            ScanTargetType.FILE -> state.selectedFile != null
            ScanTargetType.VPN_CONFIG -> state.vpnConfig.isNotBlank()
            ScanTargetType.INSTAGRAM -> state.instagramHandle.isNotBlank()
        } && !state.isScanning
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.home_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Filled.AccessTime,
                            contentDescription = stringResource(id = R.string.cd_history),
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(id = R.string.cd_settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScamynxGradients.backdrop())
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = MaterialTheme.spacing.lg,
                    end = MaterialTheme.spacing.lg,
                    top = MaterialTheme.spacing.lg,
                    bottom = MaterialTheme.spacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
            ) {
                item {
                    HeroHeader(
                        isScanning = state.isScanning,
                        progressPercent = state.progressPercent,
                        progressStage = state.progressStage,
                        progressMessage = state.progressMessage,
                        selectedTarget = state.selectedTarget,
                        onScanRequested = onScanRequested,
                        onCancelScan = onCancelScan,
                        isPrimaryActionEnabled = isPrimaryActionEnabled,
                    )
                }
                if (state.providerStatuses.isNotEmpty()) {
                    item {
                        ApiStatusCard(statuses = state.providerStatuses)
                    }
                }
                item {
                    ScanModeSelector(
                        selected = state.selectedTarget,
                        onSelected = onScanTargetChanged,
                    )
                }
                item {
                    when (state.selectedTarget) {
                        ScanTargetType.URL -> UrlInputCard(
                            url = state.currentUrl,
                            onValueChange = onUrlChanged,
                            onScanRequested = onScanRequested,
                            isLoading = state.isScanning,
                        )
                        ScanTargetType.FILE -> FileInputCard(
                            selectedFile = state.selectedFile,
                            isLoading = state.isScanning,
                            onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                            onClearFile = onFileCleared,
                            onScanRequested = onScanRequested,
                        )
                        ScanTargetType.VPN_CONFIG -> VpnConfigInputCard(
                            vpnConfig = state.vpnConfig,
                            vpnProfileLabel = state.vpnProfileLabel,
                            onConfigChanged = onVpnConfigChanged,
                            onLabelChanged = onVpnProfileLabelChanged,
                            onScanRequested = onScanRequested,
                            isLoading = state.isScanning,
                        )
                        ScanTargetType.INSTAGRAM -> InstagramInputCard(
                            state = state,
                            onHandleChanged = onInstagramHandleChanged,
                            onDisplayNameChanged = onInstagramDisplayNameChanged,
                            onFollowersChanged = onInstagramFollowersChanged,
                            onMessageChanged = onInstagramMessageChanged,
                            onBioChanged = onInstagramBioChanged,
                            onScanRequested = onScanRequested,
                            isLoading = state.isScanning,
                        )
                    }
                }
                state.errorResId?.let { messageRes ->
                    item {
                        HomeErrorBanner(message = stringResource(id = messageRes))
                    }
                }
                item {
                    SecurityToolsCard(
                        onThreatIntel = onNavigateToThreatIntel,
                        onQRScanner = onNavigateToQRScanner,
                        onNetworkMonitor = onNavigateToNetworkMonitor,
                        onPermissionAudit = onNavigateToPermissionAudit,
                        onBreachMonitoring = onNavigateToBreachMonitoring,
                        onSecurityScore = onNavigateToSecurityScore,
                        onDeviceHardening = onNavigateToDeviceHardening,
                    )
                }
                item {
                    QuickActionsCard(
                        onScanRequested = onScanRequested,
                        onShowHistory = onNavigateToHistory,
                        onOpenSettings = onNavigateToSettings,
                        onOpenAbout = onNavigateToAbout,
                        isLoading = state.isScanning,
                        isScanEnabled = isPrimaryActionEnabled,
                    )
                }
                item {
                    FeatureHighlights()
                }
            }
        }
    }
}

// Component functions are defined in HomeComponents.kt

@Preview(name = "Home EN", showBackground = true)
@Composable
private fun HomeScreenPreviewEnglish() {
    ScamynxTheme {
        HomeScreen(
            state = HomeUiState(),
            onScanRequested = {},
            onCancelScan = {},
            onNavigateToHistory = {},
            onNavigateToSettings = {},
            onNavigateToAbout = {},
            onNavigateToThreatIntel = {},
            onNavigateToQRScanner = {},
            onNavigateToNetworkMonitor = {},
            onNavigateToPermissionAudit = {},
            onNavigateToBreachMonitoring = {},
            onNavigateToSecurityScore = {},
            onNavigateToDeviceHardening = {},
            onScanTargetChanged = {},
            onUrlChanged = {},
            onVpnConfigChanged = {},
            onVpnProfileLabelChanged = {},
            onInstagramHandleChanged = {},
            onInstagramDisplayNameChanged = {},
            onInstagramFollowersChanged = {},
            onInstagramMessageChanged = {},
            onInstagramBioChanged = {},
            onFileSelected = {},
            onFileCleared = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

// loadSelectedFile, formatSize, MAX_FILE_BYTES, and FileTooLargeException are defined in HomeComponents.kt
