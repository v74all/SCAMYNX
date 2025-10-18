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

@Composable
private fun HeroHeader(
    isScanning: Boolean,
    progressPercent: Int,
    progressStage: ScanStage?,
    progressMessage: String?,
    selectedTarget: ScanTargetType,
    onScanRequested: () -> Unit,
    onCancelScan: () -> Unit,
    isPrimaryActionEnabled: Boolean,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val gradient = ScamynxGradients.hero()
    val targetLabel = stringResource(
        id = when (selectedTarget) {
            ScanTargetType.URL -> R.string.home_target_url
            ScanTargetType.FILE -> R.string.home_target_file
            ScanTargetType.VPN_CONFIG -> R.string.home_target_vpn
            ScanTargetType.INSTAGRAM -> R.string.home_target_instagram
        },
    )
    val focusLabel = stringResource(id = R.string.home_target_focus, targetLabel)
    val statusTitle = stringResource(
        id = if (isScanning) {
            R.string.home_status_title_scanning
        } else {
            R.string.home_status_title_ready
        },
    )
    val statusSubtitle = stringResource(
        id = if (isScanning) {
            R.string.home_status_subtitle_scanning
        } else {
            R.string.home_status_subtitle_ready
        },
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(gradient),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.spacing.lg,
                        vertical = MaterialTheme.spacing.lg,
                    ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Filled.Bolt else Icons.Filled.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Crossfade(targetState = statusSubtitle, label = "heroSubtitle") { subtitle ->
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(text = focusLabel) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Insights, contentDescription = null)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
                AnimatedVisibility(visible = isScanning) {
                    val progressValue = remember(progressPercent) { progressPercent.coerceIn(0, 100) }
                    val stageLabel = progressStage?.let { stage ->
                        stringResource(
                            id = when (stage) {
                                ScanStage.INITIALIZING -> R.string.scan_stage_initializing
                                ScanStage.NORMALIZING -> R.string.scan_stage_normalizing
                                ScanStage.FETCHING_THREAT_INTEL -> R.string.scan_stage_fetching_intel
                                ScanStage.ANALYZING_NETWORK_SECURITY -> R.string.scan_stage_network
                                ScanStage.RUNNING_ML -> R.string.scan_stage_ml
                                ScanStage.ANALYZING_FILE -> R.string.scan_stage_file
                                ScanStage.ANALYZING_VPN_CONFIG -> R.string.scan_stage_vpn
                                ScanStage.ANALYZING_INSTAGRAM -> R.string.scan_stage_instagram
                                ScanStage.AGGREGATING -> R.string.scan_stage_aggregating
                                ScanStage.COMPLETED -> R.string.scan_stage_completed
                                ScanStage.FAILED -> R.string.scan_stage_failed
                            },
                        )
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        tonalElevation = 1.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = MaterialTheme.spacing.md,
                                    vertical = MaterialTheme.spacing.sm,
                                ),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                        ) {
                            LinearProgressIndicator(
                                progress = { progressValue / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(
                                        id = R.string.scan_progress_percent,
                                        progressValue,
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                stageLabel?.let { label ->
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            progressMessage?.takeIf { it.isNotBlank() }?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (isScanning) {
                    Button(
                        onClick = onCancelScan,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(imageVector = Icons.Filled.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        Text(text = stringResource(id = R.string.action_stop_scan))
                    }
                } else {
                    FilledTonalButton(
                        onClick = onScanRequested,
                        enabled = isPrimaryActionEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        Text(text = stringResource(id = R.string.action_scan_now))
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiStatusCard(statuses: List<ApiProviderStatus>) {
    if (statuses.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.lg,
                    vertical = MaterialTheme.spacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(id = R.string.home_api_status_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(id = R.string.home_api_status_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            statuses.forEach { status ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                ) {
                    val iconTint = if (status.isConfigured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                    Icon(
                        imageVector = if (status.isConfigured) Icons.Filled.Verified else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = iconTint,
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
                    ) {
                        Text(
                            text = stringResource(id = status.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = stringResource(
                                id = if (status.isConfigured) {
                                    R.string.home_api_status_ready
                                } else {
                                    R.string.home_api_status_missing
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.isConfigured) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanModeSelector(
    selected: ScanTargetType,
    onSelected: (ScanTargetType) -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), shape)
                .padding(
                    horizontal = MaterialTheme.spacing.lg,
                    vertical = MaterialTheme.spacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                Text(
                    text = stringResource(id = R.string.home_scan_modes_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(id = R.string.home_scan_modes_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                val items = listOf(
                    ScanTargetType.URL to R.string.home_target_url,
                    ScanTargetType.FILE to R.string.home_target_file,
                    ScanTargetType.VPN_CONFIG to R.string.home_target_vpn,
                    ScanTargetType.INSTAGRAM to R.string.home_target_instagram,
                )
                items(items) { (type, labelRes) ->
                    FilterChip(
                        selected = selected == type,
                        onClick = { onSelected(type) },
                        label = { Text(text = stringResource(id = labelRes)) },
                        leadingIcon = {
                            when (type) {
                                ScanTargetType.URL -> Icon(imageVector = Icons.Filled.Language, contentDescription = null)
                                ScanTargetType.FILE -> Icon(imageVector = Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null)
                                ScanTargetType.VPN_CONFIG -> Icon(imageVector = Icons.Filled.Description, contentDescription = null)
                                ScanTargetType.INSTAGRAM -> Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun UrlInputCard(
    url: String,
    onValueChange: (String) -> Unit,
    onScanRequested: () -> Unit,
    isLoading: Boolean,
) {
    val urlInputDescription = stringResource(id = R.string.cd_url_input)
    val shape = MaterialTheme.shapes.large

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = shape,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), shape)
                .padding(
                    horizontal = MaterialTheme.spacing.lg,
                    vertical = MaterialTheme.spacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                text = stringResource(id = R.string.home_prompt),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = urlInputDescription
                    },
                value = url,
                onValueChange = onValueChange,
                singleLine = true,
                label = { Text(text = stringResource(id = R.string.home_url_label)) },
                placeholder = { Text(text = stringResource(id = R.string.home_url_placeholder)) },
            )
            FilledTonalButton(
                onClick = onScanRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(18.dp)
                            .width(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(imageVector = Icons.Filled.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                    Text(text = stringResource(id = R.string.action_scan_now))
                }
            }
        }
    }
}

@Composable
private fun FileInputCard(
    selectedFile: SelectedFile?,
    isLoading: Boolean,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit,
    onScanRequested: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = shape,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), shape)
                .padding(
                    horizontal = MaterialTheme.spacing.lg,
                    vertical = MaterialTheme.spacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                text = stringResource(id = R.string.home_file_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(id = R.string.home_file_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onPickFile,
                enabled = !isLoading,
            ) {
                Text(text = stringResource(id = R.string.home_file_select))
            }
            selectedFile?.let { file ->
                FileDetails(file = file, onClear = onClearFile)
            }
            FilledTonalButton(
                onClick = onScanRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedFile != null && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(18.dp)
                            .width(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(imageVector = Icons.Filled.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                    Text(text = stringResource(id = R.string.action_scan_now))
                }
            }
        }
    }
}

@Composable
private fun FileDetails(
    file: SelectedFile,
    onClear: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(id = R.string.home_file_selected, file.name),
            style = MaterialTheme.typography.bodyLarge,
        )
        val formattedSize = formatSize(file.sizeBytes ?: 0L)
        Text(
            text = stringResource(id = R.string.home_file_size, formattedSize),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        file.mimeType?.let {
            Text(
                text = stringResource(id = R.string.home_file_mime, it),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onClear) {
            Text(text = stringResource(id = R.string.home_file_clear))
        }
    }
}

@Composable
private fun VpnConfigInputCard(
    vpnConfig: String,
    vpnProfileLabel: String,
    onConfigChanged: (String) -> Unit,
    onLabelChanged: (String) -> Unit,
    onScanRequested: () -> Unit,
    isLoading: Boolean,
) {
    val shape = MaterialTheme.shapes.large
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = shape,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), shape)
                .padding(
                    horizontal = MaterialTheme.spacing.lg,
                    vertical = MaterialTheme.spacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                text = stringResource(id = R.string.home_vpn_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = vpnProfileLabel,
                onValueChange = onLabelChanged,
                singleLine = true,
                label = { Text(text = stringResource(id = R.string.home_vpn_label)) },
                placeholder = { Text(text = stringResource(id = R.string.home_vpn_label_placeholder)) },
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                value = vpnConfig,
                onValueChange = onConfigChanged,
                label = { Text(text = stringResource(id = R.string.home_vpn_config_label)) },
                placeholder = { Text(text = stringResource(id = R.string.home_vpn_config_placeholder)) },
                maxLines = 8,
            )
            FilledTonalButton(
                onClick = onScanRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = vpnConfig.isNotBlank() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(18.dp)
                            .width(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(imageVector = Icons.Filled.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                    Text(text = stringResource(id = R.string.action_scan_now))
                }
            }
        }
    }
}

@Composable
private fun InstagramInputCard(
    state: HomeUiState,
    onHandleChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onFollowersChanged: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
    onBioChanged: (String) -> Unit,
    onScanRequested: () -> Unit,
    isLoading: Boolean,
) {
    val shape = MaterialTheme.shapes.large
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = shape,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), shape)
                .padding(
                    horizontal = MaterialTheme.spacing.lg,
                    vertical = MaterialTheme.spacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                text = stringResource(id = R.string.home_instagram_title),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.instagramHandle,
                onValueChange = onHandleChanged,
                singleLine = true,
                label = { Text(text = stringResource(id = R.string.home_instagram_handle_label)) },
                placeholder = { Text(text = stringResource(id = R.string.home_instagram_handle_placeholder)) },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.instagramDisplayName,
                onValueChange = onDisplayNameChanged,
                singleLine = true,
                label = { Text(text = stringResource(id = R.string.home_instagram_display_name)) },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.instagramFollowerCount,
                onValueChange = onFollowersChanged,
                singleLine = true,
                label = { Text(text = stringResource(id = R.string.home_instagram_followers)) },
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.instagramMessage,
                onValueChange = onMessageChanged,
                label = { Text(text = stringResource(id = R.string.home_instagram_message)) },
                placeholder = { Text(text = stringResource(id = R.string.home_instagram_message_placeholder)) },
                maxLines = 3,
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.instagramBio,
                onValueChange = onBioChanged,
                label = { Text(text = stringResource(id = R.string.home_instagram_bio)) },
                maxLines = 3,
            )
            FilledTonalButton(
                onClick = onScanRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.instagramHandle.isNotBlank() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(18.dp)
                            .width(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(imageVector = Icons.Filled.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                    Text(text = stringResource(id = R.string.action_scan_now))
                }
            }
        }
    }
}

@Composable
private fun HomeErrorBanner(message: String) {
    val shape = MaterialTheme.shapes.large
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f), shape)
                .padding(
                    horizontal = MaterialTheme.spacing.lg,
                    vertical = MaterialTheme.spacing.md,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

private data class HomeFeatureHighlight(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
)

@Composable
private fun FeatureHighlights() {
    val features = remember {
        listOf(
            HomeFeatureHighlight(
                icon = Icons.Filled.Verified,
                titleRes = R.string.home_feature_ai_title,
                bodyRes = R.string.home_feature_ai_body,
            ),
            HomeFeatureHighlight(
                icon = Icons.Filled.AutoAwesome,
                titleRes = R.string.home_feature_playbook_title,
                bodyRes = R.string.home_feature_playbook_body,
            ),
            HomeFeatureHighlight(
                icon = Icons.Filled.Insights,
                titleRes = R.string.home_feature_insights_title,
                bodyRes = R.string.home_feature_insights_body,
            ),
        )
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Text(
            text = stringResource(id = R.string.home_features_title),
            style = MaterialTheme.typography.titleMedium,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            items(features) { feature ->
                FeatureCard(feature = feature)
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: HomeFeatureHighlight) {
    val shape = MaterialTheme.shapes.large
    Card(
        modifier = Modifier
            .width(260.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), shape)
                .padding(
                    horizontal = MaterialTheme.spacing.md,
                    vertical = MaterialTheme.spacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(id = feature.titleRes),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(id = feature.bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionsCard(
    onScanRequested: () -> Unit,
    onShowHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    isLoading: Boolean,
    isScanEnabled: Boolean,
) {
    val shape = MaterialTheme.shapes.large
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), shape)
                .padding(
                    horizontal = MaterialTheme.spacing.lg,
                    vertical = MaterialTheme.spacing.md,
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(id = R.string.home_quick_actions),
                style = MaterialTheme.typography.titleMedium,
            )
            FilledTonalButton(
                onClick = onScanRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = isScanEnabled,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(18.dp)
                            .width(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(imageVector = Icons.Filled.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                    Text(text = stringResource(id = R.string.action_scan_now))
                }
            }
            Button(onClick = onShowHistory, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.action_view_history))
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.action_open_settings))
            }
            OutlinedButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.action_open_about))
            }
        }
    }
}

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

private suspend fun loadSelectedFile(context: Context, uri: Uri): SelectedFile = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    var displayName: String? = null
    var size: Long? = null
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                displayName = cursor.getString(nameIndex)
            }
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0) {
                size = cursor.getLong(sizeIndex)
            }
        }
    }
    val mimeType = resolver.getType(uri)
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IOException("empty file")
    if (bytes.isEmpty()) throw IOException("empty file")
    if (bytes.size > MAX_FILE_BYTES) throw FileTooLargeException(bytes.size.toLong())
    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    SelectedFile(
        name = displayName ?: context.getString(R.string.home_file_unknown_name),
        sizeBytes = size ?: bytes.size.toLong(),
        mimeType = mimeType,
        base64 = base64,
    )
}

private fun formatSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = sizeBytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 1 }
    return "${formatter.format(size)} ${units[unitIndex]}"
}

private const val MAX_FILE_BYTES = 5 * 1024 * 1024

private class FileTooLargeException(val length: Long) : Exception()
