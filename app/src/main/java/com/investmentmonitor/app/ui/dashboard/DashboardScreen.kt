package com.investmentmonitor.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.ui.components.ChangeBadge
import com.investmentmonitor.app.ui.components.DisclaimerBanner
import com.investmentmonitor.app.ui.components.EmptyState
import com.investmentmonitor.app.ui.components.ErrorBanner
import com.investmentmonitor.app.ui.components.FreshnessLabel
import com.investmentmonitor.app.ui.components.NewsListItem
import com.investmentmonitor.app.ui.components.SectionHeader
import com.investmentmonitor.app.ui.components.formatYen
import com.investmentmonitor.app.ui.util.simpleViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    serviceLocator: ServiceLocator,
    onCompanyClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSeeAllNews: () -> Unit,
    onSeeAllTrending: () -> Unit
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = simpleViewModelFactory { DashboardViewModel(serviceLocator) }
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("投資情報モニター") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "企業を検索")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading && state.importantNews.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item { FreshnessLabel(fetchedAtEpochMillis = state.lastUpdatedEpochMillis, modifier = Modifier.padding(16.dp)) }

            state.errorMessage?.let { message ->
                item { ErrorBanner(message = message, lastUpdatedEpochMillis = state.lastUpdatedEpochMillis) }
            }

            item { SectionHeader("重要ニュース", "今日、注目すべき動き") }
            if (state.importantNews.isEmpty()) {
                item { EmptyState("まだニュースがありません。企業を登録すると表示されます。") }
            } else {
                items(state.importantNews, key = { it.id }) { news ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        NewsListItem(news = news, onClick = { onCompanyClick(news.companyId) })
                    }
                }
                item {
                    Text(
                        "すべてのニュースを見る →",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable(onClick = onSeeAllNews)
                    )
                }
            }

            item { SectionHeader("監視企業", "登録した企業の最新状況") }
            if (state.watchedCompanies.isEmpty()) {
                item {
                    EmptyState("まだ監視企業がありません。右上の検索から企業を登録しましょう。")
                }
            } else {
                items(state.watchedCompanies, key = { it.company.companyId }) { watched ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onCompanyClick(watched.company.companyId) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(watched.company.companyName, fontWeight = FontWeight.Bold)
                                Text(
                                    watched.company.stockCode ?: "-",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader("市場の注目企業", "客観的な指標から抽出(投資推奨ではありません)") }
            if (state.trending.isEmpty()) {
                item { EmptyState("データを取得中です。") }
            } else {
                item {
                    LazyRow(modifier = Modifier.padding(horizontal = 12.dp)) {
                        items(state.trending, key = { it.company.companyId }) { trending ->
                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .padding(4.dp)
                                    .clickable { onCompanyClick(trending.company.companyId) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(trending.company.companyName, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("¥${formatYen(trending.quote.price)}", style = MaterialTheme.typography.bodyMedium)
                                    ChangeBadge(trending.quote.change, trending.quote.changePercent, modifier = Modifier.padding(top = 4.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row {
                                        Text(
                                            "総合${trending.scores.totalScore}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        "注目企業をもっと見る →",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable(onClick = onSeeAllTrending)
                    )
                }
            }

            item { DisclaimerBanner(modifier = Modifier.padding(top = 8.dp)) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
