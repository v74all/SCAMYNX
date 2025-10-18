package com.v7lthronyx.scamynx.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.v7lthronyx.scamynx.common.designsystem.ScamynxTheme
import com.v7lthronyx.scamynx.ui.home.HomeScreen
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityChecks
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AccessibilityAuditTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun enableAccessibilityChecks() {
        AccessibilityChecks.enable()
    }

    @Test
    fun homeScreen_meetsBasicAccessibility() {
        composeRule.setContent {
            ScamynxTheme {
                HomeScreen(
                    onToggleTheme = {},
                    onSwitchToEnglish = {},
                    onSwitchToPersian = {},
                    onScanRequested = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                    onNavigateToAbout = {},
                )
            }
        }

        composeRule.waitForIdle()
    }
}
