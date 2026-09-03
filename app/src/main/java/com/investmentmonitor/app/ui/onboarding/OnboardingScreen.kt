package com.investmentmonitor.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.investmentmonitor.app.ServiceLocator
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    serviceLocator: ServiceLocator,
    onFinished: () -> Unit
) {
    val scope = rememberCoroutineScope()

    fun complete() {
        scope.launch {
            serviceLocator.settingsRepository.setOnboardingDone(true)
            onFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "投資情報を効率的にチェック",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "監視したい企業を登録すると、新しいニュースをできるだけ早く通知します。",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "通知を受け取ることで、新しいニュースをすぐ確認できます。通知は設定からいつでも変更できます。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "本アプリは投資助言サービスではありません。投資判断はご自身の責任で行ってください。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { complete() }, modifier = Modifier) {
            Text("企業を登録してはじめる")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { complete() }) {
            Text("スキップ")
        }
    }
}
