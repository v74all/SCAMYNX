package com.v7lthronyx.scamynx.domain.model

import kotlinx.datetime.Instant

enum class PasswordStrength {
    VERY_WEAK,
    WEAK,
    FAIR,
    STRONG,
    EXCELLENT,
}

enum class PasswordPatternWarning {
    COMMON_WORD,
    SEQUENTIAL_CHARS,
    REPEATED_CHARS,
    KEYBOARD_PATTERN,
    YEAR_PATTERN,
}

enum class PasswordRecommendation {
    INCREASE_LENGTH,
    ADD_VARIETY,
    AVOID_COMMON_PATTERNS,
    USE_PASSWORD_MANAGER,
    ENABLE_2FA,
    RESET_BREACHED_PASSWORDS,
}

data class PasswordSecurityReport(
    val passwordLength: Int,
    val entropyBits: Double,
    val strengthScore: Double,
    val strength: PasswordStrength,
    val breachCount: Int,
    val warnings: List<PasswordPatternWarning>,
    val recommendations: List<PasswordRecommendation>,
    val timestamp: Instant,
)
