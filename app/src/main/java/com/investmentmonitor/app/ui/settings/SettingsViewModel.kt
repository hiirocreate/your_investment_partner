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
    val wifiOnlyUpdates: Boolean = false
)

class SettingsViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val settings = serviceLocator.settingsRepository

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.themeMode,
        settings.notificationsEnabled,
        settings.notificationLevel,
        settings.wifiOnlyUpdates
    ) { theme, notificationsEnabled, level, wifiOnly ->
        SettingsUiState(theme, notificationsEnabled, level, wifiOnly)
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
}
