package com.v7lthronyx.scamynx.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.v7lthronyx.scamynx.R
import com.v7lthronyx.scamynx.ui.home.HomeRoute
import com.v7lthronyx.scamynx.ui.history.HistoryRoute
import com.v7lthronyx.scamynx.ui.results.ResultsRoute
import com.v7lthronyx.scamynx.ui.settings.SettingsRoute
import com.v7lthronyx.scamynx.ui.about.AboutRoute
import java.util.Locale

enum class ScamynxDestination(val route: String) {
    Home("home"),
    History("history"),
    Settings("settings"),
    About("about"),
    Results("results/{$RESULTS_SESSION_ID_KEY}"),
}

private const val RESULTS_SESSION_ID_KEY = "sessionId"

private fun resultsRoute(sessionId: String): String = "results/$sessionId"

@Composable
fun ScamynxNavHost(
    navController: NavHostController,
    layoutDirection: LayoutDirection,
    onLocaleChange: (Locale) -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        NavHost(
            navController = navController,
            startDestination = ScamynxDestination.Home.route,
        ) {
            composable(route = ScamynxDestination.Home.route) {
                HomeRoute(
                    onNavigateToHistory = { navController.navigate(ScamynxDestination.History.route) },
                    onNavigateToSettings = { navController.navigate(ScamynxDestination.Settings.route) },
                    onNavigateToAbout = { navController.navigate(ScamynxDestination.About.route) },
                    onNavigateToResults = { sessionId ->
                        navController.navigate(resultsRoute(sessionId))
                    },
                )
            }
            composable(route = ScamynxDestination.History.route) {
                HistoryRoute(
                    onBack = { navController.popBackStack() },
                    onResultSelected = { sessionId -> navController.navigate(resultsRoute(sessionId)) },
                )
            }
            composable(route = ScamynxDestination.Settings.route) {
                SettingsRoute(
                    onBack = { navController.popBackStack() },
                    onLocaleChange = onLocaleChange,
                )
            }
            composable(route = ScamynxDestination.About.route) {
                val uriHandler = LocalUriHandler.current
                AboutRoute(
                    onBack = { navController.popBackStack() },
                    onOpenLink = { uri -> uriHandler.openUri(uri) },
                )
            }
            composable(
                route = ScamynxDestination.Results.route,
                arguments = listOf(navArgument(RESULTS_SESSION_ID_KEY) { type = NavType.StringType }),
            ) {
                ResultsRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
