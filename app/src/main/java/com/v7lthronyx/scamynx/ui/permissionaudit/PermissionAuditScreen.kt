package com.v7lthronyx.scamynx.ui.permissionaudit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
private val NeonYellow = Color(0xFFFFD700)

@Composable
fun PermissionAuditRoute(
    onBack: () -> Unit,
    viewModel: PermissionAuditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PermissionAuditScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onAppClick = viewModel::toggleAppExpanded,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionAuditScreen(
    state: PermissionAuditUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAppClick: (String) -> Unit,
) {
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
                            text = stringResource(id = R.string.permission_audit_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "App Permission Analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPurple.copy(alpha = 0.7f),
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
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.cd_refresh),
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
                            NeonPurple.copy(alpha = bgAlpha),
                            NeonCyan.copy(alpha = bgAlpha * 0.5f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(padding),
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonCyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing installed apps...",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(MaterialTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                ) {
                    
                    item {
                        AuditSummaryCard(state)
                    }

                    item {
                        Text(
                            text = "Permission Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                        )
                    }

                    item {
                        PermissionCategoriesCard(state.permissionStats)
                    }

                    if (state.highRiskApps.isNotEmpty()) {
                        item {
                            Text(
                                text = "High Risk Apps",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonRed,
                            )
                        }

                        items(state.highRiskApps) { app ->
                            AppPermissionCard(
                                app = app,
                                isExpanded = state.expandedAppId == app.packageName,
                                onToggle = { onAppClick(app.packageName) },
                            )
                        }
                    }

                    item {
                        Text(
                            text = "All Apps (${state.allApps.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple,
                        )
                    }

                    items(state.allApps) { app ->
                        AppPermissionCard(
                            app = app,
                            isExpanded = state.expandedAppId == app.packageName,
                            onToggle = { onAppClick(app.packageName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditSummaryCard(state: PermissionAuditUiState) {
    val riskColor = when {
        state.overallRiskScore >= 70 -> NeonRed
        state.overallRiskScore >= 40 -> NeonOrange
        else -> NeonGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = riskColor.copy(alpha = 0.1f),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(riskColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${state.overallRiskScore}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = riskColor,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    state.overallRiskScore >= 70 -> "High Risk"
                    state.overallRiskScore >= 40 -> "Moderate Risk"
                    else -> "Low Risk"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = riskColor,
            )
            Text(
                text = "Permission Risk Score",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SummaryItem(
                    value = state.totalApps.toString(),
                    label = "Apps",
                    color = NeonCyan,
                )
                SummaryItem(
                    value = state.highRiskApps.size.toString(),
                    label = "High Risk",
                    color = NeonRed,
                )
                SummaryItem(
                    value = state.dangerousPermissions.toString(),
                    label = "Dangerous",
                    color = NeonOrange,
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    value: String,
    label: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
private fun PermissionCategoriesCard(stats: List<PermissionStat>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            stats.forEach { stat ->
                PermissionStatRow(stat)
            }
        }
    }
}

@Composable
private fun PermissionStatRow(stat: PermissionStat) {
    val icon = getPermissionIcon(stat.category)
    val color = getPermissionColor(stat.category)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stat.category,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${stat.appCount} apps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { stat.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
            )
        }
    }
}

private fun getPermissionIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "location" -> Icons.Default.LocationOn
        "camera" -> Icons.Default.Camera
        "microphone" -> Icons.Default.Mic
        "contacts" -> Icons.Default.ContactPage
        "storage" -> Icons.Default.Folder
        "phone" -> Icons.Default.Phone
        "sms" -> Icons.Default.Sms
        else -> Icons.Default.Security
    }
}

private fun getPermissionColor(category: String): Color {
    return when (category.lowercase()) {
        "location" -> NeonRed
        "camera" -> NeonOrange
        "microphone" -> NeonYellow
        "contacts" -> NeonPurple
        "storage" -> NeonCyan
        "phone" -> NeonGreen
        "sms" -> NeonRed
        else -> NeonCyan
    }
}

@Composable
private fun AppPermissionCard(
    app: AppPermissionUiModel,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    val riskColor = when (app.riskLevel) {
        RiskLevel.HIGH -> NeonRed
        RiskLevel.MEDIUM -> NeonOrange
        RiskLevel.LOW -> NeonGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(riskColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = riskColor,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${app.permissions.size} permissions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = app.riskLevel.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = riskColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        id = if (isExpanded) R.string.cd_collapse else R.string.cd_expand,
                    ),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                ) {
                    Text(
                        text = "Permissions:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    app.permissions.forEach { permission ->
                        PermissionRow(permission)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(permission: PermissionUiModel) {
    val (icon, color) = when {
        permission.isDangerous -> Icons.Default.Warning to NeonRed
        else -> Icons.Default.CheckCircle to NeonGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = permission.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        if (permission.isDangerous) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(id = R.string.cd_dangerous_permission),
                tint = NeonOrange,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

data class PermissionAuditUiState(
    val isLoading: Boolean = true,
    val overallRiskScore: Int = 0,
    val totalApps: Int = 0,
    val dangerousPermissions: Int = 0,
    val permissionStats: List<PermissionStat> = emptyList(),
    val highRiskApps: List<AppPermissionUiModel> = emptyList(),
    val allApps: List<AppPermissionUiModel> = emptyList(),
    val expandedAppId: String? = null,
)

data class PermissionStat(
    val category: String,
    val appCount: Int,
    val percentage: Float,
)

data class AppPermissionUiModel(
    val packageName: String,
    val name: String,
    val riskLevel: RiskLevel,
    val permissions: List<PermissionUiModel>,
)

data class PermissionUiModel(
    val name: String,
    val isDangerous: Boolean,
)

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
}
