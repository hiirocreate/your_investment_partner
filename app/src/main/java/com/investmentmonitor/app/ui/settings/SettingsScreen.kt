package com.investmentmonitor.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

            SettingsSectionTitle("API連携（自分のアカウントを登録）")
            Text(
                "株価・企業情報をより正確に取得するために、無料の外部サービスに自分自身のアカウント" +
                    "（APIキー）を登録できます。未登録でもアプリは動作しますが、サンプルデータ（Mockデータ）が" +
                    "表示されます。登録したキーはこの端末の中だけに保存され、開発者を含む他の誰にも送信されません。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            ApiCredentialField(
                title = "株価データ：J-Quants APIキー",
                description = "JPXが提供する無料サービス「J-Quants」で取得したAPIキーを入力してください。" +
                    "登録先: jpx-jquants.com （無料プランは実際の株価より約12週間遅れたデータになります）",
                currentValue = state.jquantsApiKey,
                onSave = viewModel::setJquantsApiKey
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            ApiCredentialField(
                title = "法人番号検索：国税庁 アプリケーションID",
                description = "国税庁「法人番号システムWeb-API」のアプリケーションIDを入力してください。" +
                    "登録方法: invoice-web-api@nta.go.jp 宛にメールで申請（無料）。README内に手順の詳細があります。",
                currentValue = state.houjinBangouAppId,
                onSave = viewModel::setHoujinBangouAppId
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

/**
 * Text field + Save/Clear buttons for one per-user API credential (spec section 47).
 * [currentValue] is null when nothing is saved yet, in which case the field starts empty and
 * a "未登録" status is shown; a non-null value pre-fills the field so the user can review or
 * edit what they already entered.
 */
@Composable
private fun ApiCredentialField(
    title: String,
    description: String,
    currentValue: String?,
    onSave: (String) -> Unit
) {
    var text by remember(currentValue) { mutableStateOf(currentValue.orEmpty()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(
            description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
        )
        Text(
            if (currentValue.isNullOrBlank()) "状態: 未登録（サンプルデータを表示中）" else "状態: 登録済み",
            style = MaterialTheme.typography.labelSmall,
            color = if (currentValue.isNullOrBlank())
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("キー / ID を貼り付け") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { onSave(text) }) { Text("保存") }
            if (!currentValue.isNullOrBlank()) {
                OutlinedButton(
                    onClick = { text = ""; onSave("") },
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("削除") }
            }
        }
    }
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
