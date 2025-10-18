package com.v7lthronyx.scamynx.common.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens used across the SCAMYNX design system.
 * Gradually scaled to align with Material 3 rhythm while providing
 * larger steps for dashboard-style layouts.
 */
data class ScamynxSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 40.dp,
    val gutter: Dp = 56.dp,
)

internal val LocalScamynxSpacing = staticCompositionLocalOf { ScamynxSpacing() }

/**
 * Convenience accessor so calling sites can reference [MaterialTheme.spacing].
 */
val MaterialTheme.spacing: ScamynxSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalScamynxSpacing.current
