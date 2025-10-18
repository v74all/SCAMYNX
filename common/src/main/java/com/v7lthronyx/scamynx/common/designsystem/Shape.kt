package com.v7lthronyx.scamynx.common.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * SCAMYNX shape system.
 * Defines rounded corner shapes for various UI components.
 */
val ScamynxShapes = Shapes(
    // Extra small - chips, small buttons
    extraSmall = RoundedCornerShape(6.dp),
    
    // Small - cards, small containers
    small = RoundedCornerShape(10.dp),
    
    // Medium - standard cards and containers
    medium = RoundedCornerShape(14.dp),
    
    // Large - dialogs, bottom sheets
    large = RoundedCornerShape(20.dp),
    
    // Extra large - modal bottom sheets
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Custom shapes for specific SCAMYNX components
 */
object CustomShapes {
    /** Sharp corners for technical UI elements */
    val sharp = RoundedCornerShape(0.dp)
    
    /** Fully rounded for pills and tags */
    val pill = RoundedCornerShape(percent = 50)
    
    /** Top rounded for bottom sheets */
    val bottomSheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    
    /** Scan result card with prominent corners */
    val scanCard = RoundedCornerShape(18.dp)
    
    /** Risk badge with soft corners */
    val riskBadge = RoundedCornerShape(10.dp)
}
