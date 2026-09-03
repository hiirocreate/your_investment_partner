package com.investmentmonitor.app.ui.watchlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.ui.components.ChangeBadge
import com.investmentmonitor.app.ui.components.EmptyState
import com.investmentmonitor.app.ui.components.formatYen
import com.investmentmonitor.app.ui.util.simpleViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    serviceLocator: ServiceLocator,
    onCompanyClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val viewModel: WatchlistViewModel = viewModel(
        factory = simpleViewModelFactory { WatchlistViewModel(serviceLocator) }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("監視企業") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "企業を追加")
            }
        }
    ) { padding ->
        if (state.rows.isEmpty() && !state.isLoading) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState("監視企業がまだ登録されていません。右下の＋から企業を追加してください。")
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(state.rows, key = { it.company.companyId }) { row ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onCompanyClick(row.company.companyId) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(row.company.companyName, fontWeight = FontWeight.Bold)
                                if (row.hasNew) {
                                    Text(
                                        " NEW",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(start = 6.dp)
                                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp)
                                    )
                                }
                            }
                            if (row.quote != null) {
                                ChangeBadge(row.quote.change, row.quote.changePercent)
                            }
                        }
                        if (row.quote != null) {
                            Text("¥${formatYen(row.quote.price)}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (row.latestNewsTitle != null) {
                            Text(
                                row.latestNewsTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
