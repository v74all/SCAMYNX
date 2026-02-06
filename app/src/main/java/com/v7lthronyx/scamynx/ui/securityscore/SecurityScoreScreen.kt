package com.v7lthronyx.scamynx.ui.securityscore

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.common.designsystem.spacing
import com.v7lthronyx.scamynx.domain.model.SecurityCategory
import com.v7lthronyx.scamynx.domain.model.SecurityStatus
import java.text.NumberFormat

private val NeonCyan = Color(0xFF00D4FF)
private val NeonGreen = Color(0xFF00FFAA)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonRed = Color(0xFFFF5C7A)
private val NeonOrange = Color(0xFFFFA14A)
private val NeonYellow = Color(0xFFF6DA61)

@Composable
fun SecurityScoreRoute(
    onBack: () -> Unit,
    viewModel: SecurityScoreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SecurityScoreScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScoreScreen(
    state: SecurityScoreUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_pulse")
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.08f,
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
                            text = stringResource(id = R.string.security_score_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Your device security posture",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan.copy(alpha = 0.7f),
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
                            contentDescription = stringResource(id = R.string.security_score_refresh),
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
                    
                    val gridColor = NeonCyan.copy(alpha = bgAlpha)
                    val gridSpacing = 60.dp.toPx()
                    for (x in 0..(size.width / gridSpacing).toInt()) {
                        drawLine(
                            color = gridColor,
                            start = Offset(x * gridSpacing, 0f),
                            end = Offset(x * gridSpacing, size.height),
                            strokeWidth = 0.5f,
                        )
                    }
                    for (y in 0..(size.height / gridSpacing).toInt()) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y * gridSpacing),
                            end = Offset(size.width, y * gridSpacing),
                            strokeWidth = 0.5f,
                        )
                    }
                },
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                strokeWidth = 3.dp,
                            )
                            Text(
                                text = "Analyzing security posture...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeonCyan.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
                state.errorRes != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center,
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
                    SecurityScoreContent(
                        report = state.report!!,
                        modifier = Modifier.padding(paddingValues),
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityScoreContent(
    report: com.v7lthronyx.scamynx.domain.model.SecurityScoreReport,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shareChooserTitle = stringResource(id = R.string.security_score_share)
    
    val onShareScore: () -> Unit = remember(report, context, shareChooserTitle) {
        {
            val shareText = report.shareableBadge.shareableText
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "My Security Score")
            }
            val shareIntent = Intent.createChooser(sendIntent, shareChooserTitle)
            context.startActivity(shareIntent)
        }
    }
    
    val numberFormatter = remember { NumberFormat.getIntegerInstance() }
    val animatedProgress by animateFloatAsState(
        targetValue = report.overallScore / 100f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "score_progress",
    )

    val statusColor = getStatusColor(report.status)

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MaterialTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        item {
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF121212).copy(alpha = 0.9f),
                                    Color(0xFF0A0A0A).copy(alpha = 0.95f),
                                ),
                            ),
                            shape = RoundedCornerShape(28.dp),
                        )
                        .padding(MaterialTheme.spacing.xl),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .drawBehind {
                                    
                                    drawCircle(
                                        color = statusColor.copy(alpha = glowAlpha * 0.3f),
                                        radius = size.minDimension / 2 + 20.dp.toPx(),
                                    )
                                    
                                    drawCircle(
                                        color = Color(0xFF1A1A1A),
                                        radius = size.minDimension / 2,
                                        style = Stroke(width = 14.dp.toPx()),
                                    )
                                    
                                    drawArc(
                                        color = statusColor,
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedProgress,
                                        useCenter = false,
                                        style = Stroke(
                                            width = 14.dp.toPx(),
                                            cap = StrokeCap.Round,
                                        ),
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "${report.overallScore}",
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Black,
                                    color = statusColor,
                                )
                                Text(
                                    text = stringResource(id = getStatusLabelRes(report.status)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor.copy(alpha = 0.8f),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

                        val badgeColor = try {
                            Color(android.graphics.Color.parseColor(report.shareableBadge.badgeColor))
                        } catch (e: Exception) {
                            statusColor
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = badgeColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(MaterialTheme.spacing.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                                    Text(
                                        text = report.shareableBadge.badgeText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                    )
                                }
                                IconButton(onClick = onShareScore) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = stringResource(id = R.string.security_score_share),
                                        tint = badgeColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(24.dp)
                        .background(NeonCyan, RoundedCornerShape(2.dp)),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                Text(
                    text = stringResource(id = R.string.security_score_components_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        itemsIndexed(report.components) { index, component ->
            SecurityComponentCard(
                component = component,
                index = index,
            )
        }

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
                    text = stringResource(id = R.string.security_score_recommendations_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        itemsIndexed(report.topRecommendations) { index, recommendation ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.15f)),
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    NeonPurple.copy(alpha = 0.08f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(MaterialTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = NeonPurple.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonPurple,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))
        }
    }
}

@Composable
private fun SecurityComponentCard(
    component: com.v7lthronyx.scamynx.domain.model.SecurityScoreComponent,
    index: Int,
) {
    val statusColor = getStatusColor(component.status)
    val animatedProgress by animateFloatAsState(
        targetValue = component.score / 100f,
        animationSpec = tween(1000 + (index * 100), easing = FastOutSlowInEasing),
        label = "component_progress",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.06f),
                            Color(0xFF121212).copy(alpha = 0.9f),
                        ),
                    ),
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(MaterialTheme.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(component.category),
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Text(
                        text = getCategoryLabel(component.category),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f),
                ) {
                    Text(
                        text = "${component.score}/100",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1A1A1A)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    statusColor.copy(alpha = 0.7f),
                                    statusColor,
                                ),
                            ),
                        ),
                )
            }

            if (component.issues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                component.issues.forEach { issue ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NeonOrange,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        Text(
                            text = issue.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonOrange.copy(alpha = 0.9f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getStatusColor(status: SecurityStatus): Color = when (status) {
    SecurityStatus.EXCELLENT -> NeonCyan
    SecurityStatus.GOOD -> NeonGreen
    SecurityStatus.FAIR -> NeonYellow
    SecurityStatus.POOR -> NeonOrange
    SecurityStatus.CRITICAL -> NeonRed
}

private fun getStatusLabelRes(status: SecurityStatus): Int = when (status) {
    SecurityStatus.EXCELLENT -> R.string.security_status_excellent
    SecurityStatus.GOOD -> R.string.security_status_good
    SecurityStatus.FAIR -> R.string.security_status_fair
    SecurityStatus.POOR -> R.string.security_status_poor
    SecurityStatus.CRITICAL -> R.string.security_status_critical
}

private fun getCategoryLabel(category: SecurityCategory): String = when (category) {
    SecurityCategory.PRIVACY_RADAR -> "Privacy Radar"
    SecurityCategory.PASSWORD_SECURITY -> "Password Security"
    SecurityCategory.WIFI_SECURITY -> "Wi-Fi Security"
    SecurityCategory.APP_PERMISSIONS -> "App Permissions"
    SecurityCategory.DEVICE_HARDENING -> "Device Hardening"
    SecurityCategory.BREACH_EXPOSURE -> "Breach Exposure"
    SecurityCategory.ANTI_PHISHING -> "Anti-Phishing"
    SecurityCategory.SOCIAL_ENGINEERING -> "Social Engineering"
}

@Composable
private fun getCategoryIcon(category: SecurityCategory): androidx.compose.ui.graphics.vector.ImageVector = when (category) {
    SecurityCategory.PRIVACY_RADAR -> Icons.Default.Security
    SecurityCategory.PASSWORD_SECURITY -> Icons.Default.Shield
    SecurityCategory.WIFI_SECURITY -> Icons.Default.Security
    SecurityCategory.APP_PERMISSIONS -> Icons.Default.Security
    SecurityCategory.DEVICE_HARDENING -> Icons.Default.Shield
    SecurityCategory.BREACH_EXPOSURE -> Icons.Default.Warning
    SecurityCategory.ANTI_PHISHING -> Icons.Default.Shield
    SecurityCategory.SOCIAL_ENGINEERING -> Icons.Default.Security
}
