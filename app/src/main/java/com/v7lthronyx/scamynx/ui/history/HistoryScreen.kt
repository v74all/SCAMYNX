package com.v7lthronyx.scamynx.ui.history

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.datetime.toJavaInstant

@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onResultSelected: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onResultSelected = onResultSelected,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onResultSelected: (String) -> Unit,
) {
    val locale = rememberCurrentLocale()
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(locale)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        when {
            state.isLoading -> LoadingState(modifier = Modifier.padding(paddingValues))
            state.errorMessageRes != null -> ErrorState(
                messageRes = state.errorMessageRes,
                onRetry = onRetry,
                modifier = Modifier.padding(paddingValues),
            )
            state.items.isEmpty() -> EmptyState(
                modifier = Modifier.padding(paddingValues),
            )
            else -> HistoryList(
                items = state.items,
                dateFormatter = dateFormatter,
                onResultSelected = onResultSelected,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun HistoryList(
    items: List<HistoryItemUi>,
    dateFormatter: DateTimeFormatter,
    onResultSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        items(items, key = { it.sessionId }) { item ->
            HistoryRow(
                item = item,
                dateFormatter = dateFormatter,
                onClick = { onResultSelected(item.sessionId) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItemUi,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timestamp = remember(item.createdAt, dateFormatter) {
        dateFormatter.format(item.createdAt.toJavaInstant().atZone(ZoneId.systemDefault()))
    }
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = item.targetLabel,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = historyTargetLabel(item.targetType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = timestamp, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = stringResource(
                        id = R.string.history_risk_label,
                        formatRiskScore(item.riskScore),
                        riskCategoryLabel(item.riskCategory),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
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
            Text(text = stringResource(id = R.string.history_error_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(id = messageRes),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text(text = stringResource(id = R.string.action_retry))
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = stringResource(id = R.string.history_empty_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(id = R.string.history_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun historyTargetLabel(targetType: ScanTargetType): String {
    val resId = when (targetType) {
        ScanTargetType.URL -> R.string.results_target_url
        ScanTargetType.FILE -> R.string.results_target_file
        ScanTargetType.VPN_CONFIG -> R.string.results_target_vpn
        ScanTargetType.INSTAGRAM -> R.string.results_target_instagram
    }
    return stringResource(id = resId)
}

private fun formatRiskScore(score: Double): String = String.format(Locale.US, "%.2f", score)

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
private fun rememberCurrentLocale(): Locale {
    val configuration = LocalConfiguration.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val locales = configuration.locales
        if (!locales.isEmpty) {
            locales[0]
        } else {
            Locale.getDefault()
        }
    } else {
        @Suppress("DEPRECATION")
        configuration.locale ?: Locale.getDefault()
    }
}
