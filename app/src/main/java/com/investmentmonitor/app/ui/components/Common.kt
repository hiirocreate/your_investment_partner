package com.investmentmonitor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.investmentmonitor.app.data.model.Importance
import com.investmentmonitor.app.data.model.NewsItem
import com.investmentmonitor.app.data.model.StockQuote
import com.investmentmonitor.app.ui.theme.PriceDown
import com.investmentmonitor.app.ui.theme.PriceFlat
import com.investmentmonitor.app.ui.theme.PriceUp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ImportanceStars(importance: Importance, modifier: Modifier = Modifier) {
    Text(
        text = "★".repeat(importance.stars) + "☆".repeat(5 - importance.stars),
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
    )
}

@Composable
fun ChangeBadge(changeValue: Double, changePercent: Double, modifier: Modifier = Modifier) {
    val color = when {
        changeValue > 0 -> PriceUp
        changeValue < 0 -> PriceDown
        else -> PriceFlat
    }
    val sign = if (changeValue > 0) "+" else ""
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$sign${"%.1f".format(changeValue)} ($sign${"%.2f".format(changePercent)}%)",
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FreshnessLabel(fetchedAtEpochMillis: Long, isStale: Boolean = false, modifier: Modifier = Modifier) {
    val text = relativeTimeLabel(fetchedAtEpochMillis)
    Text(
        text = if (isStale) "最終更新: $text (データを更新できません)" else "最終更新: $text",
        style = MaterialTheme.typography.labelSmall,
        color = if (isStale) PriceDown else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

fun relativeTimeLabel(epochMillis: Long): String {
    val diffMs = System.currentTimeMillis() - epochMillis
    val minutes = diffMs / 60_000
    return when {
        minutes < 1 -> "たった今"
        minutes < 60 -> "${minutes}分前"
        minutes < 60 * 24 -> "${minutes / 60}時間前"
        else -> "${minutes / (60 * 24)}日前"
    }
}

fun formatYen(value: Double): String =
    NumberFormat.getNumberInstance(Locale.JAPAN).format(value)

@Composable
fun StockQuoteRow(quote: StockQuote, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("¥${formatYen(quote.price)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            FreshnessLabel(fetchedAtEpochMillis = quote.asOfEpochMillis, isStale = quote.isStale)
        }
        ChangeBadge(changeValue = quote.change, changePercent = quote.changePercent)
    }
}

@Composable
fun NewsListItem(news: NewsItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryChip(text = news.category.displayName)
                Spacer(modifier = Modifier.width(6.dp))
                ImportanceStars(news.importance)
                Spacer(modifier = Modifier.width(6.dp))
                if (isNew(news.source.publishedAtEpochMillis)) {
                    Text("NEW", color = PriceUp, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(news.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Spacer(modifier = Modifier.height(2.dp))
            Text(news.companyName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(news.source.sourceName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text(relativeTimeLabel(news.source.publishedAtEpochMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (news.relatedCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("関連ニュース ${news.relatedCount}件", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private fun isNew(publishedAtEpochMillis: Long): Boolean =
    System.currentTimeMillis() - publishedAtEpochMillis < 3 * 60 * 60 * 1000 // 3h freshness window

@Composable
fun CategoryChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun DisclaimerBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "本アプリは投資情報の収集・整理・分析支援ツールであり、投資助言サービスではありません。「注目企業」等の表示も投資推奨ではありません。投資判断は必ずご自身の責任で行ってください。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ErrorBanner(message: String, lastUpdatedEpochMillis: Long?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x1AD32F2F))
            .padding(12.dp)
    ) {
        Text(message, color = PriceUp, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        if (lastUpdatedEpochMillis != null) {
            Text("最終取得: ${relativeTimeLabel(lastUpdatedEpochMillis)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
