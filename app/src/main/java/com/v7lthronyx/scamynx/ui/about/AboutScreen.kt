package com.v7lthronyx.scamynx.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v7lthronyx.scamynx.R

private data class SocialLink(
    val labelRes: Int,
    val handle: String,
    val url: String,
)

private val SOCIAL_LINKS = listOf(
    SocialLink(R.string.about_social_instagram, "@V7LTHRONYX.core", "https://instagram.com/V7LTHRONYX.core"),
    SocialLink(R.string.about_social_telegram, "v7lthronyx", "https://t.me/v7lthronyx"),
    SocialLink(R.string.about_social_github, "V74all", "https://github.com/V74all"),
    SocialLink(R.string.about_social_youtube, "V7LTHRONYX", "https://youtube.com/@V7LTHRONYX"),
)

@Composable
fun AboutRoute(
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    AboutScreen(onBack = onBack, onOpenLink = onOpenLink)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.about_title)) },
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
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(id = R.string.about_brand_heading), style = MaterialTheme.typography.headlineSmall)
                    Text(text = stringResource(id = R.string.about_brand_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(id = R.string.about_mission_heading), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(id = R.string.about_mission_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                Text(text = stringResource(id = R.string.about_social_heading), style = MaterialTheme.typography.titleMedium)
            }
            items(SOCIAL_LINKS) { link ->
                ListItem(
                    headlineContent = { Text(text = stringResource(id = link.labelRes)) },
                    supportingContent = { Text(text = link.handle, style = MaterialTheme.typography.bodySmall) },
                    trailingContent = {
                        IconButton(onClick = { onOpenLink(link.url) }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = stringResource(id = R.string.about_social_open))
                        }
                    },
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.Start) {
                    Text(text = stringResource(id = R.string.about_license_heading), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(id = R.string.about_license_body),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
    }
}
