package com.nijatva.appstore.feature.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nijatva.appstore.data.CatalogApp
import com.nijatva.appstore.data.CatalogSource
import com.nijatva.appstore.data.InstallState
import com.nijatva.appstore.data.StoreViewModel
import com.nijatva.appstore.ui.AppIconBadge
import com.nijatva.appstore.ui.CardContainer
import com.nijatva.appstore.ui.EmptyState
import com.nijatva.appstore.ui.InstallChip
import com.nijatva.appstore.ui.LoadingList
import com.nijatva.appstore.ui.PillChip
import com.nijatva.appstore.ui.SectionHeader

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreScreen(
    viewModel: StoreViewModel,
    onOpenApp: (String) -> Unit,
    onOpenDeveloper: () -> Unit,
) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val installed by viewModel.installedVersions.collectAsStateWithLifecycle()
    val installStates by viewModel.installStates.collectAsStateWithLifecycle()
    val source by viewModel.source.collectAsStateWithLifecycle()
    val fetchedAt by viewModel.fetchedAt.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val refreshError by viewModel.refreshError.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // The store's own row lives in the catalogue for updates only; the list shows apps.
    val listedApps = catalog.apps.filterNot { it.self }
    val categories = listedApps.map { it.category }.distinct().sorted()

    // A refresh can remove the category the user was filtering by.
    LaunchedEffect(categories) {
        if (selectedCategory != null && selectedCategory !in categories) selectedCategory = null
    }

    val visibleApps = listedApps.filter { selectedCategory == null || it.category == selectedCategory }
    val officialApps = visibleApps.filter { it.isOfficial }
    val communityApps = visibleApps.filterNot { it.isOfficial }
    val installedCount = listedApps.count { installed.containsKey(it.packageName) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Nijatva Appstore",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (listedApps.isEmpty()) "Loading the catalogue…"
                    else "${listedApps.size} apps · $installedCount installed · no accounts, no ads",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            CatalogueStatusRow(
                source = source,
                fetchedAt = fetchedAt,
                refreshing = refreshing,
                error = refreshError,
                onRefresh = { viewModel.refreshCatalog() },
            )
        }

        if (listedApps.isEmpty()) {
            item { LoadingList() }
            return@LazyColumn
        }

        val featured = catalog.featured.filter { it.isOfficial }
        if (featured.isNotEmpty()) {
            item { SectionHeader(title = "Featured") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(featured, key = { it.id }) { app ->
                        FeaturedCard(
                            app = app,
                            installedVersionCode = installed[app.packageName],
                            onClick = { onOpenApp(app.id) },
                        )
                    }
                }
            }
        }

        if (categories.size > 1) {
            item { SectionHeader(title = "Categories") }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PillChip(label = "All", selected = selectedCategory == null, onClick = { selectedCategory = null })
                    categories.forEach { category ->
                        PillChip(
                            label = category,
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = if (selectedCategory == category) null else category },
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = if (selectedCategory == null) "Nijatva apps" else selectedCategory!!,
                trailing = { Text("${officialApps.size}", style = MaterialTheme.typography.labelMedium) },
            )
        }

        if (officialApps.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing in this category",
                    message = "No Nijatva app is listed under ${selectedCategory ?: "this category"} right now.",
                )
            }
        } else {
            items(officialApps, key = { it.id }) { app ->
                AppRow(
                    app = app,
                    installedVersionCode = installed[app.packageName],
                    onClick = { onOpenApp(app.id) },
                    installState = installStates[app.id] ?: InstallState.Idle,
                    onInstall = { viewModel.install(app) },
                )
            }
        }

        if (communityApps.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Community apps",
                    trailing = { Text("${communityApps.size}", style = MaterialTheme.typography.labelMedium) },
                )
            }
            item {
                Text(
                    text = "Published by other developers. The APK's SHA-256 is checked, but it is not a Nijatva build.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(communityApps, key = { it.id }) { app ->
                AppRow(
                    app = app,
                    installedVersionCode = installed[app.packageName],
                    onClick = { onOpenApp(app.id) },
                    installState = installStates[app.id] ?: InstallState.Idle,
                    onInstall = { viewModel.install(app) },
                )
            }
        }

        item {
            CardContainer {
                Text(
                    text = "Built by Vasudev Dangaya",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Every APK is checked against a SHA-256 before the installer sees it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenDeveloper, modifier = Modifier.padding(top = 4.dp)) {
                    Text("About the developer")
                }
            }
        }
    }
}

@Composable
private fun CatalogueStatusRow(
    source: CatalogSource,
    fetchedAt: Long?,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
) {
    val label = when (source) {
        CatalogSource.REMOTE -> "Catalogue from GitHub"
        CatalogSource.CACHED -> "Catalogue saved on this device"
        CatalogSource.BUNDLED -> "Catalogue bundled with the app"
    }
    val stamp = fetchedAt?.let {
        android.text.format.DateFormat.format("d MMM, HH:mm", it).toString()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = when {
                    error != null -> error
                    stamp != null -> "Updated $stamp"
                    else -> "Not fetched yet"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onRefresh, enabled = !refreshing) {
            Text(if (refreshing) "Refreshing…" else "Refresh")
        }
    }
}

@Composable
private fun FeaturedCard(app: CatalogApp, installedVersionCode: Int?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(0.8f),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppIconBadge(app = app, size = 48.dp)
            Text(text = app.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = app.shortDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Text(
                text = if (installedVersionCode != null) "Installed · v${app.versionName}"
                else "${app.category} · v${app.versionName}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun AppRow(
    app: CatalogApp,
    installedVersionCode: Int?,
    onClick: () -> Unit,
    installState: InstallState = InstallState.Idle,
    onInstall: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconBadge(app = app)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = app.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = app.shortDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Text(
                    text = buildString {
                        if (!app.isOfficial) {
                            append(app.publisher)
                            append(" · ")
                        }
                        append(app.category)
                        append(" · ")
                        append(formatSize(app.sizeBytes))
                        when {
                            installedVersionCode != null && app.versionCode > installedVersionCode -> append(" · update")
                            installedVersionCode != null -> append(" · installed")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onInstall != null) {
                RowActionChip(
                    app = app,
                    installedVersionCode = installedVersionCode,
                    state = installState,
                    onInstall = onInstall,
                )
            }
        }
    }
}

@Composable
private fun RowActionChip(
    app: CatalogApp,
    installedVersionCode: Int?,
    state: InstallState,
    onInstall: () -> Unit,
) {
    when {
        state is InstallState.Downloading ->
            InstallChip(label = "${state.percent}%", enabled = false, emphasised = true, onClick = {})
        state is InstallState.Verifying || state is InstallState.Installing ->
            InstallChip(label = "…", enabled = false, emphasised = true, onClick = {})
        !app.isPublished ->
            InstallChip(label = "Soon", enabled = false, emphasised = false, onClick = {})
        installedVersionCode != null && app.versionCode > installedVersionCode ->
            InstallChip(label = "Update", enabled = true, emphasised = true, onClick = onInstall)
        installedVersionCode != null ->
            InstallChip(label = "Installed", enabled = false, emphasised = false, onClick = {})
        else ->
            InstallChip(label = "Install", enabled = true, emphasised = true, onClick = onInstall)
    }
}

fun formatSize(bytes: Long): String = when {
    bytes <= 0L -> "—"
    bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
