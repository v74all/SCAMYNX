package com.v7lthronyx.scamynx.common.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Dark color scheme for SCAMYNX.
 * Features cybersecurity accents on deep neutrals with high contrast.
 */
private val DarkColorScheme = darkColorScheme(
    primary = ScamynxPrimary80,
    onPrimary = Color.White,
    primaryContainer = ScamynxPrimary30,
    onPrimaryContainer = ScamynxPrimary95,
    
    secondary = ScamynxSecondary80,
    onSecondary = ScamynxSecondary20,
    secondaryContainer = ScamynxSecondary30,
    onSecondaryContainer = ScamynxSecondary90,
    
    tertiary = ScamynxTertiary80,
    onTertiary = ScamynxTertiary20,
    tertiaryContainer = ScamynxTertiary30,
    onTertiaryContainer = ScamynxTertiary90,
    
    // Background & Surface
    background = ScamynxBackgroundDark,
    onBackground = ScamynxNeutral90,
    surface = ScamynxSurfaceDark,
    onSurface = ScamynxNeutral90,
    surfaceVariant = ScamynxSurfaceDarkVariant,
    onSurfaceVariant = ScamynxNeutral70,
    surfaceTint = ScamynxPrimary80,
    
    // Inverse colors
    inverseSurface = ScamynxNeutral90,
    inverseOnSurface = ScamynxNeutral20,
    inversePrimary = ScamynxPrimary40,
    
    // Error states
    error = ScamynxRiskRed,
    onError = Color.White,
    errorContainer = ScamynxRiskRedDark,
    onErrorContainer = ScamynxRiskRedLight,
    
    // Outline & Borders
    outline = ScamynxNeutral50,
    outlineVariant = ScamynxNeutral40,
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.72f),
)

/**
 * Light color scheme for SCAMYNX.
 * Clean and professional look balancing deep purples with refined cyans.
 */
private val LightColorScheme = lightColorScheme(
    // Primary - Crimson focus
    primary = ScamynxPrimary40,
    onPrimary = Color.White,
    primaryContainer = ScamynxPrimary90,
    onPrimaryContainer = ScamynxPrimary10,
    
    // Secondary - Ember accents
    secondary = ScamynxSecondary40,
    onSecondary = Color.White,
    secondaryContainer = ScamynxSecondary90,
    onSecondaryContainer = ScamynxSecondary20,
    
    // Tertiary - Midnight violet
    tertiary = ScamynxTertiary40,
    onTertiary = Color.White,
    tertiaryContainer = ScamynxTertiary90,
    onTertiaryContainer = ScamynxTertiary20,
    
    // Background & Surface
    background = ScamynxBackgroundLight,
    onBackground = ScamynxNeutral20,
    surface = ScamynxSurfaceLight,
    onSurface = ScamynxNeutral20,
    surfaceVariant = ScamynxSurfaceLightVariant,
    onSurfaceVariant = ScamynxNeutral50,
    surfaceTint = ScamynxPrimary40,
    
    // Inverse colors
    inverseSurface = ScamynxNeutral30,
    inverseOnSurface = ScamynxNeutral95,
    inversePrimary = ScamynxPrimary80,
    
    // Error states
    error = ScamynxRiskRed,
    onError = Color.White,
    errorContainer = ScamynxRiskRedLight,
    onErrorContainer = ScamynxRiskRedDark,
    
    // Outline & Borders
    outline = ScamynxNeutral60,
    outlineVariant = ScamynxNeutral80,
    
    // Scrim
    scrim = Color.Black.copy(alpha = 0.45f),
)

/**
 * SCAMYNX Material Theme wrapper.
 * 
 * @param useDarkTheme Whether to use dark theme (defaults to system preference)
 * @param dynamicColor Whether to use dynamic color on Android 12+ (defaults to true)
 * @param content The composable content to wrap with the theme
 */
@Composable
fun ScamynxTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val spacing = remember { ScamynxSpacing() }
    val isPersian = remember(configuration) {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
        locale?.language.equals("fa", ignoreCase = true)
    }
    val colorScheme = remember(useDarkTheme, dynamicColor, context) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val dynamicScheme = if (useDarkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
                // Merge dynamic colors with brand palette
                mergeWithBrandPalette(dynamicScheme, useDarkTheme)
            }

            useDarkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    }
    val typography = remember(isPersian) {
        scamynxTypography(localeAwareFontFamily(isPersian))
    }

    CompositionLocalProvider(LocalScamynxSpacing provides spacing) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = ScamynxShapes,
            content = content,
        )
    }
}

/**
 * Merges dynamic color scheme with SCAMYNX brand colors.
 * Ensures brand consistency while respecting user's wallpaper colors.
 */
private fun mergeWithBrandPalette(
    base: ColorScheme,
    useDarkTheme: Boolean,
): ColorScheme = base.copy(
    // Override primary with brand colors
    primary = if (useDarkTheme) ScamynxPrimary80 else ScamynxPrimary40,
    onPrimary = if (useDarkTheme) Color.White else Color.White,
    primaryContainer = if (useDarkTheme) ScamynxPrimary30 else ScamynxPrimary95,
    onPrimaryContainer = if (useDarkTheme) ScamynxPrimary95 else ScamynxPrimary10,
    
    // Keep secondary as ember accent
    secondary = if (useDarkTheme) ScamynxSecondary80 else ScamynxSecondary40,
    onSecondary = Color.White,
    secondaryContainer = if (useDarkTheme) ScamynxSecondary30 else ScamynxSecondary90,
    onSecondaryContainer = if (useDarkTheme) ScamynxSecondary90 else ScamynxSecondary20,
    
    // Tertiary as midnight violet
    tertiary = if (useDarkTheme) ScamynxTertiary80 else ScamynxTertiary40,
    onTertiary = if (useDarkTheme) ScamynxTertiary20 else Color.White,
    tertiaryContainer = if (useDarkTheme) ScamynxTertiary30 else ScamynxTertiary90,
    onTertiaryContainer = if (useDarkTheme) ScamynxTertiary90 else ScamynxTertiary20,
    
    // Override surfaces for consistency
    surface = if (useDarkTheme) ScamynxSurfaceDark else ScamynxSurfaceLight,
    onSurface = if (useDarkTheme) ScamynxNeutral90 else ScamynxNeutral10,
    surfaceVariant = if (useDarkTheme) ScamynxSurfaceDarkVariant else ScamynxSurfaceLightVariant,
    onSurfaceVariant = if (useDarkTheme) ScamynxNeutral70 else ScamynxNeutral40,
    background = if (useDarkTheme) ScamynxBackgroundDark else ScamynxBackgroundLight,
    onBackground = if (useDarkTheme) ScamynxNeutral90 else ScamynxNeutral10,
    
    // Error state with brand red
    error = ScamynxRiskRed,
    errorContainer = if (useDarkTheme) ScamynxRiskRedDark else ScamynxRiskRedLight,
    
    // Outline with brand accent
    outline = if (useDarkTheme) ScamynxNeutral50 else ScamynxNeutral50,
    outlineVariant = if (useDarkTheme) ScamynxNeutral40 else ScamynxNeutral80,
)
