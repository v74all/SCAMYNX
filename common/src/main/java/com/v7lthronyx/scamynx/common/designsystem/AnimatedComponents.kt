package com.v7lthronyx.scamynx.common.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GlowingCard(
    modifier: Modifier = Modifier,
    glowColor: Color = ScamynxPrimary80,
    glowIntensity: Float = 0.6f,
    isGlowing: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowingCard")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = glowIntensity,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    val animatedGlowAlpha = if (isGlowing) glowAlpha else 0f

    Box(
        modifier = modifier
            .drawBehind {
                if (isGlowing) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = animatedGlowAlpha * 0.3f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                    )
                }
            }
            .border(
                width = if (isGlowing) 1.dp else 0.5.dp,
                color = if (isGlowing) glowColor.copy(alpha = animatedGlowAlpha) else glowColor.copy(alpha = 0.2f),
                shape = shape,
            )
            .clip(shape)
            .background(ScamynxGradients.cardElevated(), shape),
        content = content,
    )
}

@Composable
fun PulseIndicator(
    modifier: Modifier = Modifier,
    color: Color = ScamynxSignalGreen,
    size: Dp = 12.dp,
    isPulsing: Boolean = true,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = if (isPulsing) 0.3f else 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Box(
        modifier = modifier.size(size * 2),
        contentAlignment = Alignment.Center,
    ) {
        
        if (isPulsing) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(scale)
                    .alpha(alpha)
                    .background(color, CircleShape),
            )
        }
        
        Box(
            modifier = Modifier
                .size(size)
                .background(color, CircleShape),
        )
    }
}

@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier,
    lineSpacing: Dp = 4.dp,
    lineAlpha: Float = 0.06f,
    animated: Boolean = true,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) lineSpacing.value * 2 else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scanlineOffset",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = lineSpacing.toPx()
        var y = offset
        while (y < size.height) {
            drawLine(
                color = Color.White.copy(alpha = lineAlpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += spacingPx
        }
    }
}

@Composable
fun ParticleBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 20,
    particleColors: List<Color> = listOf(
        ScamynxParticleCyan,
        ScamynxParticleViolet,
        ScamynxParticleMagenta,
    ),
) {
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 4f + 2f,
                speed = Random.nextFloat() * 0.0005f + 0.0002f,
                color = particleColors.random(),
                initialPhase = Random.nextFloat() * 2f * PI.toFloat(),
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particleTime",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val animatedY = (particle.y + time * particle.speed) % 1f
            val wobble = sin(time * 0.01f + particle.initialPhase) * 0.02f
            val x = (particle.x + wobble).coerceIn(0f, 1f)

            drawCircle(
                color = particle.color.copy(alpha = 0.6f),
                radius = particle.size,
                center = Offset(x * size.width, animatedY * size.height),
            )
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val color: Color,
    val initialPhase: Float,
)

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    delayPerChar: Long = 50L,
    onComplete: () -> Unit = {},
) {
    var visibleChars by remember { mutableStateOf(0) }

    LaunchedEffect(text) {
        visibleChars = 0
        text.forEachIndexed { index, _ ->
            delay(delayPerChar)
            visibleChars = index + 1
        }
        onComplete()
    }

    Text(
        text = text.take(visibleChars),
        modifier = modifier,
        style = style,
        color = color,
    )
}

@Composable
fun HolographicSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "holo")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        ScamynxChromePurple30,
                        ScamynxChromePurple40,
                        ScamynxChromePurple30,
                    ),
                ),
                shape = shape,
            )
            .drawBehind {
                val shimmerWidth = size.width * 0.5f
                val shimmerX = shimmerOffset * (size.width + shimmerWidth) - shimmerWidth

                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            ScamynxHoloCyan.copy(alpha = 0.3f),
                            ScamynxHoloPink.copy(alpha = 0.3f),
                            Color.Transparent,
                        ),
                        startX = shimmerX,
                        endX = shimmerX + shimmerWidth,
                    ),
                )
            },
        content = content,
    )
}

@Composable
fun AnimatedRing(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 4.dp,
    colors: List<Color> = listOf(
        ScamynxPrimary80,
        ScamynxSecondary80,
        ScamynxTertiary80,
    ),
    rotationDuration: Int = 3000,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(rotationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringRotation",
    )

    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val radius = (this.size.minDimension - strokePx) / 2

        rotate(rotation) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = colors + colors.first().copy(alpha = 0f),
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(strokePx / 2, strokePx / 2),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun GlowOrb(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = ScamynxParticleCyan,
    glowRadius: Dp = 20.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbPulse",
    )

    Box(
        modifier = modifier.size(size + glowRadius * 2),
        contentAlignment = Alignment.Center,
    ) {
        
        Box(
            modifier = Modifier
                .size(size + glowRadius)
                .scale(pulse)
                .alpha(pulse * 0.5f)
                .blur(glowRadius / 2)
                .background(color, CircleShape),
        )
        
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ScamynxOrbCyanCenter,
                            color,
                            color.copy(alpha = 0.7f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 8.dp,
    trackColor: Color = ScamynxNeutral30,
    progressColor: Color = ScamynxPrimary80,
    glowEnabled: Boolean = true,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progressAnim",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "progressGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val radius = (this.size.minDimension - strokePx) / 2
        val topLeft = Offset(strokePx / 2, strokePx / 2)
        val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )

        if (glowEnabled && animatedProgress > 0) {
            drawArc(
                color = progressColor.copy(alpha = glowAlpha * 0.4f),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx * 3, cap = StrokeCap.Round),
            )
        }

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    progressColor.copy(alpha = 0.6f),
                    progressColor,
                    progressColor,
                ),
            ),
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = ScamynxNeutral25,
    highlightColor: Color = ScamynxNeutral40,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(baseColor)
            .drawBehind {
                val shimmerWidth = size.width * 0.4f
                val shimmerX = shimmerOffset * (size.width + shimmerWidth) - shimmerWidth

                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            highlightColor.copy(alpha = 0.4f),
                            highlightColor.copy(alpha = 0.6f),
                            highlightColor.copy(alpha = 0.4f),
                            Color.Transparent,
                        ),
                        startX = shimmerX,
                        endX = shimmerX + shimmerWidth,
                    ),
                )
            },
    )
}

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ScamynxNeutral20,
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShimmerEffect(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                    )
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        ShimmerEffect(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .size(height = 16.dp, width = 0.dp),
                        )
                        ShimmerEffect(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .size(height = 12.dp, width = 0.dp),
                        )
                    }
                }
                ShimmerEffect(
                    modifier = Modifier
                        .fillMaxWidth()
                        .size(height = 40.dp, width = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }
    }
}

@Composable
fun CyberBorderBox(
    modifier: Modifier = Modifier,
    borderColor: Color = ScamynxPrimary80,
    backgroundColor: Color = ScamynxNeutral15,
    cornerSize: Dp = 8.dp,
    isActive: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyberBorder")
    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dashOffset",
    )

    Box(
        modifier = modifier
            .drawBehind {
                val cornerPx = cornerSize.toPx()
                val strokeWidth = 2.dp.toPx()

                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(cornerPx, 0f)
                    lineTo(size.width - cornerPx, 0f)
                    lineTo(size.width, cornerPx)
                    lineTo(size.width, size.height - cornerPx)
                    lineTo(size.width - cornerPx, size.height)
                    lineTo(cornerPx, size.height)
                    lineTo(0f, size.height - cornerPx)
                    lineTo(0f, cornerPx)
                    close()
                }

                drawPath(
                    path = path,
                    color = backgroundColor,
                )

                if (isActive) {
                    drawPath(
                        path = path,
                        color = borderColor.copy(alpha = 0.8f),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                intervals = floatArrayOf(10f, 10f),
                                phase = dashOffset,
                            ),
                        ),
                    )
                } else {
                    drawPath(
                        path = path,
                        color = borderColor.copy(alpha = 0.3f),
                        style = Stroke(width = strokeWidth),
                    )
                }
            },
        content = content,
    )
}

@Composable
fun AnimatedBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ScamynxRiskRed,
    textColor: Color = Color.White,
    isPulsing: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "badgeScale",
    )

    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = color,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

@Composable
fun ScanningBeam(
    modifier: Modifier = Modifier,
    beamColor: Color = ScamynxPrimary80,
    scanDuration: Int = 2000,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanBeam")
    val beamPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(scanDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "beamPosition",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val beamY = beamPosition * size.height
        val beamHeight = 4.dp.toPx()
        val glowHeight = 20.dp.toPx()

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    beamColor.copy(alpha = 0.3f),
                    beamColor.copy(alpha = 0.5f),
                    beamColor.copy(alpha = 0.3f),
                    Color.Transparent,
                ),
                startY = beamY - glowHeight,
                endY = beamY + glowHeight,
            ),
            topLeft = Offset(0f, beamY - glowHeight),
            size = androidx.compose.ui.geometry.Size(size.width, glowHeight * 2),
        )

        drawRect(
            color = beamColor,
            topLeft = Offset(0f, beamY - beamHeight / 2),
            size = androidx.compose.ui.geometry.Size(size.width, beamHeight),
        )
    }
}

@Composable
fun RippleCircle(
    modifier: Modifier = Modifier,
    color: Color = ScamynxPrimary80,
    rippleCount: Int = 3,
    rippleDuration: Int = 2000,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")

    val ripples = (0 until rippleCount).map { index ->
        val delay = (rippleDuration / rippleCount) * index
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = rippleDuration,
                    delayMillis = delay,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Restart,
            ),
            label = "ripple$index",
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = minOf(size.width, size.height) / 2

        ripples.forEach { ripple ->
            val radius = ripple.value * maxRadius
            val alpha = 1f - ripple.value

            drawCircle(
                color = color.copy(alpha = alpha * 0.6f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}
