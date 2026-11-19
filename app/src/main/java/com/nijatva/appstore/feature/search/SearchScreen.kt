package com.nijatva.appstore.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nijatva.appstore.data.SettingsRepository
import com.nijatva.appstore.data.StoreViewModel
import com.nijatva.appstore.feature.store.AppRow
import com.nijatva.appstore.ui.EmptyState
import com.nijatva.appstore.ui.PillChip
import com.nijatva.appstore.ui.SectionHeader
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: StoreViewModel,
    settings: SettingsRepository,
    onOpenApp: (String) -> Unit,
) {
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val installed by viewModel.installedVersions.collectAsStateWithLifecycle()
    val recent by settings.recentSearches.collectAsStateWithLifecycle(initialValue = emptyList())
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    val results = remember(query, catalog) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else catalog.apps.filterNot { it.self }.filter { app ->
            app.name.contains(q, true) ||
                app.category.contains(q, true) ||
                app.shortDescription.contains(q, true) ||
                app.publisher.contains(q, true) ||
                "vasudev".contains(q, true)
        }.sortedWith(compareByDescending<com.nijatva.appstore.data.CatalogApp> { it.isOfficial }.thenBy { it.name })
    }

    fun submit(text: String) {
        query = text
        keyboard?.hide()
        scope.launch { settings.addRecentSearch(text) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Search", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("App, developer or category") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { submit(query) },
                ),
            )
        }

        if (query.isBlank() && recent.isNotEmpty()) {
            item {
                SectionHeader(title = "Recent searches", trailing = {
                    TextButton(onClick = { scope.launch { settings.clearRecentSearches() } }) { Text("Clear") }
                })
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    recent.forEach { term ->
                        PillChip(label = term, selected = false, onClick = { submit(term) })
                    }
                }
            }
        }

        if (query.isNotBlank() && results.isEmpty()) {
            item {
                EmptyState(
                    title = "No matches",
                    message = "Nothing in the catalogue matches \"${query.trim()}\". Try a category such as Tools or Internet.",
                )
            }
        }

        if (results.isNotEmpty()) {
            item { SectionHeader(title = "${results.size} result${if (results.size == 1) "" else "s"}") }
            items(results, key = { it.id }) { app ->
                AppRow(
                    app = app,
                    installedVersionCode = installed[app.packageName],
                    onClick = {
                        scope.launch { settings.addRecentSearch(query) }
                        onOpenApp(app.id)
                    },
                )
            }
        }

        if (query.isBlank() && recent.isEmpty()) {
            item {
                EmptyState(
                    title = "Find an app",
                    message = "Search by app name, category or developer. Everything in the catalogue is listed on the Store tab too.",
                )
            }
        }
    }
}
