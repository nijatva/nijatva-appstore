package com.nijatva.appstore.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nijatva.appstore.core.theme.Accent
import com.nijatva.appstore.core.theme.ThemeMode
import com.nijatva.appstore.core.theme.ThemeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nijatva_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val AUTO_UPDATE = booleanPreferencesKey("auto_update")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
    }

    val themeState: Flow<ThemeState> = context.dataStore.data.map { prefs ->
        ThemeState(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            accent = prefs[Keys.ACCENT]?.let { runCatching { Accent.valueOf(it) }.getOrNull() }
                ?: Accent.GREEN,
        )
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS] ?: true }
    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { it[Keys.WIFI_ONLY] ?: false }
    val autoUpdate: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_UPDATE] ?: true }
    val recentSearches: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECENT_SEARCHES]?.split("\u001F")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAccent(accent: Accent) {
        context.dataStore.edit { it[Keys.ACCENT] = accent.name }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    suspend fun setAutoUpdate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_UPDATE] = enabled }
    }

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_SEARCHES]?.split("\u001F")?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, true) }).take(8)
            prefs[Keys.RECENT_SEARCHES] = updated.joinToString("\u001F")
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
    }
}
