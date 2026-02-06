package com.v7lthronyx.scamynx.ui.devicehardening

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.common.designsystem.spacing
import com.v7lthronyx.scamynx.domain.model.HardeningAction
import com.v7lthronyx.scamynx.domain.model.HardeningCategory
import com.v7lthronyx.scamynx.domain.model.HardeningImpact

private val NeonCyan = Color(0xFF00D4FF)
private val NeonGreen = Color(0xFF00FFAA)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonRed = Color(0xFFFF5C7A)
private val NeonOrange = Color(0xFFFFA14A)
private val NeonYellow = Color(0xFFF6DA61)

@Composable
fun DeviceHardeningRoute(
    onBack: () -> Unit,
    viewModel: DeviceHardeningViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DeviceHardeningScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::analyzeDevice,
        onApplyAction = viewModel::applyAction,
        onApplyAll = viewModel::applyAllRecommended,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceHardeningScreen(
    state: DeviceHardeningUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onApplyAction: (HardeningAction) -> Unit,
    onApplyAll: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_pulse")
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.02f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
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
                            text = stringResource(id = R.string.device_hardening_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Strengthen your device security",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGreen.copy(alpha = 0.7f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_navigate_back),
                            tint = NeonCyan,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.device_hardening_refresh),
                            tint = NeonCyan,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0A0A),
                            Color(0xFF0D1117),
                            Color(0xFF0A0A0A),
                        ),
                    ),
                )
                .drawBehind {
                    
                    val gridColor = NeonGreen.copy(alpha = bgAlpha)
                    val spacing = 80.dp.toPx()
                    for (y in 0..(size.height / spacing).toInt() + 1) {
                        val offset = if (y % 2 == 0) 0f else spacing / 2
                        for (x in 0..(size.width / spacing).toInt() + 1) {
                            drawCircle(
                                color = gridColor,
                                radius = 2.dp.toPx(),
                                center = Offset(x * spacing + offset, y * spacing),
                            )
                        }
                    }
                },
        ) {
            when {
                state.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            color = NeonGreen,
                            strokeWidth = 3.dp,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing device configuration...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NeonGreen.copy(alpha = 0.7f),
                        )
                    }
                }
                state.errorRes != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Card(
                            modifier = Modifier.padding(MaterialTheme.spacing.lg),
                            colors = CardDefaults.cardColors(
                                containerColor = NeonRed.copy(alpha = 0.1f),
                            ),
                            border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.3f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(MaterialTheme.spacing.lg),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = NeonRed,
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                                Text(
                                    text = stringResource(id = state.errorRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = NeonRed,
                                )
                            }
                        }
                    }
                }
                state.report != null -> {
                    DeviceHardeningContent(
                        report = state.report!!,
                        applyingActionId = state.applyingActionId,
                        onApplyAction = onApplyAction,
                        onApplyAll = onApplyAll,
                        modifier = Modifier.padding(paddingValues),
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceHardeningContent(
    report: com.v7lthronyx.scamynx.domain.model.DeviceHardeningReport,
    applyingActionId: String?,
    onApplyAction: (HardeningAction) -> Unit,
    onApplyAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        item {
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    NeonGreen.copy(alpha = 0.08f),
                                    Color(0xFF121212).copy(alpha = 0.95f),
                                ),
                            ),
                            shape = RoundedCornerShape(24.dp),
                        )
                        .padding(MaterialTheme.spacing.lg),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = stringResource(id = R.string.device_hardening_security_improvement),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    Text(
                                        text = "+${report.securityImprovement}",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Black,
                                        color = NeonGreen,
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "points",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = NeonGreen.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                NeonGreen.copy(alpha = 0.3f),
                                                NeonGreen.copy(alpha = 0.05f),
                                            ),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonCyan.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(MaterialTheme.spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                                Text(
                                    text = stringResource(
                                        id = R.string.device_hardening_recommended_count,
                                        report.recommendedActions.size,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = NeonCyan,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (report.recommendedActions.isNotEmpty()) {
            item {
                Button(
                    onClick = onApplyAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = applyingActionId == null,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color.Black,
                        disabledContainerColor = NeonGreen.copy(alpha = 0.3f),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Text(
                        text = stringResource(id = R.string.device_hardening_apply_all),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(NeonOrange, RoundedCornerShape(2.dp)),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                Text(
                    text = stringResource(id = R.string.device_hardening_recommended_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        itemsIndexed(report.recommendedActions) { index, action ->
            HardeningActionCard(
                action = action,
                isApplied = report.appliedActions.contains(action.id),
                isApplying = applyingActionId == action.id,
                onApply = { onApplyAction(action) },
                isRecommended = true,
                index = index,
            )
        }

        if (report.availableActions.size > report.recommendedActions.size) {
            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .background(NeonPurple, RoundedCornerShape(2.dp)),
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Text(
                        text = stringResource(id = R.string.device_hardening_all_actions_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }

            itemsIndexed(report.availableActions.filter { it !in report.recommendedActions }) { index, action ->
                HardeningActionCard(
                    action = action,
                    isApplied = report.appliedActions.contains(action.id),
                    isApplying = applyingActionId == action.id,
                    onApply = { onApplyAction(action) },
                    isRecommended = false,
                    index = index,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
        }
    }
}

@Composable
private fun HardeningActionCard(
    action: HardeningAction,
    isApplied: Boolean,
    isApplying: Boolean,
    onApply: () -> Unit,
    isRecommended: Boolean,
    index: Int,
) {
    val categoryColor = getCategoryColor(action.category)
    val impactColor = getImpactColor(action.impact)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isApplied) {
                NeonGreen.copy(alpha = 0.08f)
            } else {
                Color.Transparent
            },
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isApplied) {
                NeonGreen.copy(alpha = 0.3f)
            } else {
                categoryColor.copy(alpha = 0.15f)
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (!isApplied) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                categoryColor.copy(alpha = 0.05f),
                                Color(0xFF121212).copy(alpha = 0.9f),
                            ),
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                NeonGreen.copy(alpha = 0.08f),
                                NeonGreen.copy(alpha = 0.03f),
                            ),
                        )
                    },
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(MaterialTheme.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top,
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = categoryColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(action.category),
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Column {
                        Text(
                            text = action.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = action.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
                if (isApplied) {
                    Surface(
                        shape = CircleShape,
                        color = NeonGreen.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(id = R.string.device_hardening_applied),
                                tint = NeonGreen,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = categoryColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = getCategoryLabel(action.category),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = categoryColor,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = impactColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = getImpactLabel(action.impact),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = impactColor,
                    )
                }
                if (action.reversible) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeonPurple.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = stringResource(id = R.string.device_hardening_reversible),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = NeonPurple,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isApplied,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    Button(
                        onClick = onApply,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isApplying,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecommended) NeonOrange else NeonCyan,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f),
                        ),
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black,
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        }
                        Text(
                            text = stringResource(id = R.string.device_hardening_apply),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

private fun getCategoryLabel(category: HardeningCategory): String = when (category) {
    HardeningCategory.PRIVACY -> "Privacy"
    HardeningCategory.NETWORK -> "Network"
    HardeningCategory.APP_SECURITY -> "App Security"
    HardeningCategory.SYSTEM -> "System"
    HardeningCategory.DEVELOPER_OPTIONS -> "Developer"
}

private fun getImpactLabel(impact: HardeningImpact): String = when (impact) {
    HardeningImpact.LOW -> "Low Impact"
    HardeningImpact.MEDIUM -> "Medium Impact"
    HardeningImpact.HIGH -> "High Impact"
}

@Composable
private fun getCategoryColor(category: HardeningCategory): Color = when (category) {
    HardeningCategory.PRIVACY -> NeonPurple
    HardeningCategory.NETWORK -> NeonCyan
    HardeningCategory.APP_SECURITY -> NeonOrange
    HardeningCategory.SYSTEM -> NeonGreen
    HardeningCategory.DEVELOPER_OPTIONS -> NeonYellow
}

@Composable
private fun getImpactColor(impact: HardeningImpact): Color = when (impact) {
    HardeningImpact.LOW -> NeonGreen
    HardeningImpact.MEDIUM -> NeonYellow
    HardeningImpact.HIGH -> NeonOrange
}

@Composable
private fun getCategoryIcon(category: HardeningCategory): ImageVector = when (category) {
    HardeningCategory.PRIVACY -> Icons.Default.PrivacyTip
    HardeningCategory.NETWORK -> Icons.Default.NetworkCheck
    HardeningCategory.APP_SECURITY -> Icons.Default.Lock
    HardeningCategory.SYSTEM -> Icons.Default.PhoneAndroid
    HardeningCategory.DEVELOPER_OPTIONS -> Icons.Default.Build
}
