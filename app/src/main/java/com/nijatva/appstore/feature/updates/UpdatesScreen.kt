package com.nijatva.appstore.feature.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nijatva.appstore.data.InstallState
import com.nijatva.appstore.data.SettingsRepository
import com.nijatva.appstore.data.StoreViewModel
import com.nijatva.appstore.feature.store.AppRow
import com.nijatva.appstore.ui.CardContainer
import com.nijatva.appstore.ui.EmptyState
import com.nijatva.appstore.ui.SettingSwitchRow
import kotlinx.coroutines.launch

@Composable
fun UpdatesScreen(
    viewModel: StoreViewModel,
    settings: SettingsRepository,
    onOpenApp: (String) -> Unit,
) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val installed by viewModel.installedVersions.collectAsStateWithLifecycle()
    val installStates by viewModel.installStates.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val autoUpdate by settings.autoUpdate.collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

    val updates = catalog.apps.filter { app ->
        val local = installed[app.packageName] ?: return@filter false
        app.versionCode > local
    }
    val installedApps = catalog.apps.filter { installed.containsKey(it.packageName) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Updates", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text = when {
                        installedApps.isEmpty() -> "No Nijatva app is installed yet"
                        updates.isEmpty() -> "${installedApps.size} installed · all up to date"
                        else -> "${installedApps.size} installed · ${updates.size} to update"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            CardContainer {
                SettingSwitchRow(
                    title = "Check for updates on launch",
                    subtitle = "The catalogue is fetched every time the store opens, over Wi-Fi or mobile data.",
                    checked = autoUpdate,
                    onCheckedChange = { enabled -> scope.launch { settings.setAutoUpdate(enabled) } },
                )
                if (updates.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.refreshCatalog() },
                        enabled = !refreshing,
                    ) { Text(if (refreshing) "Refreshing…" else "Refresh catalogue") }
                }
            }
        }

        if (updates.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing to update",
                    message = if (installedApps.isEmpty()) {
                        "Install an app from the Store tab and it will show up here when a new version is published."
                    } else {
                        "All ${installedApps.size} installed Nijatva apps are on the latest version in the catalogue."
                    },
                )
            }
        } else {
            items(updates, key = { it.id }) { app ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AppRow(
                        app = app,
                        installedVersionCode = installed[app.packageName],
                        onClick = { onOpenApp(app.id) },
                        installState = installStates[app.id] ?: InstallState.Idle,
                        onInstall = { viewModel.install(app) },
                    )
                    Text(
                        text = "Installed ${installed[app.packageName]} → ${app.versionCode}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 14.dp),
                    )
                }
            }
        }
    }
}
