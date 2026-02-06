package com.v7lthronyx.scamynx.ui.threatintel

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.common.designsystem.spacing

private val NeonCyan = Color(0xFF00D4FF)
private val NeonGreen = Color(0xFF00FFAA)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonRed = Color(0xFFFF5C7A)
private val NeonOrange = Color(0xFFFFA14A)

@Composable
fun ThreatIntelRoute(
    onBack: () -> Unit,
    viewModel: ThreatIntelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ThreatIntelScreen(
        state = state,
        onBack = onBack,
        onSearch = viewModel::searchIoC,
        onRefreshFeeds = viewModel::refreshFeeds,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreatIntelScreen(
    state: ThreatIntelUiState,
    onBack: () -> Unit,
    onSearch: (String) -> Unit,
    onRefreshFeeds: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.02f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bg_alpha",
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.threat_intel_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(id = R.string.threat_intel_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan.copy(alpha = 0.7f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshFeeds) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.threat_intel_refresh_feeds),
                            tint = NeonCyan,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = bgAlpha),
                            NeonPurple.copy(alpha = bgAlpha * 0.5f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(id = R.string.threat_intel_search_hint)) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = stringResource(id = R.string.threat_intel_search_button), tint = NeonCyan)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                FilledTonalButton(
                                    onClick = { onSearch(searchQuery) },
                                    enabled = !state.isSearching,
                                ) {
                                    Text(stringResource(id = R.string.threat_intel_search_button))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            cursorColor = NeonCyan,
                        ),
                    )
                }

                if (state.isSearching) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    }
                }

                item {
                    ThreatStatsCard(state)
                }

                item {
                    Text(
                        text = stringResource(id = R.string.threat_intel_active_feeds),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                    )
                }

                items(state.threatFeeds) { feed ->
                    ThreatFeedCard(feed)
                }

                if (state.searchResults.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(id = R.string.threat_intel_search_results),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple,
                        )
                    }

                    items(state.searchResults) { result ->
                        IoCResultCard(result)
                    }
                }

                item {
                    Text(
                        text = stringResource(id = R.string.threat_intel_recent_threats),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonOrange,
                    )
                }

                if (state.recentThreats.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Shield,
                            title = stringResource(id = R.string.threat_intel_no_threats),
                            subtitle = stringResource(id = R.string.threat_intel_feeds_clean),
                            accentColor = NeonGreen,
                        )
                    }
                } else {
                    items(state.recentThreats) { threat ->
                        ThreatCard(threat)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreatStatsCard(state: ThreatIntelUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
        ) {
            Text(
                text = stringResource(id = R.string.threat_intel_landscape),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(
                    value = state.totalIndicators.toString(),
                    label = stringResource(id = R.string.threat_intel_indicators),
                    icon = Icons.Default.BugReport,
                    color = NeonCyan,
                )
                StatItem(
                    value = state.activeFeeds.toString(),
                    label = stringResource(id = R.string.threat_intel_active_feeds),
                    icon = Icons.Default.Radar,
                    color = NeonGreen,
                )
                StatItem(
                    value = state.threatsToday.toString(),
                    label = stringResource(id = R.string.threat_intel_today),
                    icon = Icons.Default.Warning,
                    color = NeonOrange,
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun ThreatFeedCard(feed: ThreatFeedUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (feed.isActive) {
                            NeonGreen.copy(alpha = 0.2f)
                        } else {
                            NeonRed.copy(alpha = 0.2f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = feed.name,
                    tint = if (feed.isActive) NeonGreen else NeonRed,
                )
            }
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feed.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${feed.indicatorCount} indicators",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            if (feed.isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(id = R.string.threat_intel_feed_active),
                    tint = NeonGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun IoCResultCard(result: IoCResultUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (result.isMalicious) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = stringResource(
                        id = if (result.isMalicious) {
                            R.string.threat_intel_malicious
                        } else {
                            R.string.threat_intel_safe
                        },
                    ),
                    tint = if (result.isMalicious) NeonRed else NeonGreen,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = result.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (result.isMalicious) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Threat Type: ${result.threatType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonRed,
                )
                Text(
                    text = "Sources: ${result.sources.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun ThreatCard(threat: ThreatUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = NeonRed.copy(alpha = 0.1f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = stringResource(id = R.string.cd_threat_level_indicator),
                tint = NeonRed,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = threat.indicator,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = threat.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Text(
                text = threat.severity,
                style = MaterialTheme.typography.labelSmall,
                color = NeonRed,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.1f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

data class ThreatIntelUiState(
    val isSearching: Boolean = false,
    val totalIndicators: Int = 0,
    val activeFeeds: Int = 0,
    val threatsToday: Int = 0,
    val threatFeeds: List<ThreatFeedUiModel> = emptyList(),
    val searchResults: List<IoCResultUiModel> = emptyList(),
    val recentThreats: List<ThreatUiModel> = emptyList(),
)

data class ThreatFeedUiModel(
    val id: String,
    val name: String,
    val indicatorCount: Int,
    val isActive: Boolean,
)

data class IoCResultUiModel(
    val value: String,
    val isMalicious: Boolean,
    val threatType: String,
    val sources: List<String>,
)

data class ThreatUiModel(
    val indicator: String,
    val type: String,
    val severity: String,
)
