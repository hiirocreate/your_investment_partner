package com.investmentmonitor.app.ui.companydetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.data.model.ChartRange
import com.investmentmonitor.app.data.model.StockPricePoint
import com.investmentmonitor.app.ui.components.DisclaimerBanner
import com.investmentmonitor.app.ui.components.EmptyState
import com.investmentmonitor.app.ui.components.NewsListItem
import com.investmentmonitor.app.ui.components.StockQuoteRow
import com.investmentmonitor.app.ui.theme.PriceDown
import com.investmentmonitor.app.ui.theme.PriceUp
import com.investmentmonitor.app.ui.util.simpleViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDetailScreen(
    companyId: String,
    serviceLocator: ServiceLocator,
    onBack: () -> Unit
) {
    val viewModel: CompanyDetailViewModel = viewModel(
        key = "detail-$companyId",
        factory = simpleViewModelFactory { CompanyDetailViewModel(serviceLocator, companyId) }
    )
    val state by viewModel.uiState.collectAsState()
    val company = state.company

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(company?.companyName ?: "企業詳細") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "戻る") }
                },
                actions = {
                    IconButton(onClick = viewModel::toggleWatch) {
                        Icon(
                            if (state.isWatched) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (state.isWatched) "監視をやめる" else "監視する",
                            tint = if (state.isWatched) PriceUp else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (company == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (state.isLoading) Text("読み込み中...", modifier = Modifier.padding(16.dp))
                else EmptyState(state.errorMessage ?: "企業情報が見つかりません。")
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                state.quote?.let { StockQuoteRow(quote = it, modifier = Modifier.padding(16.dp)) }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChartRange.entries.forEach { range ->
                        FilterChip(
                            selected = state.selectedRange == range,
                            onClick = { viewModel.onRangeSelected(range) },
                            label = { Text(range.label) }
                        )
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    PriceLineChart(points = state.history, modifier = Modifier.fillMaxWidth().height(160.dp).padding(12.dp))
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("企業概要", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow("証券コード", company.stockCode ?: "-")
                        DetailRow("市場", company.exchange.displayName)
                        DetailRow("業種", company.industry)
                        DetailRow("時価総額", company.marketCapBillionYen?.let { "${it}億円" } ?: "-")
                        DetailRow("PER", company.per?.let { "${it}倍" } ?: "-")
                        DetailRow("PBR", company.pbr?.let { "${it}倍" } ?: "-")
                        DetailRow("ROE", company.roe?.let { "${it}%" } ?: "-")
                        DetailRow("売上高", company.revenueBillionYen?.let { "${it}億円" } ?: "-")
                        DetailRow("営業利益", company.operatingIncomeBillionYen?.let { "${it}億円" } ?: "-")
                    }
                }
            }
            if (state.relations.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("関連企業", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        state.relations.forEach { relation ->
                            Text("・${relation.toCompany.companyName}(${relation.relationType.displayName})", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Text("ニュース", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            }
            if (state.news.isEmpty()) {
                item { EmptyState("この企業に関するニュースはまだありません。") }
            } else {
                items(state.news, key = { it.id }) { news ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        NewsListItem(news = news, onClick = { })
                    }
                }
            }
            item { DisclaimerBanner(modifier = Modifier.padding(top = 8.dp)) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/** Minimal dependency-free line chart - avoids pulling in a full charting library for Phase 1. */
@Composable
private fun PriceLineChart(points: List<StockPricePoint>, modifier: Modifier = Modifier) {
    if (points.size < 2) {
        Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
            Text("チャートデータがありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val closes = points.map { it.close }
    val minValue = closes.min()
    val maxValue = closes.max()
    val range = (maxValue - minValue).let { if (it == 0.0) 1.0 else it }
    val lineColor = if (closes.last() >= closes.first()) PriceUp else PriceDown

    Canvas(modifier = modifier) {
        val stepX = size.width / (points.size - 1)
        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, point ->
            val x = stepX * index
            val normalized = ((point.close - minValue) / range).toFloat()
            val y = size.height - (normalized * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = StrokeCap.Round))

        // Baseline (first value) for a quick visual reference.
        val baselineY = size.height - (((points.first().close - minValue) / range).toFloat() * size.height)
        drawLine(
            color = lineColor.copy(alpha = 0.25f),
            start = Offset(0f, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 2f
        )
    }
}
