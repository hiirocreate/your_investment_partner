package com.investmentmonitor.app.ui.trending

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.investmentmonitor.app.data.model.TrendCategory
import com.investmentmonitor.app.ui.components.ChangeBadge
import com.investmentmonitor.app.ui.components.DisclaimerBanner
import com.investmentmonitor.app.ui.components.EmptyState
import com.investmentmonitor.app.ui.components.formatYen
import com.investmentmonitor.app.ui.util.simpleViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingScreen(
    serviceLocator: ServiceLocator,
    onCompanyClick: (String) -> Unit
) {
    val viewModel: TrendingViewModel = viewModel(factory = simpleViewModelFactory { TrendingViewModel(serviceLocator) })
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("注目企業") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                items(TrendCategory.entries) { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category.displayName) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            if (state.selectedCategory == TrendCategory.LONG_TERM_WATCH) {
                Text(
                    "「中長期指標では注目度が高い一方、現在株価が調整している企業」を表示しています。買い時を示すものではありません。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (state.selectedCategory == TrendCategory.NEW_IPO) {
                if (state.ipoCompanies.isEmpty()) {
                    EmptyState("新規上場企業のデータがありません。")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(state.ipoCompanies, key = { it.company.companyId }) { ipo ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { onCompanyClick(ipo.company.companyId) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(ipo.company.companyName, fontWeight = FontWeight.Bold)
                                    Text("市場: ${ipo.market.displayName}", style = MaterialTheme.typography.bodyMedium)
                                    Text("公募価格: ¥${formatYen(ipo.offeringPrice)}", style = MaterialTheme.typography.bodyMedium)
                                    Text("現在値: ¥${formatYen(ipo.currentPrice)} (公募比 ${"%.1f".format(ipo.currentChangePercent)}%)", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            } else {
                val list = state.filtered
                if (list.isEmpty() && !state.isLoading) {
                    EmptyState("該当する企業がありません。")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(list, key = { it.company.companyId }) { trending ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { onCompanyClick(trending.company.companyId) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(trending.company.companyName, fontWeight = FontWeight.Bold)
                                        ChangeBadge(trending.quote.change, trending.quote.changePercent)
                                    }
                                    Text("¥${formatYen(trending.quote.price)}", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "総合注目度 ${trending.scores.totalScore} / 100  (モメンタム${trending.scores.momentumScore}・成長${trending.scores.growthScore}・出来高${trending.scores.volumeScore})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    DisclaimerBanner()
                }
            }
        }
    }
}
