package com.v7lthronyx.scamynx.ui.breachmonitoring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.common.designsystem.spacing
import com.v7lthronyx.scamynx.domain.model.BreachCheckResult
import com.v7lthronyx.scamynx.domain.model.BreachExposure
import com.v7lthronyx.scamynx.domain.model.BreachSeverity
import java.text.NumberFormat

private val NeonCyan = Color(0xFF00D4FF)
private val NeonGreen = Color(0xFF00FFAA)
private val NeonPurple = Color(0xFFBB86FC)
private val NeonRed = Color(0xFFFF5C7A)
private val NeonOrange = Color(0xFFFFA14A)
private val NeonYellow = Color(0xFFF6DA61)

@Composable
fun BreachMonitoringRoute(
    onBack: () -> Unit,
    viewModel: BreachMonitoringViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BreachMonitoringScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::loadMonitoringReport,
        onCheckEmail = viewModel::checkEmail,
        onCheckPhone = viewModel::checkPhoneNumber,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreachMonitoringScreen(
    state: BreachMonitoringUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCheckEmail: (String) -> Unit,
    onCheckPhone: (String) -> Unit,
) {
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }

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
                            text = stringResource(id = R.string.breach_monitoring_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Check your exposure status",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonRed.copy(alpha = 0.7f),
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
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isLoading,
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = NeonCyan,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(id = R.string.breach_monitoring_refresh),
                                tint = NeonCyan,
                            )
                        }
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
                    val gridColor = NeonRed.copy(alpha = bgAlpha)
                    val spacing = 50.dp.toPx()
                    for (i in 0..(size.width / spacing).toInt()) {
                        drawLine(
                            color = gridColor,
                            start = Offset(i * spacing, 0f),
                            end = Offset(i * spacing, size.height),
                            strokeWidth = 0.5f,
                        )
                    }
                    for (i in 0..(size.height / spacing).toInt()) {
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, i * spacing),
                            end = Offset(size.width, i * spacing),
                            strokeWidth = 0.5f,
                        )
                    }
                },
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(MaterialTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                item { HeroCard() }

                item {
                    CheckInputCard(
                        icon = Icons.Default.Email,
                        title = stringResource(id = R.string.breach_monitoring_check_email),
                        inputValue = emailInput,
                        onInputChange = { emailInput = it },
                        inputLabel = stringResource(id = R.string.breach_monitoring_email_label),
                        isChecking = state.checkingEmail,
                        onCheck = { onCheckEmail(emailInput) },
                        keyboardType = KeyboardType.Email,
                        accentColor = NeonCyan,
                    )
                }

                state.emailResult?.let { result ->
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            BreachCheckResultCard(result = result)
                        }
                    }
                }

                item {
                    CheckInputCard(
                        icon = Icons.Default.Phone,
                        title = stringResource(id = R.string.breach_monitoring_check_phone),
                        inputValue = phoneInput,
                        onInputChange = { phoneInput = it },
                        inputLabel = stringResource(id = R.string.breach_monitoring_phone_label),
                        isChecking = state.checkingPhone,
                        onCheck = { onCheckPhone(phoneInput) },
                        keyboardType = KeyboardType.Phone,
                        accentColor = NeonPurple,
                    )
                }

                state.phoneResult?.let { result ->
                    item {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            BreachCheckResultCard(result = result)
                        }
                    }
                }

                state.report?.let { report ->
                    item {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                        SectionHeader(
                            title = stringResource(id = R.string.breach_monitoring_summary_title),
                            color = NeonOrange,
                        )
                    }

                    item {
                        MonitoringSummaryCard(
                            totalExposures = report.totalExposures,
                            criticalExposures = report.criticalExposures,
                            overallRiskScore = report.overallRiskScore,
                            monitoringEnabled = report.monitoringEnabled,
                        )
                    }

                    if (report.emailChecks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = stringResource(id = R.string.breach_monitoring_email_checks),
                                color = NeonCyan,
                            )
                        }
                        items(report.emailChecks) { check ->
                            BreachCheckResultCard(result = check)
                        }
                    }

                    if (report.phoneChecks.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Phone Checks", color = NeonPurple)
                        }
                        items(report.phoneChecks) { check ->
                            BreachCheckResultCard(result = check)
                        }
                    }
                }

                state.errorRes?.let { errorRes ->
                    item { ErrorCard(errorRes = errorRes) }
                }

                item { Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.2f)),
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            NeonRed.copy(alpha = 0.08f),
                            Color(0xFF121212).copy(alpha = 0.95f),
                        ),
                    ),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(MaterialTheme.spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    NeonRed.copy(alpha = 0.3f),
                                    NeonRed.copy(alpha = 0.05f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = NeonRed,
                    )
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.breach_monitoring_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInputCard(
    icon: ImageVector,
    title: String,
    inputValue: String,
    onInputChange: (String) -> Unit,
    inputLabel: String,
    isChecking: Boolean,
    onCheck: () -> Unit,
    keyboardType: KeyboardType,
    accentColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.06f),
                            Color(0xFF121212).copy(alpha = 0.95f),
                        ),
                    ),
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(MaterialTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = accentColor,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }

            OutlinedTextField(
                value = inputValue,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(inputLabel, color = Color.White.copy(alpha = 0.6f)) },
                enabled = !isChecking,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = accentColor.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
                    cursorColor = accentColor,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { if (inputValue.isNotBlank() && !isChecking) onCheck() },
                ),
                trailingIcon = {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = accentColor,
                        )
                    }
                },
            )

            FilledTonalButton(
                onClick = onCheck,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isChecking && inputValue.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                }
                Text(
                    text = stringResource(id = R.string.breach_monitoring_check_button),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun BreachCheckResultCard(result: BreachCheckResult) {
    val numberFormatter = remember { NumberFormat.getIntegerInstance() }
    val isExposed = result.isExposed

    val statusColor = if (isExposed) NeonRed else NeonGreen
    val statusIcon = if (isExposed) Icons.Default.Warning else Icons.Default.CheckCircle

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.08f),
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Column {
                        Text(
                            text = result.identifier.takeIf { it.isNotBlank() } ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = result.identifierType.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = riskScoreColor(result.riskScore.toFloat()).copy(alpha = 0.2f),
                ) {
                    Text(
                        text = "${(result.riskScore * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = riskScoreColor(result.riskScore.toFloat()),
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = statusColor.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (isExposed) {
                        stringResource(
                            id = R.string.breach_monitoring_exposed_count,
                            numberFormatter.format(result.totalBreachCount),
                        )
                    } else {
                        stringResource(id = R.string.breach_monitoring_not_exposed)
                    },
                    modifier = Modifier.padding(MaterialTheme.spacing.sm),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = statusColor,
                )
            }

            if (result.exposures.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                Text(
                    text = "Breach Details",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                result.exposures.take(5).forEach { exposure ->
                    ExposureItem(exposure = exposure)
                }
                if (result.exposures.size > 5) {
                    Text(
                        text = "+${result.exposures.size - 5} more breaches",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
                    )
                }
            }

            if (result.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                Text(
                    text = stringResource(id = R.string.breach_monitoring_recommendations),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                result.recommendations.forEach { recommendation ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonCyan),
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExposureItem(exposure: BreachExposure) {
    val severityColor = when (exposure.severity) {
        BreachSeverity.LOW -> NeonGreen
        BreachSeverity.MEDIUM -> NeonYellow
        BreachSeverity.HIGH -> NeonOrange
        BreachSeverity.CRITICAL -> NeonRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(severityColor),
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.xs))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exposure.breachName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
            exposure.breachDate?.let { date ->
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = severityColor.copy(alpha = 0.2f),
        ) {
            Text(
                text = exposure.severity.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = severityColor,
            )
        }
    }
}

@Composable
private fun MonitoringSummaryCard(
    totalExposures: Int,
    criticalExposures: Int,
    overallRiskScore: Double,
    monitoringEnabled: Boolean,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = overallRiskScore.toFloat(),
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "risk_progress",
    )

    val riskColor = riskScoreColor(overallRiskScore.toFloat())

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            riskColor.copy(alpha = 0.08f),
                            Color(0xFF121212).copy(alpha = 0.95f),
                        ),
                    ),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(MaterialTheme.spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Risk Score",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                    Text(
                        text = "${(overallRiskScore * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = riskColor,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFF1A1A1A),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 10.dp.toPx()),
                            )
                            drawArc(
                                color = riskColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = riskColor,
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(
                    label = stringResource(id = R.string.breach_monitoring_total_exposures),
                    value = "$totalExposures",
                    color = if (totalExposures > 0) NeonRed else NeonGreen,
                )
                StatItem(
                    label = "Critical",
                    value = "$criticalExposures",
                    color = if (criticalExposures > 0) NeonRed else Color.White.copy(alpha = 0.5f),
                )
                StatItem(
                    label = "Monitoring",
                    value = if (monitoringEnabled) "Active" else "Inactive",
                    color = if (monitoringEnabled) NeonGreen else Color.White.copy(alpha = 0.5f),
                )
            }

            if (criticalExposures > 0) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NeonRed.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(MaterialTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NeonRed,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                        Text(
                            text = stringResource(
                                id = R.string.breach_monitoring_critical_exposures,
                                criticalExposures,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonRed,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
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
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ErrorCard(errorRes: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NeonRed.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.3f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = NeonRed,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
            Text(
                text = stringResource(id = errorRes),
                style = MaterialTheme.typography.bodyMedium,
                color = NeonRed,
            )
        }
    }
}

@Composable
private fun riskScoreColor(score: Float): Color = when {
    score < 0.2f -> NeonGreen
    score < 0.4f -> NeonYellow
    score < 0.6f -> NeonOrange
    else -> NeonRed
}
