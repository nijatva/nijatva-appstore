package com.nijatva.appstore.feature.store

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nijatva.appstore.data.InstallState
import com.nijatva.appstore.data.StoreViewModel
import com.nijatva.appstore.ui.AppIconBadge
import com.nijatva.appstore.ui.CardContainer
import com.nijatva.appstore.ui.EmptyState
import com.nijatva.appstore.ui.ScreenshotPlaceholder
import com.nijatva.appstore.ui.StatStrip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    viewModel: StoreViewModel,
    appId: String?,
    onBack: () -> Unit,
    onOpenDeveloper: () -> Unit,
) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val installStates by viewModel.installStates.collectAsStateWithLifecycle()
    val installed by viewModel.installedVersions.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Read from the catalogue state, so a refresh while this screen is open still lands.
    val app = catalog.apps.firstOrNull { it.id == appId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(app?.name ?: "App") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (app != null) {
                val state = installStates[app.id] ?: InstallState.Idle
                val installedVersion = installed[app.packageName]
                val updateAvailable = installedVersion != null && app.versionCode > installedVersion
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state is InstallState.Downloading) {
                            LinearProgressIndicator(
                                progress = { state.percent / 100f },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Button(
                            onClick = {
                                when (state) {
                                    is InstallState.NeedsPermission -> context.startActivity(viewModel.permissionIntent())
                                    else -> viewModel.install(app)
                                }
                            },
                            enabled = state !is InstallState.Downloading &&
                                state !is InstallState.Installing &&
                                (app.isPublished || state is InstallState.NeedsPermission),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = when {
                                    state is InstallState.NeedsPermission -> "Allow installs, then continue"
                                    state is InstallState.Installing -> "Installing…"
                                    state is InstallState.Verifying -> "Verifying checksum…"
                                    state is InstallState.Downloading -> "Downloading ${state.percent}%"
                                    state is InstallState.Installed -> "Installed"
                                    !app.isPublished -> "Not published yet"
                                    updateAvailable -> "Update to ${app.versionName}"
                                    installedVersion != null -> "Reinstall"
                                    else -> "Install"
                                },
                            )
                        }
                        if (state is InstallState.NeedsPermission) {
                            OutlinedButton(
                                onClick = { viewModel.resumePendingInstall() },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("I allowed installs — continue") }
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (app == null) {
            EmptyState(
                title = if (catalog.apps.isEmpty()) "Loading…" else "App not found",
                message = if (catalog.apps.isEmpty()) "Reading the catalogue."
                else "This entry is no longer in the catalogue.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        val state = installStates[app.id] ?: InstallState.Idle
        val installedVersion = installed[app.packageName]

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIconBadge(app = app, size = 72.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                    Text(text = app.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    TextButton(onClick = onOpenDeveloper, contentPadding = PaddingValues(0.dp)) {
                        Text("@vasudev-work", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = app.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (app.isOfficial) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = if (app.isOfficial) {
                        "Official Nijatva build · SHA-256 checked before the installer sees it"
                    } else {
                        "Community build by ${app.publisher} · SHA-256 checked, but not a Nijatva build"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (app.isOfficial) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }

            StatStrip(
                items = listOf(
                    "Version" to app.versionName,
                    "Size" to formatSize(app.sizeBytes),
                    "Android" to "${app.minSdk}+",
                ),
            )

            if (state is InstallState.Failed) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = state.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            if (installedVersion != null && app.versionCode > installedVersion) {
                Text(
                    text = "Installed ${installedVersion} → ${app.versionCode} available",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            CardContainer {
                Text("About this app", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = app.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Screenshots", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(listOf("Screenshot 1", "Screenshot 2", "Screenshot 3")) { label ->
                        ScreenshotPlaceholder(label)
                    }
                }
            }

            CardContainer {
                Text("What's new", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = app.changelog,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            CardContainer {
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
                if (app.permissions.isEmpty()) {
                    Text(
                        text = "This app requests no permissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                } else {
                    app.permissions.forEach { permission ->
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("•", color = MaterialTheme.colorScheme.primary)
                            Text(permission, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (app.repoUrl.isNotBlank()) {
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.repoUrl)))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Source on GitHub", textAlign = TextAlign.Center)
                }
            }

            Text(
                text = "Package ${app.packageName} · released ${app.releaseDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
