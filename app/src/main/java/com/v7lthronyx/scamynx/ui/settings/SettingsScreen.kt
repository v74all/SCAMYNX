package com.v7lthronyx.scamynx.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import com.v7lthronyx.scamynx.R

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onLocaleChange: (Locale) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        onDarkThemeChanged = viewModel::onDarkThemeChanged,
        onDynamicColorChanged = viewModel::onDynamicColorChanged,
        onTelemetryChanged = viewModel::onTelemetryChanged,
        onLanguageSelected = { tag ->
            viewModel.onLanguageSelected(tag)
            onLocaleChange(Locale.forLanguageTag(tag))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onTelemetryChanged: (Boolean) -> Unit,
    onLanguageSelected: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.cd_navigate_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Text(text = stringResource(id = R.string.settings_appearance_section), style = MaterialTheme.typography.titleMedium)
                SettingToggleRow(
                    title = stringResource(id = R.string.settings_dark_theme),
                    subtitle = stringResource(id = R.string.settings_dark_theme_subtitle),
                    checked = state.useDarkTheme,
                    onCheckedChange = onDarkThemeChanged,
                )
                SettingToggleRow(
                    title = stringResource(id = R.string.settings_dynamic_color),
                    subtitle = stringResource(id = R.string.settings_dynamic_color_subtitle),
                    checked = state.useDynamicColor,
                    onCheckedChange = onDynamicColorChanged,
                )
            }
            item {
                Text(text = stringResource(id = R.string.settings_privacy_section), style = MaterialTheme.typography.titleMedium)
                SettingToggleRow(
                    title = stringResource(id = R.string.settings_telemetry),
                    subtitle = stringResource(id = R.string.settings_telemetry_subtitle),
                    checked = state.telemetryOptIn,
                    onCheckedChange = onTelemetryChanged,
                )
            }
            item {
                LanguageSelector(
                    selectedTag = state.languageTag,
                    supportedTags = state.supportedLanguages,
                    onLanguageSelected = onLanguageSelected,
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = subtitle, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageSelector(
    selectedTag: String,
    supportedTags: List<String>,
    onLanguageSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(id = R.string.settings_language_section), style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            supportedTags.forEach { tag ->
                val isSelected = tag.equals(selectedTag, ignoreCase = true)
                AssistChip(
                    onClick = { onLanguageSelected(tag) },
                    label = { Text(text = languageDisplayName(tag)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }
        Text(
            text = stringResource(id = R.string.settings_language_note),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Start,
        )
    }
}

private fun languageDisplayName(tag: String): String {
    val targetLocale = Locale.forLanguageTag(tag)
    val displayLocale = currentAppLocale()
    val displayName = targetLocale.getDisplayLanguage(displayLocale)
    return displayName.replaceFirstChar { char ->
        if (char.isLowerCase()) {
            char.titlecase(displayLocale)
        } else {
            char.toString()
        }
    }
}

private fun currentAppLocale(): Locale {
    AppCompatDelegate.getApplicationLocales().let { locales ->
        locales.get(0)?.let { return it }
    }
    LocaleListCompat.getAdjustedDefault().let { locales ->
        locales.get(0)?.let { return it }
    }
    return Locale.getDefault()
}
