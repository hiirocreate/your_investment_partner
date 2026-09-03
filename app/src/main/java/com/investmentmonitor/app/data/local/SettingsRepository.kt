package com.investmentmonitor.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.investmentmonitor.app.data.model.NotificationLevel
import com.investmentmonitor.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * User-facing settings (spec section 44). Stored via DataStore (Preferences) rather than
 * Room, per spec section 45 - this is simple key/value state, not relational data.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_LEVEL = stringPreferencesKey("notification_level")
        val WIFI_ONLY_UPDATES = booleanPreferencesKey("wifi_only_updates")
    }

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    val notificationLevel: Flow<NotificationLevel> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATION_LEVEL]?.let { raw -> runCatching { NotificationLevel.valueOf(raw) }.getOrNull() }
            ?: NotificationLevel.IMPORTANT_ONLY
    }

    suspend fun setNotificationLevel(level: NotificationLevel) {
        context.dataStore.edit { it[Keys.NOTIFICATION_LEVEL] = level.name }
    }

    val wifiOnlyUpdates: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.WIFI_ONLY_UPDATES] ?: false }

    suspend fun setWifiOnlyUpdates(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY_UPDATES] = enabled }
    }
}
