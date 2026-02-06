# SCAMYNX Theme Quick Reference

## Color Palette

### Brand Colors
```kotlin
// Primary cyan ramp
ScamynxPrimary10        = #001521
ScamynxPrimary20        = #002A3C
ScamynxPrimary30        = #004057
ScamynxPrimary40        = #005674
ScamynxPrimary80        = #4FD8F8
ScamynxPrimary90        = #96EDFF

// Secondary violet ramp
ScamynxSecondary10      = #190B3A
ScamynxSecondary20      = #282151
ScamynxSecondary30      = #3A346A
ScamynxSecondary40      = #504886
ScamynxSecondary80      = #D0C7FF
ScamynxSecondary90      = #E8E1FF

// Tertiary teal ramp
ScamynxTertiary10       = #002117
ScamynxTertiary20       = #00382A
ScamynxTertiary30       = #00513C
ScamynxTertiary40       = #006A51
ScamynxTertiary80       = #66DCB3
ScamynxTertiary90       = #87F8CB
```

### Risk Colors
```kotlin
ScamynxRiskRed          = #EF476F  // Critical/high risk
ScamynxRiskOrange       = #F59E0B  // Medium risk
ScamynxRiskYellow       = #FACC15  // Low risk/warning
ScamynxSignalGreen      = #34D399  // Safe/no risk
```

### Gray Scale
```kotlin
ScamynxNeutral5         = #070A11
ScamynxNeutral10        = #0F141C
ScamynxNeutral20        = #1B212C
ScamynxNeutral30        = #262D3A
ScamynxNeutral40        = #323A48
ScamynxNeutral50        = #3F4757
ScamynxNeutral60        = #4D5567
ScamynxNeutral70        = #5B6477
ScamynxNeutral80        = #CAD0DC
ScamynxNeutral90        = #E2E6F0
ScamynxNeutral95        = #F2F4FA
ScamynxNeutral99        = #FBFCFF
```

### Semantic Colors
```kotlin
ScamynxInfoBlue         = #4597F8
ScamynxWarningAmber     = #FFB547
ScamynxSuccessTeal      = #2AB3A5
```

### Surface Tokens
```kotlin
// Dark surfaces
ScamynxSurfaceDark         = #0F141C
ScamynxSurfaceDarkVariant  = #1B212C
ScamynxBackgroundDark      = #070A11

// Light surfaces
ScamynxSurfaceLight        = #FBFCFF
ScamynxSurfaceLightVariant = #F2F4FA
ScamynxBackgroundLight     = #E2E6F0
```

## Typography Scale

### Usage Guide
```kotlin
// Hero/Marketing text
MaterialTheme.typography.displayLarge    // 57sp, Bold
MaterialTheme.typography.displayMedium   // 45sp, Bold
MaterialTheme.typography.displaySmall    // 36sp, Bold

// Section Headers
MaterialTheme.typography.headlineLarge   // 32sp, SemiBold
MaterialTheme.typography.headlineMedium  // 28sp, SemiBold
MaterialTheme.typography.headlineSmall   // 24sp, SemiBold

// Subsection Headers
MaterialTheme.typography.titleLarge      // 22sp, SemiBold
MaterialTheme.typography.titleMedium     // 16sp, SemiBold
MaterialTheme.typography.titleSmall      // 14sp, Medium

// Body Content
MaterialTheme.typography.bodyLarge       // 16sp, Normal
MaterialTheme.typography.bodyMedium      // 14sp, Normal
MaterialTheme.typography.bodySmall       // 12sp, Normal

// UI Labels & Buttons
MaterialTheme.typography.labelLarge      // 14sp, Medium
MaterialTheme.typography.labelMedium     // 12sp, Medium
MaterialTheme.typography.labelSmall      // 11sp, Medium
```

## Shapes

### Standard Material Shapes
```kotlin
MaterialTheme.shapes.extraSmall  // 6dp  - chips, small buttons
MaterialTheme.shapes.small       // 10dp - compact cards
MaterialTheme.shapes.medium      // 14dp - standard cards
MaterialTheme.shapes.large       // 20dp - dialogs, sheets
MaterialTheme.shapes.extraLarge  // 32dp - modal sheets
```

### Custom SCAMYNX Shapes
```kotlin
CustomShapes.sharp          // 0dp   - technical UI
CustomShapes.pill           // 50%   - fully rounded tags
CustomShapes.bottomSheetTop // 28dp  - top corners only
CustomShapes.scanCard       // 18dp  - scan result cards
CustomShapes.riskBadge      // 10dp  - risk indicators
```

## Spacing

```kotlin
MaterialTheme.spacing.xxs   // 4.dp   - tight gutters
MaterialTheme.spacing.xs    // 8.dp   - micro-spacing, icon padding
MaterialTheme.spacing.sm    // 12.dp  - chip spacing, compact rows
MaterialTheme.spacing.md    // 16.dp  - default control spacing
MaterialTheme.spacing.lg    // 24.dp  - card padding, screen gutters
MaterialTheme.spacing.xl    // 32.dp  - section separation
MaterialTheme.spacing.xxl   // 40.dp  - hero spacers
MaterialTheme.spacing.gutter// 56.dp  - large dashboard gutters
```

## Utility Functions

### Risk Score Colors
```kotlin
// Automatically choose color based on risk score (0.0 - 1.0)
val color = riskScoreColor(0.8f)  // Returns ScamynxRiskRed

// Get human-readable label
val label = riskScoreLabel(0.8f)  // Returns "High Risk"

// Risk Score Ranges:
// 0.0 - 0.3  → Safe         (Green)
// 0.3 - 0.5  → Low Risk     (Yellow)
// 0.5 - 0.7  → Medium Risk  (Orange)
// 0.7 - 0.9  → High Risk    (Red)
// 0.9 - 1.0  → Critical     (Red)
```

### Brand Colors Access
```kotlin
// Access brand colors anywhere
ScamynxColors.riskGreen    // #34D399
ScamynxColors.riskYellow   // #FACC15
ScamynxColors.riskOrange   // #F59E0B
ScamynxColors.riskRed      // #EF476F

ScamynxColors.neonCyan     // #4FD8F8
ScamynxColors.neonPurple   // #504886

ScamynxColors.info         // #4597F8
ScamynxColors.warning      // #FFB547
ScamynxColors.success      // #2AB3A5
```

## Common Usage Examples

### Scan Result Card
```kotlin
Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = CustomShapes.scanCard,
    tonalElevation = 2.dp
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Risk Score: ${score * 100}%",
            style = MaterialTheme.typography.titleLarge,
            color = riskScoreColor(score)
        )
        Text(
            text = riskScoreLabel(score),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

### Risk Badge
```kotlin
Surface(
    color = riskScoreColor(score).copy(alpha = 0.2f),
    shape = CustomShapes.riskBadge
) {
    Text(
        text = riskScoreLabel(score),
        style = MaterialTheme.typography.labelMedium,
        color = riskScoreColor(score),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
```

### Branded Button
```kotlin
Button(
    onClick = { /* action */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = ScamynxColors.neonCyan,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ),
    shape = MaterialTheme.shapes.medium
) {
    Text(
        text = "Scan URL",
        style = MaterialTheme.typography.labelLarge
    )
}
```

### Status Message
```kotlin
Row(
    modifier = Modifier
        .background(
            color = ScamynxColors.info.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.small
        )
        .padding(12.dp)
) {
    Icon(
        imageVector = Icons.Default.Info,
        contentDescription = null,
        tint = ScamynxColors.info
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
        text = "Scanning in progress...",
        style = MaterialTheme.typography.bodyMedium,
        color = ScamynxColors.info
    )
}
```

## Theme Configuration

### Enable/Disable Dynamic Color
```kotlin
ScamynxTheme(
    useDarkTheme = isSystemInDarkTheme(),
    dynamicColor = true  // Android 12+ wallpaper-based colors
) {
    // Your app content
}
```

### Force Light/Dark Theme
```kotlin
// Force dark theme
ScamynxTheme(useDarkTheme = true) { /* ... */ }

// Force light theme
ScamynxTheme(useDarkTheme = false) { /* ... */ }

// Follow system (default)
ScamynxTheme { /* ... */ }
```
