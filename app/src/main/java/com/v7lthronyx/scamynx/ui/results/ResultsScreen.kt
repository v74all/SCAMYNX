package com.v7lthronyx.scamynx.ui.results

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.common.designsystem.ScamynxColors
import com.v7lthronyx.scamynx.common.designsystem.ScamynxGradients
import com.v7lthronyx.scamynx.common.designsystem.spacing
import com.v7lthronyx.scamynx.domain.model.FileScanReport
import com.v7lthronyx.scamynx.domain.model.GeneratedReport
import com.v7lthronyx.scamynx.domain.model.InstagramScanReport
import com.v7lthronyx.scamynx.domain.model.IssueSeverity
import com.v7lthronyx.scamynx.domain.model.MlReport
import com.v7lthronyx.scamynx.domain.model.NetworkReport
import com.v7lthronyx.scamynx.domain.model.Provider
import com.v7lthronyx.scamynx.domain.model.ReportFormat
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.ScanIssue
import com.v7lthronyx.scamynx.domain.model.ScanResult
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.domain.model.VerdictStatus
import com.v7lthronyx.scamynx.domain.model.VendorVerdict
import com.v7lthronyx.scamynx.domain.model.VpnConfigReport
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.datetime.toJavaInstant
import kotlinx.coroutines.launch

@Composable
fun ResultsRoute(
    onBack: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val shareChooserTitle = stringResource(id = R.string.results_export_share_title)
    val noViewerMessage = stringResource(id = R.string.results_export_no_viewer)
    val noShareMessage = stringResource(id = R.string.results_export_no_target)

    val exportSuccessMessage = state.lastExport?.let {
        when (it.format) {
            ReportFormat.JSON -> stringResource(id = R.string.results_export_json_success)
            ReportFormat.PDF -> stringResource(id = R.string.results_export_pdf_success)
        }
    }
    val exportErrorMessage = state.exportErrorRes?.let { stringResource(id = it) }

    val shareReport: (GeneratedReport) -> Unit = remember(context, shareChooserTitle, noShareMessage) {
        { report ->
            val uri = Uri.parse(report.uri)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = report.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val handlers = context.packageManager.queryIntentActivities(sendIntent, 0)
            if (handlers.isEmpty()) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(noShareMessage)
                }
            } else {
                handlers.forEach { resolveInfo ->
                    context.grantUriPermission(
                        resolveInfo.activityInfo.packageName,
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                val chooser = Intent.createChooser(sendIntent, shareChooserTitle).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    ContextCompat.startActivity(context, chooser, null)
                } catch (error: ActivityNotFoundException) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(noShareMessage)
                    }
                }
            }
        }
    }

    val openReport: (GeneratedReport) -> Unit = remember(context, noViewerMessage) {
        { report ->
            val uri = Uri.parse(report.uri)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                data = uri
                type = report.mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val handlers = context.packageManager.queryIntentActivities(viewIntent, 0)
            if (handlers.isEmpty()) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(noViewerMessage)
                }
            } else {
                handlers.forEach { resolveInfo ->
                    context.grantUriPermission(
                        resolveInfo.activityInfo.packageName,
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
                try {
                    ContextCompat.startActivity(context, viewIntent, null)
                } catch (error: ActivityNotFoundException) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(noViewerMessage)
                    }
                }
            }
        }
    }

    LaunchedEffect(exportSuccessMessage) {
        exportSuccessMessage?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.clearExportStatus()
        }
    }

    LaunchedEffect(exportErrorMessage) {
        exportErrorMessage?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.clearExportStatus()
        }
    }

    ResultsScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onExportPdf = { viewModel.export(ReportFormat.PDF) },
        onExportJson = { viewModel.export(ReportFormat.JSON) },
        onShareReport = shareReport,
        onOpenReport = openReport,
        lastExport = state.lastExport,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    state: ResultsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onExportPdf: () -> Unit,
    onExportJson: () -> Unit,
    onShareReport: (GeneratedReport) -> Unit,
    onOpenReport: (GeneratedReport) -> Unit,
    lastExport: GeneratedReport?,
    snackbarHostState: SnackbarHostState,
) {
    val result = state.scanResult
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.results_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.cd_navigate_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingState(modifier = Modifier.padding(paddingValues))
            state.errorRes != null -> ErrorState(
                messageRes = state.errorRes,
                onRetry = onRefresh,
                modifier = Modifier.padding(paddingValues),
            )
            result != null -> ResultContent(
                result = result,
                isExporting = state.isExporting,
                onExportPdf = onExportPdf,
                onExportJson = onExportJson,
                onShareReport = onShareReport,
                onOpenReport = onOpenReport,
                lastExport = lastExport,
                dateFormatter = dateFormatter,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun ResultContent(
    result: ScanResult,
    isExporting: Boolean,
    onExportPdf: () -> Unit,
    onExportJson: () -> Unit,
    onShareReport: (GeneratedReport) -> Unit,
    onOpenReport: (GeneratedReport) -> Unit,
    lastExport: GeneratedReport?,
    dateFormatter: DateTimeFormatter,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.spacing.lg,
            vertical = MaterialTheme.spacing.lg,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
    ) {
        item {
            SummarySection(result, dateFormatter)
        }
        item {
            ExportSection(
                isExporting = isExporting,
                onExportPdf = onExportPdf,
                onExportJson = onExportJson,
                onShareReport = onShareReport,
                onOpenReport = onOpenReport,
                lastExport = lastExport,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        result.file?.let { fileReport ->
            item {
                SectionTitle(text = stringResource(id = R.string.results_file_section_title))
            }
            item {
                FileSection(fileReport)
            }
        }
        result.vpn?.let { vpnReport ->
            item {
                SectionTitle(text = stringResource(id = R.string.results_vpn_section_title))
            }
            item {
                VpnSection(vpnReport)
            }
        }
        result.instagram?.let { instagramReport ->
            item {
                SectionTitle(text = stringResource(id = R.string.results_instagram_section_title))
            }
            item {
                InstagramSection(instagramReport)
            }
        }
        if (result.vendors.isNotEmpty()) {
            item {
                SectionTitle(text = stringResource(id = R.string.results_vendors_title))
            }
            items(result.vendors) { vendor ->
                VendorCard(vendor)
            }
        }
        result.network?.let { networkReport ->
            item {
                SectionTitle(text = stringResource(id = R.string.results_network_title))
            }
            item {
                NetworkSection(networkReport)
            }
        }
        result.ml?.let { mlReport ->
            item {
                SectionTitle(text = stringResource(id = R.string.results_ml_title))
            }
            item {
                MlSection(mlReport)
            }
        }
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SummarySection(
    result: ScanResult,
    dateFormatter: DateTimeFormatter,
) {
    val timestamp = remember(result.createdAt, dateFormatter) {
        dateFormatter.format(result.createdAt.toJavaInstant().atZone(ZoneId.systemDefault()))
    }
    val shape = MaterialTheme.shapes.large
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
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
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                Text(text = result.targetLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = targetTypeLabel(result.targetType),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                result.normalizedUrl
                    ?.takeIf { it.isNotBlank() && it != result.targetLabel }
                    ?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RiskScoreBadge(result = result)
        }
    }
}

@Composable
private fun RiskScoreBadge(result: ScanResult) {
    val score = result.risk
    val label = result.breakdown.categories
        .maxByOrNull { it.value }
        ?.takeIf { it.value >= 0.2 }
        ?.key
        ?: when {
            score >= 4.5 -> RiskCategory.CRITICAL
            score >= 3.5 -> RiskCategory.HIGH
            score >= 2.0 -> RiskCategory.MEDIUM
            score >= 1.0 -> RiskCategory.LOW
            else -> RiskCategory.MINIMAL
        }
    val formattedScore = formatScore(score)
    val progress = (score / 5.0).coerceIn(0.0, 1.0).toFloat()
    val badgeColor = when (label) {
        RiskCategory.CRITICAL -> ScamynxColors.riskRed
        RiskCategory.HIGH -> ScamynxColors.riskOrange
        RiskCategory.MEDIUM -> ScamynxColors.riskYellow
        RiskCategory.LOW, RiskCategory.MINIMAL -> ScamynxColors.riskGreen
    }
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        Text(
            text = stringResource(id = R.string.results_score_label, formattedScore),
            style = MaterialTheme.typography.headlineSmall,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = badgeColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(text = riskCategoryLabel(label)) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = badgeColor.copy(alpha = 0.18f),
                labelColor = badgeColor,
            ),
        )
    }
}

@Composable
private fun ExportSection(
    isExporting: Boolean,
    onExportPdf: () -> Unit,
    onExportJson: () -> Unit,
    onShareReport: (GeneratedReport) -> Unit,
    onOpenReport: (GeneratedReport) -> Unit,
    lastExport: GeneratedReport?,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
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
            Text(text = stringResource(id = R.string.results_export_title), style = MaterialTheme.typography.titleMedium)
            BoxWithConstraints {
                val buttonSpacing = MaterialTheme.spacing.sm
                val isCompactWidth = maxWidth < 360.dp
                if (isCompactWidth) {
                    Column(verticalArrangement = Arrangement.spacedBy(buttonSpacing)) {
                        Button(
                            onClick = onExportPdf,
                            enabled = !isExporting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(imageVector = Icons.Filled.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                            Text(text = stringResource(id = R.string.results_export_pdf))
                        }
                        OutlinedButton(
                            onClick = onExportJson,
                            enabled = !isExporting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(imageVector = Icons.Filled.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                            Text(text = stringResource(id = R.string.results_export_json))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
                    ) {
                        Button(
                            onClick = onExportPdf,
                            enabled = !isExporting,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(imageVector = Icons.Filled.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                            Text(text = stringResource(id = R.string.results_export_pdf))
                        }
                        OutlinedButton(
                            onClick = onExportJson,
                            enabled = !isExporting,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(imageVector = Icons.Filled.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                            Text(text = stringResource(id = R.string.results_export_json))
                        }
                    }
                }
            }
            lastExport?.let { report ->
                HorizontalDivider()
                Text(
                    text = stringResource(
                        id = R.string.results_export_last_saved,
                        report.fileName,
                        formatFileSize(report.sizeBytes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                BoxWithConstraints {
                    val buttonSpacing = MaterialTheme.spacing.sm
                    val isCompactWidth = maxWidth < 360.dp
                    if (isCompactWidth) {
                        Column(verticalArrangement = Arrangement.spacedBy(buttonSpacing)) {
                            Button(
                                onClick = { onShareReport(report) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                Text(text = stringResource(id = R.string.results_export_share))
                            }
                            OutlinedButton(
                                onClick = { onOpenReport(report) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                Text(text = stringResource(id = R.string.results_export_open))
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
                        ) {
                            Button(
                                onClick = { onShareReport(report) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(imageVector = Icons.Filled.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                Text(text = stringResource(id = R.string.results_export_share))
                            }
                            OutlinedButton(
                                onClick = { onOpenReport(report) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                Text(text = stringResource(id = R.string.results_export_open))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorCard(vendor: VendorVerdict) {
    val providerName = providerLabel(vendor.provider)
    val statusText = statusLabel(vendor.status)
    ListItem(
        headlineContent = {
            Text(text = providerName, style = MaterialTheme.typography.titleMedium)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(
                        id = R.string.results_vendor_status,
                        statusText,
                        formatScore(vendor.score),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (vendor.details.isNotEmpty()) {
                    vendor.details.entries.take(3).forEach { entry ->
                        Text(
                            text = "${entry.key}: ${entry.value ?: stringResource(id = R.string.results_value_unknown)}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun NetworkSection(network: NetworkReport) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val tlsValue = network.tlsVersion ?: stringResource(id = R.string.results_value_unknown)
        val cipherValue = network.cipherSuite ?: stringResource(id = R.string.results_value_unknown)
        val certValue = when (network.certValid) {
            true -> stringResource(id = R.string.results_value_yes)
            false -> stringResource(id = R.string.results_value_no)
            null -> stringResource(id = R.string.results_value_unknown)
        }
        Text(text = stringResource(id = R.string.results_network_tls, tlsValue))
        Text(text = stringResource(id = R.string.results_network_cipher, cipherValue))
        Text(text = stringResource(id = R.string.results_network_certificate, certValue))
        if (network.headers.isNotEmpty()) {
            Text(text = stringResource(id = R.string.results_network_headers))
            network.headers.forEach { (key, value) ->
                Text(text = "• $key: $value", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MlSection(mlReport: MlReport) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = stringResource(id = R.string.results_ml_score, formatScore(mlReport.probability)))
        if (mlReport.topFeatures.isNotEmpty()) {
            Text(text = stringResource(id = R.string.results_ml_top_features))
            mlReport.topFeatures.take(5).forEach { feature ->
                Text(
                    text = "• ${feature.feature}: ${formatScore(feature.weight)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun FileSection(report: FileScanReport) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val sizeText = report.sizeBytes?.let { formatFileSize(it) }
            ?: stringResource(id = R.string.results_value_unknown)
        Text(text = stringResource(id = R.string.home_file_size, sizeText))
        report.mimeType?.let {
            Text(
                text = stringResource(id = R.string.home_file_mime, it),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        report.sha256?.let {
            Text(text = stringResource(id = R.string.results_file_hash, it))
        }
        if (report.suspiciousStrings.isNotEmpty()) {
            Text(text = stringResource(id = R.string.results_file_suspicious_strings))
            report.suspiciousStrings.take(5).forEach { match ->
                Text(text = "• $match", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (report.issues.isNotEmpty()) {
            Text(text = stringResource(id = R.string.results_file_issues))
            IssueList(report.issues)
        }
    }
}

@Composable
private fun VpnSection(report: VpnConfigReport) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        report.serverAddress?.let {
            Text(text = stringResource(id = R.string.results_vpn_server, it))
        }
        report.port?.let {
            Text(text = stringResource(id = R.string.results_vpn_port, it.toString()))
        }
        report.clientType?.let {
            Text(text = stringResource(id = R.string.results_vpn_client, it))
        }
        report.outboundType?.let {
            Text(text = stringResource(id = R.string.results_vpn_outbound, it))
        }
        val tlsValue = when (report.tlsEnabled) {
            true -> stringResource(id = R.string.results_value_yes)
            false -> stringResource(id = R.string.results_value_no)
            null -> stringResource(id = R.string.results_value_unknown)
        }
        Text(text = stringResource(id = R.string.results_vpn_tls, tlsValue))
        if (report.insecureTransports.isNotEmpty()) {
            Text(text = stringResource(id = R.string.results_vpn_insecure_transports))
            report.insecureTransports.forEach { transport ->
                Text(text = "• $transport", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (report.issues.isNotEmpty()) {
            IssueList(report.issues)
        }
    }
}

@Composable
private fun InstagramSection(report: InstagramScanReport) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = stringResource(id = R.string.results_instagram_handle, report.handle))
        report.displayName?.let {
            Text(text = stringResource(id = R.string.results_instagram_display, it))
        }
        report.followerCount?.let {
            val formatted = NumberFormat.getIntegerInstance().format(it)
            Text(text = stringResource(id = R.string.results_instagram_followers, formatted))
        }
        if (report.suspiciousSignals.isNotEmpty()) {
            Text(text = stringResource(id = R.string.results_instagram_suspicious_signals))
            report.suspiciousSignals.forEach { signal ->
                Text(text = "• $signal", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (report.issues.isNotEmpty()) {
            IssueList(report.issues)
        }
    }
}

@Composable
private fun IssueList(issues: List<ScanIssue>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        issues.forEach { issue ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${issue.title} • ${issueSeverityLabel(issue.severity)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                issue.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    @androidx.annotation.StringRes messageRes: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(id = R.string.results_error_title), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(id = messageRes), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            Button(onClick = onRetry) {
                Text(text = stringResource(id = R.string.action_retry))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun formatScore(value: Double): String {
    val locale = currentAppLocale()
    val formatter = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    return formatter.format(value)
}

@Composable
private fun formatFileSize(sizeBytes: Long): String {
    val context = LocalContext.current
    return Formatter.formatFileSize(context, sizeBytes)
}

private fun currentAppLocale(): Locale {
    AppCompatDelegate.getApplicationLocales().let { locales ->
        locales.get(0)?.let { return it }
    }
    LocaleListCompat.getAdjustedDefault().let { locales ->
        locales.get(0)?.let { return it }
    }
    return Locale.getDefault()
}

@Composable
private fun riskCategoryLabel(category: RiskCategory): String {
    val resId = when (category) {
        RiskCategory.MINIMAL -> R.string.risk_category_minimal
        RiskCategory.LOW -> R.string.risk_category_low
        RiskCategory.MEDIUM -> R.string.risk_category_medium
        RiskCategory.HIGH -> R.string.risk_category_high
        RiskCategory.CRITICAL -> R.string.risk_category_critical
    }
    return stringResource(id = resId)
}

@Composable
private fun targetTypeLabel(targetType: ScanTargetType): String {
    val resId = when (targetType) {
        ScanTargetType.URL -> R.string.results_target_url
        ScanTargetType.FILE -> R.string.results_target_file
        ScanTargetType.VPN_CONFIG -> R.string.results_target_vpn
        ScanTargetType.INSTAGRAM -> R.string.results_target_instagram
    }
    return stringResource(id = resId)
}

@Composable
private fun providerLabel(provider: Provider): String {
    val resId = when (provider) {
        Provider.VIRUS_TOTAL -> R.string.provider_virustotal
        Provider.GOOGLE_SAFE_BROWSING -> R.string.provider_google_safe_browsing
        Provider.URL_SCAN -> R.string.provider_urlscan
        Provider.URL_HAUS -> R.string.provider_urlhaus
        Provider.PHISH_STATS -> R.string.provider_phish_stats
        Provider.THREAT_FOX -> R.string.provider_threat_fox
        Provider.NETWORK -> R.string.provider_network
        Provider.ML -> R.string.provider_ml
        Provider.FILE_STATIC -> R.string.provider_file_static
        Provider.VPN_CONFIG -> R.string.provider_vpn_config
        Provider.INSTAGRAM -> R.string.provider_instagram
        Provider.LOCAL_HEURISTIC -> R.string.provider_local_heuristics
        Provider.CHAT_GPT -> R.string.provider_chat_gpt
        Provider.MANUAL -> R.string.provider_manual
    }
    return stringResource(id = resId)
}

@Composable
private fun statusLabel(status: VerdictStatus): String {
    val resId = when (status) {
        VerdictStatus.CLEAN -> R.string.vendor_status_clean
        VerdictStatus.SUSPICIOUS -> R.string.vendor_status_suspicious
        VerdictStatus.MALICIOUS -> R.string.vendor_status_malicious
        VerdictStatus.UNKNOWN -> R.string.vendor_status_unknown
        VerdictStatus.ERROR -> R.string.vendor_status_error
    }
    return stringResource(id = resId)
}

@Composable
private fun issueSeverityLabel(severity: IssueSeverity): String {
    val resId = when (severity) {
        IssueSeverity.LOW -> R.string.results_issue_severity_low
        IssueSeverity.MEDIUM -> R.string.results_issue_severity_medium
        IssueSeverity.HIGH -> R.string.results_issue_severity_high
        IssueSeverity.CRITICAL -> R.string.results_issue_severity_critical
    }
    return stringResource(id = resId)
}
