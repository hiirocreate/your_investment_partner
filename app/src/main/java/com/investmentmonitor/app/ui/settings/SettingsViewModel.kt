package com.investmentmonitor.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.data.model.NotificationLevel
import com.investmentmonitor.app.data.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val notificationLevel: NotificationLevel = NotificationLevel.IMPORTANT_ONLY,
    val wifiOnlyUpdates: Boolean = false,
    // Per-user API credentials (spec section 47). Null = not configured yet -> app falls back
    // to Mock data automatically (see CompositeMarketDataProvider / CompositeCorporateNumberProvider).
    val jquantsApiKey: String? = null,
    val houjinBangouAppId: String? = null
)

class SettingsViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val settings = serviceLocator.settingsRepository

    // Combined in two steps (rather than one 6-flow combine) so each step stays within the
    // typed combine() overloads kotlinx.coroutines provides - avoids relying on the untyped
    // vararg Array<T> overload for a mix of different flow types.
    private val baseSettings = combine(
        settings.themeMode,
        settings.notificationsEnabled,
        settings.notificationLevel,
        settings.wifiOnlyUpdates
    ) { theme, notificationsEnabled, level, wifiOnly ->
        SettingsUiState(theme, notificationsEnabled, level, wifiOnly)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        baseSettings,
        settings.jquantsApiKey,
        settings.houjinBangouAppId
    ) { base, jquantsApiKey, houjinBangouAppId ->
        base.copy(jquantsApiKey = jquantsApiKey, houjinBangouAppId = houjinBangouAppId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setNotificationsEnabled(enabled) }
    }

    fun setNotificationLevel(level: NotificationLevel) {
        viewModelScope.launch { settings.setNotificationLevel(level) }
    }

    fun setWifiOnlyUpdates(enabled: Boolean) {
        viewModelScope.launch { settings.setWifiOnlyUpdates(enabled) }
    }

    /** Saves (or, if [value] is blank, clears) the current user's own J-Quants API key. */
    fun setJquantsApiKey(value: String) {
        viewModelScope.launch { settings.setJquantsApiKey(value) }
    }

    /** Saves (or, if [value] is blank, clears) the current user's own 法人番号 アプリケーションID. */
    fun setHoujinBangouAppId(value: String) {
        viewModelScope.launch { settings.setHoujinBangouAppId(value) }
    }
}
