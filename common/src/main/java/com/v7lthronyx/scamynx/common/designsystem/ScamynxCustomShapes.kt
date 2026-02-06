package com.v7lthronyx.scamynx.common.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Custom shape tokens for the SCAMYNX design system.
 * Provides consistent shape definitions across the app.
 */
object ScamynxCustomShapes {
    val heroCard: Shape = RoundedCornerShape(24.dp)
    val card: Shape = RoundedCornerShape(16.dp)
    val cardLarge: Shape = RoundedCornerShape(20.dp)
    val pill: Shape = RoundedCornerShape(50)
    val button: Shape = RoundedCornerShape(12.dp)
    val buttonLarge: Shape = RoundedCornerShape(16.dp)
    val chip: Shape = RoundedCornerShape(8.dp)
    val textField: Shape = RoundedCornerShape(12.dp)
    val dialog: Shape = RoundedCornerShape(28.dp)
    val bottomSheet: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}
