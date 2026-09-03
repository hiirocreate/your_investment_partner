package com.investmentmonitor.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.investmentmonitor.app.BuildConfig
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.data.model.NotificationLevel
import com.investmentmonitor.app.data.model.ThemeMode
import com.investmentmonitor.app.ui.util.simpleViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(serviceLocator: ServiceLocator) {
    val viewModel: SettingsViewModel = viewModel(factory = simpleViewModelFactory { SettingsViewModel(serviceLocator) })
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("設定") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SettingsSectionTitle("通知")
            SettingsSwitchRow(
                label = "通知を受け取る",
                checked = state.notificationsEnabled,
                onCheckedChange = viewModel::setNotificationsEnabled
            )
            if (state.notificationsEnabled) {
                Text("通知レベル", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Row {
                    NotificationLevel.entries.forEach { level ->
                        FilterChip(
                            selected = state.notificationLevel == level,
                            onClick = { viewModel.setNotificationLevel(level) },
                            label = { Text(level.label) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SettingsSectionTitle("データ更新")
            SettingsSwitchRow(
                label = "Wi-Fi接続時のみ更新",
                checked = state.wifiOnlyUpdates,
                onCheckedChange = viewModel::setWifiOnlyUpdates
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SettingsSectionTitle("表示")
            Row {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = state.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.label) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SettingsSectionTitle("このアプリについて")
            Text(
                "投資情報モニター v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "本アプリは投資助言サービスではありません。表示される情報は投資情報の収集・整理・分析を支援するものであり、投資判断は必ずご自身の責任で行ってください。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
