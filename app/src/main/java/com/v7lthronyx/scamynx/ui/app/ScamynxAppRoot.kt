package com.v7lthronyx.scamynx.ui.app

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.v7lthronyx.scamynx.common.designsystem.ScamynxTheme
import com.v7lthronyx.scamynx.ui.navigation.ScamynxNavHost

@Composable
fun ScamynxAppRoot(
    viewModel: ScamynxAppViewModel,
) {
    val appState by viewModel.state.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val layoutDirection = remember(appState.currentLocale) {
        val direction = TextUtils.getLayoutDirectionFromLocale(appState.currentLocale)
        if (direction == View.LAYOUT_DIRECTION_RTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    }

    val localeList = remember(appState.currentLocale) { LocaleListCompat.create(appState.currentLocale) }

    SideEffect {
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    val baseContext = LocalContext.current
    val localizedContext = remember(appState.currentLocale, baseContext) {
        val configuration = Configuration(baseContext.resources.configuration).apply {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocales(LocaleList(appState.currentLocale))
            } else {
                setLocale(appState.currentLocale)
            }
            setLayoutDirection(appState.currentLocale)
        }
        val localizedResources = baseContext.createConfigurationContext(configuration).resources
        object : ContextWrapper(baseContext) {
            override fun getResources(): Resources = localizedResources
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalContext provides localizedContext) {
        ScamynxTheme(
            useDarkTheme = appState.isDarkTheme,
            dynamicColor = appState.dynamicColor,
        ) {
            ScamynxNavHost(
                navController = navController,
                layoutDirection = layoutDirection,
                onLocaleChange = viewModel::setLocale,
            )
        }
    }
}
