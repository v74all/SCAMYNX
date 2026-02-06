package com.v7lthronyx.scamynx.ui.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.common.designsystem.ScamynxCustomShapes
import com.v7lthronyx.scamynx.common.designsystem.ScamynxGradients
import com.v7lthronyx.scamynx.common.designsystem.ScamynxPrimary80
import com.v7lthronyx.scamynx.common.designsystem.ScamynxSecondary80
import com.v7lthronyx.scamynx.common.designsystem.ScamynxSignalGreen
import com.v7lthronyx.scamynx.common.designsystem.ScamynxTertiary80
import com.v7lthronyx.scamynx.common.designsystem.ScamynxTheme
import com.v7lthronyx.scamynx.common.designsystem.spacing
import com.v7lthronyx.scamynx.data.privacyradar.coordinator.PrivacyRadarStatus
import com.v7lthronyx.scamynx.domain.model.ScanStage
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HeroHeader(
    isScanning: Boolean,
    progressPercent: Int,
    progressStage: ScanStage?,
    progressMessage: String?,
    selectedTarget: ScanTargetType,
    onScanRequested: () -> Unit,
    onCancelScan: () -> Unit,
    isPrimaryActionEnabled: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    val scanRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scanRotation",
    )

    val targetLabel = stringResource(
        id = when (selectedTarget) {
            ScanTargetType.URL -> R.string.home_target_url
            ScanTargetType.FILE -> R.string.home_target_file
            ScanTargetType.VPN_CONFIG -> R.string.home_target_vpn
            ScanTargetType.INSTAGRAM -> R.string.home_target_instagram
        },
    )

    val statusTitle = stringResource(
        id = if (isScanning) {
            R.string.home_status_title_scanning
        } else {
            R.string.home_status_title_ready
        },
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ScamynxCustomShapes.heroCard,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            width = 1.dp,
            brush = if (isScanning) {
                Brush.sweepGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                        MaterialTheme.colorScheme.secondary.copy(alpha = glowAlpha * 0.7f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = glowAlpha * 0.5f),
                        MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                    ),
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    ),
                )
            },
        ),
    ) {
        Box(
            modifier = Modifier
                .clip(ScamynxCustomShapes.heroCard)
                .background(ScamynxGradients.hero()),
        ) {
            
            if (isScanning) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        ScamynxPrimary80.copy(alpha = glowAlpha * 0.3f),
                                        Color.Transparent,
                                    ),
                                ),
                                radius = size.minDimension * 0.8f,
                                center = Offset(size.width * 0.3f, size.height * 0.3f),
                            )
                        },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                
                Surface(
                    shape = ScamynxCustomShapes.pill,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.spacing.md,
                            vertical = MaterialTheme.spacing.xs,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Radar,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(id = R.string.home_target_focus, targetLabel),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                ) {
                    
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        
                        if (isScanning) {
                            Canvas(modifier = Modifier.size(56.dp)) {
                                rotate(scanRotation) {
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                ScamynxPrimary80,
                                                ScamynxSecondary80,
                                                ScamynxTertiary80,
                                                ScamynxPrimary80,
                                            ),
                                        ),
                                        startAngle = 0f,
                                        sweepAngle = 270f,
                                        useCenter = false,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isScanning) Icons.Filled.Radar else Icons.Filled.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
                    ) {
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                id = if (isScanning) {
                                    R.string.home_status_subtitle_scanning
                                } else {
                                    R.string.home_status_subtitle_ready
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isScanning,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    val progressValue = remember(progressPercent) { progressPercent.coerceIn(0, 100) }
                    val animatedProgress by animateFloatAsState(
                        targetValue = progressValue / 100f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "progress",
                    )

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
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                        tonalElevation = 2.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(MaterialTheme.spacing.md),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                        ) {
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedProgress)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary,
                                                ),
                                            ),
                                        ),
                                )
                            }

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
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
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
                        shape = ScamynxCustomShapes.buttonLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        Text(
                            text = stringResource(id = R.string.action_stop_scan),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Button(
                        onClick = onScanRequested,
                        enabled = isPrimaryActionEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = ScamynxCustomShapes.buttonLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        Text(
                            text = stringResource(id = R.string.action_scan_now),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ApiStatusCard(statuses: List<ApiProviderStatus>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(id = R.string.home_api_status_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = stringResource(id = R.string.home_api_status_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (statuses.isEmpty()) {
                val emptyStatusText = stringResource(id = R.string.home_api_status_empty)
                Text(
                    text = emptyStatusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { contentDescription = emptyStatusText },
                )
                return@Column
            }

            statuses.forEach { status ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                ) {
                    val iconTint = if (status.isConfigured) {
                        ScamynxSignalGreen
                    } else {
                        MaterialTheme.colorScheme.error
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (status.isConfigured) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = iconTint,
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = status.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
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
fun PrivacyRadarStatusCard(
    enabled: Boolean,
    status: PrivacyRadarStatus,
    isBatterySaverOn: Boolean,
    onEnable: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                modifier = Modifier.semantics { heading() },
            ) {
                Icon(
                    imageVector = Icons.Filled.Radar,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(id = R.string.home_privacy_radar_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            val (statusLabel, statusColor) = when {
                !enabled -> stringResource(id = R.string.home_privacy_radar_disabled) to MaterialTheme.colorScheme.onSurfaceVariant
                isBatterySaverOn -> stringResource(id = R.string.home_privacy_radar_battery) to MaterialTheme.colorScheme.error
                status == PrivacyRadarStatus.RUNNING -> stringResource(id = R.string.home_privacy_radar_running) to ScamynxSignalGreen
                status == PrivacyRadarStatus.STARTING -> stringResource(id = R.string.home_privacy_radar_starting) to MaterialTheme.colorScheme.primary
                status == PrivacyRadarStatus.ERROR -> stringResource(id = R.string.home_privacy_radar_error) to MaterialTheme.colorScheme.error
                else -> stringResource(id = R.string.home_privacy_radar_off) to MaterialTheme.colorScheme.onSurfaceVariant
            }

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                Button(
                    onClick = onEnable,
                    enabled = enabled && !isBatterySaverOn,
                    modifier = Modifier.weight(1f),
                    shape = ScamynxCustomShapes.button,
                ) {
                    Text(text = stringResource(id = R.string.home_privacy_radar_enable))
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                    shape = ScamynxCustomShapes.button,
                ) {
                    Text(text = stringResource(id = R.string.home_privacy_radar_open_settings))
                }
            }
        }
    }
}

@Composable
fun ScanModeSelector(
    selected: ScanTargetType,
    onSelected: (ScanTargetType) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs),
            ) {
                Text(
                    text = stringResource(id = R.string.home_scan_modes_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
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
                    Triple(ScanTargetType.URL, R.string.home_target_url, Icons.Filled.Language),
                    Triple(ScanTargetType.FILE, R.string.home_target_file, Icons.AutoMirrored.Filled.InsertDriveFile),
                    Triple(ScanTargetType.VPN_CONFIG, R.string.home_target_vpn, Icons.Filled.Description),
                    Triple(ScanTargetType.INSTAGRAM, R.string.home_target_instagram, Icons.Filled.AutoAwesome),
                )

                items(items) { (type, labelRes, icon) ->
                    val isSelected = selected == type

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelected(type) },
                        label = {
                            Text(
                                text = stringResource(id = labelRes),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        shape = ScamynxCustomShapes.chip,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        border = if (isSelected) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun UrlInputCard(
    url: String,
    onValueChange: (String) -> Unit,
    onScanRequested: () -> Unit,
    isLoading: Boolean,
    isUrlValidating: Boolean = false,
    urlValidationError: String? = null,
) {
    val urlInputDescription = stringResource(id = R.string.cd_url_input)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                text = stringResource(id = R.string.home_prompt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = urlInputDescription },
                value = url,
                onValueChange = onValueChange,
                singleLine = true,
                label = { Text(text = stringResource(id = R.string.home_url_label)) },
                placeholder = { Text(text = stringResource(id = R.string.home_url_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (isUrlValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                },
                isError = urlValidationError != null,
                supportingText = urlValidationError?.let { { Text(text = it) } },
                shape = ScamynxCustomShapes.textField,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                ),
            )

            Button(
                onClick = onScanRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && !isLoading,
                shape = ScamynxCustomShapes.button,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                    Text(
                        text = stringResource(id = R.string.action_scan_now),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun FileInputCard(
    selectedFile: SelectedFile?,
    isLoading: Boolean,
    onPickFile: () -> Unit,
    onClearFile: () -> Unit,
    onScanRequested: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                text = stringResource(id = R.string.home_file_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = stringResource(id = R.string.home_file_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selectedFile != null) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    },
                ),
            ) {
                if (selectedFile != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }

                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedFile.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = formatSize(selectedFile.sizeBytes ?: 0L),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        IconButton(onClick = onClearFile) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = onPickFile,
                            enabled = !isLoading,
                            shape = ScamynxCustomShapes.button,
                        ) {
                            Text(text = stringResource(id = R.string.home_file_select))
                        }
                    }
                }
            }

            Button(
                onClick = onScanRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedFile != null && !isLoading,
                shape = ScamynxCustomShapes.button,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                    Text(
                        text = stringResource(id = R.string.action_scan_now),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun VpnConfigInputCard(
    vpnConfig: String,
    vpnProfileLabel: String,
    onConfigChanged: (String) -> Unit,
    onLabelChanged: (String) -> Unit,
    onScanRequested: () -> Unit,
    isLoading: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                text = stringResource(id = R.string.home_vpn_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = vpnProfileLabel,
                onValueChange = onLabelChanged,
                singleLine = true,
                label = { Text(text = stringResource(id = R.string.home_vpn_label)) },
                placeholder = { Text(text = stringResource(id = R.string.home_vpn_label_placeholder)) },
                shape = ScamynxCustomShapes.textField,
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
                shape = ScamynxCustomShapes.textField,
            )

            Button(
                onClick = onScanRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = vpnConfig.isNotBlank() && !isLoading,
                shape = ScamynxCustomShapes.button,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                    Text(
                        text = stringResource(id = R.string.action_scan_now),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun InstagramInputCard(
    state: HomeUiState,
    onHandleChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onFollowersChanged: (String) -> Unit,
    onMessageChanged: (String) -> Unit,
    onBioChanged: (String) -> Unit,
    onScanRequested: () -> Unit,
    isLoading: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Text(
                text = stringResource(id = R.string.home_instagram_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.instagramHandle,
                onValueChange = onHandleChanged,
                singleLine = true,
                label = { Text(text = stringResource(id = R.string.home_instagram_handle_label)) },
                placeholder = { Text(text = stringResource(id = R.string.home_instagram_handle_placeholder)) },
                shape = ScamynxCustomShapes.textField,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.instagramDisplayName,
                onValueChange = onDisplayNameChanged,
                singleLine = true,
                label = { Text(text = stringResource(id = R.string.home_instagram_display_name)) },
                shape = ScamynxCustomShapes.textField,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.instagramFollowerCount,
                onValueChange = onFollowersChanged,
                singleLine = true,
                label = { Text(text = stringResource(id = R.string.home_instagram_followers)) },
                shape = ScamynxCustomShapes.textField,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.instagramMessage,
                onValueChange = onMessageChanged,
                label = { Text(text = stringResource(id = R.string.home_instagram_message)) },
                placeholder = { Text(text = stringResource(id = R.string.home_instagram_message_placeholder)) },
                maxLines = 3,
                shape = ScamynxCustomShapes.textField,
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.instagramBio,
                onValueChange = onBioChanged,
                label = { Text(text = stringResource(id = R.string.home_instagram_bio)) },
                maxLines = 3,
                shape = ScamynxCustomShapes.textField,
            )

            Button(
                onClick = onScanRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.instagramHandle.isNotBlank() && !isLoading,
                shape = ScamynxCustomShapes.button,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                    Text(
                        text = stringResource(id = R.string.action_scan_now),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun HomeErrorBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
fun PasswordSecurityCard(
    passwordValue: String,
    onPasswordValueChanged: (String) -> Unit,
    onPasswordCheckRequested: () -> Unit,
    isChecking: Boolean,
    report: PasswordSecurityUiModel?,
    @StringRes passwordErrorResId: Int?,
) {
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val buttonEnabled = passwordValue.isNotBlank() && !isChecking

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(id = R.string.home_password_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = stringResource(id = R.string.home_password_card_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = passwordValue,
                onValueChange = onPasswordValueChanged,
                label = { Text(stringResource(id = R.string.home_password_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null,
                        )
                    }
                },
                shape = ScamynxCustomShapes.textField,
            )

            passwordErrorResId?.let { resId ->
                Text(
                    text = stringResource(id = resId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            FilledTonalButton(
                onClick = onPasswordCheckRequested,
                enabled = buttonEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = ScamynxCustomShapes.button,
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                }
                Text(
                    text = stringResource(
                        if (isChecking) {
                            R.string.home_password_button_checking
                        } else {
                            R.string.home_password_button_check
                        },
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            AnimatedVisibility(visible = report != null) {
                report?.let { PasswordInsights(it) }
            }
        }
    }
}

@Composable
private fun PasswordInsights(report: PasswordSecurityUiModel) {
    val numberFormatter = remember { NumberFormat.getIntegerInstance() }

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.home_password_strength_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(id = report.strengthLabelRes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(report.strengthPercent / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                ),
                            ),
                        ),
                )
            }
        }

        Text(
            text = stringResource(id = R.string.home_password_entropy_bits, report.entropyBits),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val breachText = if (report.isBreached) {
            stringResource(
                id = R.string.home_password_breach_detected,
                numberFormatter.format(report.breachCount),
            )
        } else {
            stringResource(id = R.string.home_password_breach_safe)
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (report.isBreached) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            } else {
                ScamynxSignalGreen.copy(alpha = 0.15f)
            },
        ) {
            Text(
                text = breachText,
                modifier = Modifier.padding(MaterialTheme.spacing.sm),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (report.isBreached) MaterialTheme.colorScheme.error else ScamynxSignalGreen,
            )
        }

        if (report.warnings.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
                Text(
                    text = stringResource(id = R.string.home_password_warnings_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                report.warnings.forEach { warningRes ->
                    Text(
                        text = "• ${stringResource(id = warningRes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (report.recommendations.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
                Text(
                    text = stringResource(id = R.string.home_password_recommendations_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                report.recommendations.forEach { recoRes ->
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                text = stringResource(id = recoRes),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun SocialEngineeringCard(
    messageValue: String,
    onMessageChanged: (String) -> Unit,
    onAnalyzeMessage: () -> Unit,
    isChecking: Boolean,
    report: SocialEngineeringUiModel?,
    @StringRes errorResId: Int?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Filled.Insights,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = stringResource(id = R.string.home_social_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = stringResource(id = R.string.home_social_card_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = messageValue,
                onValueChange = onMessageChanged,
                label = { Text(stringResource(id = R.string.home_social_placeholder)) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                shape = ScamynxCustomShapes.textField,
            )

            errorResId?.let { resId ->
                Text(
                    text = stringResource(id = resId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            FilledTonalButton(
                onClick = onAnalyzeMessage,
                enabled = messageValue.isNotBlank() && !isChecking,
                modifier = Modifier.fillMaxWidth(),
                shape = ScamynxCustomShapes.button,
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                }
                Text(
                    text = stringResource(
                        if (isChecking) R.string.home_social_button_scanning else R.string.home_social_button_scan,
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            AnimatedVisibility(visible = report != null) {
                report?.let { SocialEngineeringInsights(it) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SocialEngineeringInsights(report: SocialEngineeringUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
        
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.home_social_risk_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = stringResource(id = report.riskLabelRes),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(report.riskPercent / 100f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary,
                                ),
                            ),
                        ),
                )
            }
        }

        if (report.indicators.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Text(
                    text = stringResource(id = R.string.home_social_indicators_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
                ) {
                    report.indicators.forEach { indicator ->
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = { Text(text = stringResource(id = indicator.labelRes)) },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                        )
                    }
                }
            }
        }

        if (report.snippets.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xxs)) {
                Text(
                    text = stringResource(id = R.string.home_social_snippets_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                report.snippets.forEach { snippet ->
                    Text(
                        text = "• \"$snippet\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (report.recommendations.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
                Text(
                    text = stringResource(id = R.string.home_social_recommendations_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                report.recommendations.forEach { reco ->
                    Text(
                        text = "• $reco",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private data class HomeFeatureHighlight(
    val icon: ImageVector,
    val titleRes: Int,
    val bodyRes: Int,
    val accentColor: Color,
)

@Composable
fun FeatureHighlights() {
    val features = remember {
        listOf(
            HomeFeatureHighlight(
                icon = Icons.Filled.Shield,
                titleRes = R.string.home_feature_ai_title,
                bodyRes = R.string.home_feature_ai_body,
                accentColor = ScamynxPrimary80,
            ),
            HomeFeatureHighlight(
                icon = Icons.Filled.AutoAwesome,
                titleRes = R.string.home_feature_playbook_title,
                bodyRes = R.string.home_feature_playbook_body,
                accentColor = ScamynxSecondary80,
            ),
            HomeFeatureHighlight(
                icon = Icons.Filled.Insights,
                titleRes = R.string.home_feature_insights_title,
                bodyRes = R.string.home_feature_insights_body,
                accentColor = ScamynxTertiary80,
            ),
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Text(
            text = stringResource(id = R.string.home_features_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
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
    Card(
        modifier = Modifier.width(240.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, feature.accentColor.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(feature.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = feature.accentColor,
                )
            }

            Text(
                text = stringResource(id = feature.titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
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
fun QuickActionsCard(
    onScanRequested: () -> Unit,
    onShowHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    isLoading: Boolean,
    isScanEnabled: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier
                .background(ScamynxGradients.card(), MaterialTheme.shapes.large)
                .padding(MaterialTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            Text(
                text = stringResource(id = R.string.home_quick_actions),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Button(
                onClick = onScanRequested,
                modifier = Modifier.fillMaxWidth(),
                enabled = isScanEnabled,
                shape = ScamynxCustomShapes.button,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                    Text(
                        text = stringResource(id = R.string.action_scan_now),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            FilledTonalButton(
                onClick = onShowHistory,
                modifier = Modifier.fillMaxWidth(),
                shape = ScamynxCustomShapes.button,
            ) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                Text(text = stringResource(id = R.string.action_view_history))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                    shape = ScamynxCustomShapes.button,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xxs))
                    Text(text = stringResource(id = R.string.action_open_settings))
                }

                OutlinedButton(
                    onClick = onOpenAbout,
                    modifier = Modifier.weight(1f),
                    shape = ScamynxCustomShapes.button,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xxs))
                    Text(text = stringResource(id = R.string.action_open_about))
                }
            }
        }
    }
}

// Preview removed - HomeScreen has different parameters
// Use HomeScreen.kt previews instead

internal suspend fun loadSelectedFile(context: Context, uri: Uri): SelectedFile = withContext(Dispatchers.IO) {
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

internal fun formatSize(sizeBytes: Long): String {
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

internal const val MAX_FILE_BYTES = 5 * 1024 * 1024

internal class FileTooLargeException(val length: Long) : Exception()

private val NeonCyan = Color(0xFF00D4FF)
private val NeonGreen = Color(0xFF00FFAA)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonRed = Color(0xFFFF5C7A)
private val NeonOrange = Color(0xFFFFA14A)
private val NeonBlue = Color(0xFF4A90D9)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SecurityToolsCard(
    onThreatIntel: () -> Unit,
    onQRScanner: () -> Unit,
    onNetworkMonitor: () -> Unit,
    onPermissionAudit: () -> Unit,
    onBreachMonitoring: () -> Unit,
    onSecurityScore: () -> Unit,
    onDeviceHardening: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Security Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            Text(
                text = "Advanced security features for comprehensive protection",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecurityToolChip(
                    icon = Icons.Filled.Radar,
                    label = "Threat Intel",
                    color = NeonCyan,
                    onClick = onThreatIntel,
                )
                SecurityToolChip(
                    icon = Icons.Filled.QrCodeScanner,
                    label = "QR Scanner",
                    color = NeonPurple,
                    onClick = onQRScanner,
                )
                SecurityToolChip(
                    icon = Icons.Filled.Wifi,
                    label = "Network",
                    color = NeonBlue,
                    onClick = onNetworkMonitor,
                )
                SecurityToolChip(
                    icon = Icons.Filled.VerifiedUser,
                    label = "Permissions",
                    color = NeonOrange,
                    onClick = onPermissionAudit,
                )
                SecurityToolChip(
                    icon = Icons.Filled.ShieldMoon,
                    label = "Breach Monitor",
                    color = NeonRed,
                    onClick = onBreachMonitoring,
                )
                SecurityToolChip(
                    icon = Icons.Filled.Shield,
                    label = "Security Score",
                    color = NeonGreen,
                    onClick = onSecurityScore,
                )
                SecurityToolChip(
                    icon = Icons.Filled.Build,
                    label = "Hardening",
                    color = NeonPurple,
                    onClick = onDeviceHardening,
                )
            }
        }
    }
}

@Composable
private fun SecurityToolChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = color,
            )
        }
    }
}
