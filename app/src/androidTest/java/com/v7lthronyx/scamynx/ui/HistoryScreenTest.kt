package com.v7lthronyx.scamynx.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.common.designsystem.ScamynxTheme
import com.v7lthronyx.scamynx.domain.model.RiskCategory
import com.v7lthronyx.scamynx.domain.model.ScanTargetType
import com.v7lthronyx.scamynx.ui.history.HistoryItemUi
import com.v7lthronyx.scamynx.ui.history.HistoryScreen
import com.v7lthronyx.scamynx.ui.history.HistoryUiState
import kotlinx.datetime.Clock
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun historyScreen_displaysItems() {
        val item = HistoryItemUi(
            sessionId = "1",
            targetLabel = "https://example.com",
            targetType = ScanTargetType.URL,
            riskScore = 2.5,
            riskCategory = RiskCategory.MEDIUM,
            createdAt = Clock.System.now(),
        )
        val mediumLabel = composeRule.activity.getString(R.string.risk_category_medium)
        composeRule.setContent {
            ScamynxTheme {
                HistoryScreen(
                    state = HistoryUiState(isLoading = false, items = listOf(item)),
                    onBack = {},
                    onRetry = {},
                    onResultSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("https://example.com").assertIsDisplayed()
        composeRule.onNodeWithText(mediumLabel).assertIsDisplayed()
    }
}
