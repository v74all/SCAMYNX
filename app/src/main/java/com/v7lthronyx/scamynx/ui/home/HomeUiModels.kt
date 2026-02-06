package com.v7lthronyx.scamynx.ui.home

import androidx.annotation.StringRes
import com.v7lthronyx.scamynx.R

/**
 * UI model for password security check results.
 */
data class PasswordSecurityUiModel(
    @StringRes val strengthLabelRes: Int = R.string.home_password_strength_weak,
    val strengthPercent: Float = 0f,
    val entropyBits: Float = 0f,
    val isBreached: Boolean = false,
    val breachCount: Long = 0L,
    @StringRes val warnings: List<Int> = emptyList(),
    @StringRes val recommendations: List<Int> = emptyList(),
)

/**
 * UI model for social engineering detection results.
 */
data class SocialEngineeringUiModel(
    @StringRes val riskLabelRes: Int = R.string.home_social_risk_safe,
    val riskPercent: Float = 0f,
    val indicators: List<SocialEngineeringIndicator> = emptyList(),
    val snippets: List<String> = emptyList(),
    @StringRes val recommendations: List<Int> = emptyList(),
)

data class SocialEngineeringIndicator(
    @StringRes val labelRes: Int,
    val confidence: Float = 0f,
)
