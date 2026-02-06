package com.v7lthronyx.scamynx.common.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RiskScoreGauge(
    score: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 16.dp,
    showScore: Boolean = true,
    showLabel: Boolean = true,
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.coerceIn(0f, 1f),
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "scoreAnim",
    )

    val scoreColor = when {
        score < 0.25f -> ScamynxSignalGreen
        score < 0.5f -> ScamynxRiskYellow
        score < 0.75f -> ScamynxRiskOrange
        else -> ScamynxRiskRed
    }

    val scoreLabel = when {
        score < 0.25f -> "Safe"
        score < 0.5f -> "Low Risk"
        score < 0.75f -> "Medium Risk"
        score < 0.9f -> "High Risk"
        else -> "Critical"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "gaugeGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val strokePx = strokeWidth.toPx()
                val radius = (this.size.minDimension - strokePx * 2) / 2
                val centerX = this.size.width / 2
                val centerY = this.size.height / 2 + radius * 0.1f

                drawArc(
                    color = ScamynxNeutral30,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )

                val sections = listOf(
                    Pair(ScamynxSignalGreen, 0.25f),
                    Pair(ScamynxRiskYellow, 0.25f),
                    Pair(ScamynxRiskOrange, 0.25f),
                    Pair(ScamynxRiskRed, 0.25f),
                )

                var startAngle = 180f
                sections.forEach { (color, fraction) ->
                    drawArc(
                        color = color.copy(alpha = 0.2f),
                        startAngle = startAngle,
                        sweepAngle = 180f * fraction,
                        useCenter = false,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokePx * 0.6f, cap = StrokeCap.Butt),
                    )
                    startAngle += 180f * fraction
                }

                drawArc(
                    color = scoreColor.copy(alpha = glowAlpha * 0.3f),
                    startAngle = 180f,
                    sweepAngle = 180f * animatedScore,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokePx * 2.5f, cap = StrokeCap.Round),
                )

                drawArc(
                    color = scoreColor,
                    startAngle = 180f,
                    sweepAngle = 180f * animatedScore,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )

                val needleAngle = 180f + (180f * animatedScore)
                val needleLength = radius * 0.7f
                val needleAngleRad = needleAngle * PI.toFloat() / 180f
                val needleEndX = centerX + needleLength * cos(needleAngleRad)
                val needleEndY = centerY + needleLength * sin(needleAngleRad)

                drawLine(
                    color = scoreColor.copy(alpha = 0.3f),
                    start = Offset(centerX, centerY),
                    end = Offset(needleEndX, needleEndY),
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                drawLine(
                    color = scoreColor,
                    start = Offset(centerX, centerY),
                    end = Offset(needleEndX, needleEndY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                drawCircle(
                    color = scoreColor,
                    radius = 10.dp.toPx(),
                    center = Offset(centerX, centerY),
                )
                drawCircle(
                    color = ScamynxNeutral10,
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, centerY),
                )
            }

            if (showScore) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = size / 6),
                ) {
                    Text(
                        text = "${(score * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = scoreColor.copy(alpha = 0.15f),
            ) {
                Text(
                    text = scoreLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scoreColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun ThreatBarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    barHeight: Dp = 24.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        data.forEach { (label, value) ->
            val animatedValue by animateFloatAsState(
                targetValue = value.coerceIn(0f, 1f),
                animationSpec = tween(1000, easing = FastOutSlowInEasing),
                label = "barAnim_$label",
            )

            val barColor = when {
                value < 0.25f -> ScamynxSignalGreen
                value < 0.5f -> ScamynxRiskYellow
                value < 0.75f -> ScamynxRiskOrange
                else -> ScamynxRiskRed
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${(value * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = barColor,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(barHeight / 2))
                        .background(ScamynxNeutral25),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedValue)
                            .height(barHeight)
                            .clip(RoundedCornerShape(barHeight / 2))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        barColor.copy(alpha = 0.7f),
                                        barColor,
                                    ),
                                ),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
fun ThreatRadarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    color: Color = ScamynxPrimary80,
) {
    val animatedData = data.map { (label, value) ->
        val animatedValue by animateFloatAsState(
            targetValue = value.coerceIn(0f, 1f),
            animationSpec = tween(1200, easing = FastOutSlowInEasing),
            label = "radarAnim_$label",
        )
        Pair(label, animatedValue)
    }

    val sides = data.size
    if (sides < 3) return

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val centerX = this.size.width / 2
            val centerY = this.size.height / 2
            val radius = minOf(centerX, centerY) * 0.75f
            val angleStep = (2 * PI / sides).toFloat()

            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { level ->
                val ringRadius = radius * level
                val path = Path()
                for (i in 0 until sides) {
                    val angle = -PI.toFloat() / 2 + i * angleStep
                    val x = centerX + ringRadius * cos(angle)
                    val y = centerY + ringRadius * sin(angle)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(
                    path = path,
                    color = ScamynxNeutral30,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            for (i in 0 until sides) {
                val angle = -PI.toFloat() / 2 + i * angleStep
                val endX = centerX + radius * cos(angle)
                val endY = centerY + radius * sin(angle)
                drawLine(
                    color = ScamynxNeutral40,
                    start = Offset(centerX, centerY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            val dataPath = Path()
            animatedData.forEachIndexed { i, (_, value) ->
                val angle = -PI.toFloat() / 2 + i * angleStep
                val dataRadius = radius * value
                val x = centerX + dataRadius * cos(angle)
                val y = centerY + dataRadius * sin(angle)
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()

            drawPath(
                path = dataPath,
                color = color.copy(alpha = 0.25f),
            )

            drawPath(
                path = dataPath,
                color = color,
                style = Stroke(width = 2.dp.toPx()),
            )

            animatedData.forEachIndexed { i, (_, value) ->
                val angle = -PI.toFloat() / 2 + i * angleStep
                val dataRadius = radius * value
                val x = centerX + dataRadius * cos(angle)
                val y = centerY + dataRadius * sin(angle)
                drawCircle(
                    color = color,
                    radius = 6.dp.toPx(),
                    center = Offset(x, y),
                )
                drawCircle(
                    color = ScamynxNeutral10,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y),
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}
