package com.nijatva.appstore.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nijatva.appstore.core.theme.ThemeState
import com.nijatva.appstore.data.SettingsRepository
import com.nijatva.appstore.ui.CardContainer
import com.nijatva.appstore.ui.SettingSwitchRow
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    themeState: ThemeState,
    onAppearance: () -> Unit,
    onDeveloper: () -> Unit,
    onAbout: () -> Unit,
) {
    val notifications by settings.notificationsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val wifiOnly by settings.wifiOnly.collectAsStateWithLifecycle(initialValue = false)
    val autoUpdate by settings.autoUpdate.collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

        CardContainer {
            Text("Appearance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            NavigationRow(
                title = "Theme and accent",
                subtitle = "${themeState.themeMode.label} · ${themeState.accent.label}",
                onClick = onAppearance,
            )
        }

        CardContainer {
            Text("Downloads and updates", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SettingSwitchRow(
                title = "Auto-update",
                subtitle = "Look for newer catalogue versions when the app opens.",
                checked = autoUpdate,
                onCheckedChange = { scope.launch { settings.setAutoUpdate(it) } },
            )
            SettingSwitchRow(
                title = "Download over Wi-Fi only",
                subtitle = "Hold APK downloads until a Wi-Fi network is available.",
                checked = wifiOnly,
                onCheckedChange = { scope.launch { settings.setWifiOnly(it) } },
            )
            SettingSwitchRow(
                title = "Notifications",
                subtitle = "Update and featured-app alerts.",
                checked = notifications,
                onCheckedChange = { scope.launch { settings.setNotifications(it) } },
            )
        }

        CardContainer {
            Text("About", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            NavigationRow(
                title = "About the developer",
                subtitle = "Vasudev Dangaya · @vasudev-work",
                onClick = onDeveloper,
            )
            NavigationRow(
                title = "About Nijatva Appstore",
                subtitle = "Version 0.1.0",
                onClick = onAbout,
            )
        }
    }
}

@Composable
private fun NavigationRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
