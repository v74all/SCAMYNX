package com.v7lthronyx.scamynx.ui.results.components

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.v7lthronyx.scamynx.common.designsystem.ScamynxBioBlue
import com.v7lthronyx.scamynx.common.designsystem.ScamynxChromePurple50
import com.v7lthronyx.scamynx.common.designsystem.ScamynxGradients
import com.v7lthronyx.scamynx.common.designsystem.ScamynxHoloCyan
import com.v7lthronyx.scamynx.common.designsystem.ScamynxHoloPink
import com.v7lthronyx.scamynx.common.designsystem.ScamynxNeutral30
import com.v7lthronyx.scamynx.common.designsystem.ScamynxPrimary80
import com.v7lthronyx.scamynx.common.designsystem.ScamynxRiskRed
import com.v7lthronyx.scamynx.common.designsystem.ScamynxRiskYellow
import com.v7lthronyx.scamynx.common.designsystem.ScamynxSignalGreen
import com.v7lthronyx.scamynx.common.designsystem.spacing
import com.v7lthronyx.scamynx.domain.model.MlReport
import kotlinx.coroutines.delay

@Composable
fun MlAnalysisCard(
    mlReport: MlReport,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ScamynxGradients.cardElevated())
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ScamynxHoloCyan.copy(alpha = 0.3f),
                            ScamynxChromePurple50.copy(alpha = 0.2f),
                            ScamynxHoloPink.copy(alpha = 0.3f),
                        ),
                    ),
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(MaterialTheme.spacing.lg),
        ) {
            Column {
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "aiGlow")
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 0.7f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "glow",
                        )

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .blur(8.dp)
                                .alpha(glowAlpha)
                                .background(ScamynxHoloCyan, CircleShape),
                        )
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI Analysis",
                            tint = ScamynxHoloCyan,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Security Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Machine Learning Detection",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    ConfidenceRing(
                        probability = mlReport.probability.toFloat(),
                        modifier = Modifier.size(64.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                ProbabilityBar(
                    probability = mlReport.probability.toFloat(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (mlReport.topFeatures.isNotEmpty()) {
                    Text(
                        text = "Key Detection Factors",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    mlReport.topFeatures.take(3).forEachIndexed { index, feature ->
                        FeatureBar(
                            featureName = formatFeatureName(feature.feature),
                            value = feature.weight.toFloat(),
                            delay = index * 100L,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                if (mlReport.topFeatures.size > 3) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isExpanded) "Show Less" else "Show All Features",
                            style = MaterialTheme.typography.labelMedium,
                            color = ScamynxPrimary80,
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = ScamynxPrimary80,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            mlReport.topFeatures.drop(3).forEachIndexed { index, feature ->
                                FeatureBar(
                                    featureName = formatFeatureName(feature.feature),
                                    value = feature.weight.toFloat(),
                                    delay = (index + 3) * 100L,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfidenceRing(
    probability: Float,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = probability,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "confidence",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ringGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    val ringColor = when {
        probability < 0.3f -> ScamynxSignalGreen
        probability < 0.6f -> ScamynxRiskYellow
        else -> ScamynxRiskRed
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        
        Canvas(
            modifier = Modifier
                .size(64.dp)
                .blur(4.dp)
                .alpha(glowPulse * 0.5f),
        ) {
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
            )
        }

        Canvas(modifier = Modifier.size(56.dp)) {
            drawArc(
                color = ScamynxNeutral30,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        Canvas(modifier = Modifier.size(56.dp)) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        ringColor.copy(alpha = 0.6f),
                        ringColor,
                        ringColor,
                    ),
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        Text(
            text = "${(probability * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = ringColor,
        )
    }
}

@Composable
private fun ProbabilityBar(
    probability: Float,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = probability,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "probBar",
    )

    val barColor = when {
        probability < 0.3f -> ScamynxSignalGreen
        probability < 0.6f -> ScamynxRiskYellow
        else -> ScamynxRiskRed
    }

    val riskLabel = when {
        probability < 0.3f -> "Low Risk"
        probability < 0.6f -> "Medium Risk"
        probability < 0.8f -> "High Risk"
        else -> "Critical Risk"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Threat Probability",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = riskLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = barColor,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ScamynxNeutral30),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                barColor.copy(alpha = 0.7f),
                                barColor,
                            ),
                        ),
                        shape = RoundedCornerShape(4.dp),
                    ),
            )
        }
    }
}

@Composable
private fun FeatureBar(
    featureName: String,
    value: Float,
    delay: Long = 0,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay)
        visible = true
    }

    val animatedValue by animateFloatAsState(
        targetValue = if (visible) value else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "featureValue",
    )

    val barColor = when {
        value > 0.7f -> ScamynxRiskRed
        value > 0.4f -> ScamynxRiskYellow
        else -> ScamynxBioBlue
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        
        if (value > 0.7f) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = ScamynxRiskRed,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = featureName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ScamynxNeutral30.copy(alpha = 0.5f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedValue)
                    .height(4.dp)
                    .background(barColor, RoundedCornerShape(2.dp)),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = String.format("%.0f%%", value * 100),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = barColor,
            modifier = Modifier.width(36.dp),
        )
    }
}

private fun formatFeatureName(feature: String): String {
    return feature
        .replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}
