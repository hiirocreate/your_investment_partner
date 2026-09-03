package com.investmentmonitor.app.ui.news

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.data.model.NewsCategory
import com.investmentmonitor.app.ui.components.EmptyState
import com.investmentmonitor.app.ui.components.ErrorBanner
import com.investmentmonitor.app.ui.components.NewsListItem
import com.investmentmonitor.app.ui.util.simpleViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen(
    serviceLocator: ServiceLocator,
    onCompanyClick: (String) -> Unit
) {
    val viewModel: NewsViewModel = viewModel(factory = simpleViewModelFactory { NewsViewModel(serviceLocator) })
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("ニュース") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyRow(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text("すべて") },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                items(NewsCategory.entries) { category ->
                    FilterChip(
                        selected = state.selectedCategory == category,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category.displayName) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
            Row(modifier = Modifier.padding(horizontal = 12.dp)) {
                NewsSortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = state.sortOrder == order,
                        onClick = { viewModel.onSortOrderSelected(order) },
                        label = { Text(order.label) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))

            state.errorMessage?.let { message ->
                ErrorBanner(message = message, lastUpdatedEpochMillis = null)
            }

            val news = state.filteredNews
            if (news.isEmpty() && !state.isLoading) {
                EmptyState("該当するニュースがありません。")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(news, key = { it.id }) { item ->
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            NewsListItem(news = item, onClick = { onCompanyClick(item.companyId) })
                        }
                    }
                }
            }
        }
    }
}
