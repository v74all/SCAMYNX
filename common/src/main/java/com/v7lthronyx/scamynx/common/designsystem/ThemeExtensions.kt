package com.v7lthronyx.scamynx.common.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.v7lthronyx.scamynx.common.R
import androidx.compose.runtime.remember

/**
 * Extension properties for accessing SCAMYNX-specific colors beyond Material Theme.
 * These provide quick access to brand and status colors.
 */
object ScamynxColors {
    
    /** Risk level colors for scan results */
    val riskGreen: Color
        @Composable
        @ReadOnlyComposable
        get() = ScamynxSignalGreen
    
    val riskYellow: Color
        @Composable
        @ReadOnlyComposable
        get() = ScamynxRiskYellow
    
    val riskOrange: Color
        @Composable
        @ReadOnlyComposable
        get() = ScamynxRiskOrange
    
    val riskRed: Color
        @Composable
        @ReadOnlyComposable
        get() = ScamynxRiskRed
    
    /** Brand accent colors */
    val crimson: Color
        @Composable
        @ReadOnlyComposable
        get() = ScamynxPrimary80
    
    val ember: Color
        @Composable
        @ReadOnlyComposable
        get() = ScamynxSecondary80
    
    val midnight: Color
        @Composable
        @ReadOnlyComposable
        get() = ScamynxTertiary80
    
    /** Semantic colors */
    val info: Color
        @Composable
        @ReadOnlyComposable
        get() = ScamynxInfoBlue
    
    val warning: Color
        @Composable
        @ReadOnlyComposable
        get() = ScamynxWarningAmber
    
    val success: Color
        @Composable
        @ReadOnlyComposable
        get() = ScamynxSuccessTeal
}

/**
 * Returns color based on risk score (0.0 - 1.0)
 * - 0.0 - 0.3: Green (Safe)
 * - 0.3 - 0.5: Yellow (Low risk)
 * - 0.5 - 0.7: Orange (Medium risk)
 * - 0.7 - 1.0: Red (High risk)
 */
@Composable
@ReadOnlyComposable
fun riskScoreColor(score: Float): Color = when {
    score < 0.3f -> ScamynxColors.riskGreen
    score < 0.5f -> ScamynxColors.riskYellow
    score < 0.7f -> ScamynxColors.riskOrange
    else -> ScamynxColors.riskRed
}

/**
 * Returns a human-readable risk level text
 */
@Composable
@ReadOnlyComposable
fun riskScoreLabel(score: Float): String {
    @StringRes val labelRes = when {
        score < 0.3f -> R.string.risk_level_safe
        score < 0.5f -> R.string.risk_level_low
        score < 0.7f -> R.string.risk_level_medium
        score < 0.9f -> R.string.risk_level_high
        else -> R.string.risk_level_critical
    }
    return stringResource(id = labelRes)
}

/**
 * Provides a subtle brand gradient suitable for cards or headers.
 */
@Composable
fun brandGradient(): Brush = Brush.linearGradient(
    colors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.58f),
    ),
    start = Offset.Zero,
    end = Offset(x = 480f, y = 520f),
)

/**
 * Convenience helper to fetch a tonal surface tinted by elevation.
 */
@Composable
fun elevatedSurface(elevation: Dp): Color = MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)

/**
 * Gradient helpers for elevated visuals across the app.
 */
object ScamynxGradients {
    /**
     * Vibrant gradient for hero or header sections.
     */
    @Composable
    fun hero(): Brush {
        val scheme = MaterialTheme.colorScheme
        return remember(scheme.primary, scheme.secondary, scheme.tertiary) {
            Brush.linearGradient(
                colors = listOf(
                    scheme.primary.copy(alpha = 0.92f),
                    scheme.primaryContainer.copy(alpha = 0.78f),
                    scheme.tertiary.copy(alpha = 0.7f),
                    scheme.secondary.copy(alpha = 0.66f),
                ),
                start = Offset.Zero,
                end = Offset(x = 860f, y = 540f),
            )
        }
    }

    /**
     * Subtle gradient for cards that need more depth than flat surfaces.
     */
    @Composable
    fun card(): Brush {
        val scheme = MaterialTheme.colorScheme
        return remember(scheme.surface, scheme.surfaceVariant, scheme.primary) {
            Brush.linearGradient(
                colors = listOf(
                    scheme.surface,
                    scheme.surfaceVariant.copy(alpha = 0.78f),
                    scheme.primary.copy(alpha = 0.14f),
                    scheme.secondaryContainer.copy(alpha = 0.18f),
                ),
                start = Offset(x = 180f, y = 0f),
                end = Offset(x = 780f, y = 620f),
            )
        }
    }

    /**
     * Background wash used behind scrollable content to soften transitions.
     */
    @Composable
    fun backdrop(): Brush {
        val scheme = MaterialTheme.colorScheme
        return remember(scheme.surface, scheme.secondaryContainer) {
            Brush.radialGradient(
                colors = listOf(
                    scheme.surface,
                    scheme.primaryContainer.copy(alpha = 0.38f),
                    scheme.secondaryContainer.copy(alpha = 0.28f),
                ),
                center = Offset(0.35f, 0.25f),
                radius = 950f,
            )
        }
    }

    /**
     * Elevated card gradient with more depth for floating components.
     */
    @Composable
    fun cardElevated(): Brush {
        val scheme = MaterialTheme.colorScheme
        return remember(scheme.surface, scheme.surfaceVariant, scheme.primary) {
            Brush.linearGradient(
                colors = listOf(
                    scheme.surfaceVariant.copy(alpha = 0.95f),
                    scheme.surface,
                    scheme.primary.copy(alpha = 0.08f),
                    scheme.tertiaryContainer.copy(alpha = 0.12f),
                ),
                start = Offset(x = 0f, y = 0f),
                end = Offset(x = 600f, y = 400f),
            )
        }
    }
}
