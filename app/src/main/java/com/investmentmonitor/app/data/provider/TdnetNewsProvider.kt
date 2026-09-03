package com.investmentmonitor.app.data.provider

import com.investmentmonitor.app.data.model.Importance
import com.investmentmonitor.app.data.model.NewsCategory
import com.investmentmonitor.app.data.model.NewsItem
import com.investmentmonitor.app.data.model.NewsSource
import com.investmentmonitor.app.data.model.Sentiment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Real (Phase 2) [NewsProvider] backed by TDnet (適時開示情報) via the unofficial, keyless
 * public mirror at https://webapi.yanoshin.jp/tdnet/ ("やのしんTDnet WEB-API").
 *
 * Why this source: spec section 9 ranks 適時開示 (timely disclosure) near the top of the
 * priority list, and this endpoint requires no signup/API key, so it can ship immediately
 * without blocking on the user creating any accounts. It IS an unofficial community mirror
 * of JPX's TDnet data (JPX's own TDnet API is a paid product), not an official JPX service -
 * see README for the caveat and a documented fallback if it ever becomes unavailable.
 *
 * Uses only java.net.HttpURLConnection + org.json (both built into Android) to avoid pulling
 * in a networking library, per spec section 4 (keep dependencies minimal).
 */
class TdnetNewsProvider : NewsProvider {

    override suspend fun getNewsForCompany(companyId: String, companyName: String): Result<List<NewsItem>> =
        fetch(condition = companyId, companyNameFallback = companyName)

    override suspend fun getLatestNews(limit: Int): Result<List<NewsItem>> =
        fetch(condition = "recent", limit = limit)

    private suspend fun fetch(
        condition: String,
        limit: Int = 50,
        companyNameFallback: String? = null
    ): Result<List<NewsItem>> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$BASE_URL/$condition.json?limit=$limit")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "InvestmentMonitorApp/0.1 (+https://github.com/hiirocreate/your_investment_partner)")
            }
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                return@withContext Result.failure(Exception("TDnet API returned HTTP $statusCode"))
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val items = root.optJSONArray("items") ?: return@withContext Result.success(emptyList())

            val parsed = (0 until items.length()).mapNotNull { index ->
                val tdnet = items.optJSONObject(index)?.optJSONObject("Tdnet") ?: return@mapNotNull null
                toNewsItem(tdnet, companyNameFallback)
            }
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun toNewsItem(tdnet: JSONObject, companyNameFallback: String?): NewsItem? {
        val title = tdnet.optString("title").ifBlank { return null }
        // TDnet company codes are 5 digits (a trailing check digit); our app uses the
        // standard 4-digit stock code everywhere else, so normalize here.
        val rawCode = tdnet.optString("company_code")
        val stockCode = if (rawCode.length >= 4) rawCode.take(4) else rawCode
        val companyName = tdnet.optString("company_name").ifBlank { companyNameFallback ?: stockCode }
        val documentUrl = tdnet.optString("document_url")
        val publishedAt = parsePubDate(tdnet.optString("pubdate"))
        val category = categorize(title)
        val importance = estimateImportance(title, category)
        val id = tdnet.optString("id").ifBlank { "$stockCode-${title.hashCode()}-$publishedAt" }

        return NewsItem(
            id = "tdnet-$id",
            companyId = stockCode,
            companyName = companyName,
            title = title,
            summary = "TDnet(適時開示情報)より取得。詳細は原文(PDF)をご確認ください。",
            source = NewsSource(
                sourceName = "TDnet(適時開示情報)",
                sourceUrl = documentUrl,
                publishedAtEpochMillis = publishedAt,
                collectedAtEpochMillis = System.currentTimeMillis()
            ),
            category = category,
            importance = importance,
            sentiment = Sentiment.UNKNOWN,
            stockImpactNote = "適時開示に基づく情報です。株価への影響度はご自身でご判断ください。",
            contentHash = documentUrl.ifBlank { title }
        )
    }

    private fun parsePubDate(raw: String): Long = try {
        JST_FORMAT.get()?.parse(raw)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    private fun categorize(title: String): NewsCategory = when {
        listOf("決算短信", "四半期決算", "決算説明", "決算補足").any { title.contains(it) } -> NewsCategory.EARNINGS
        listOf("業績予想", "上方修正", "下方修正", "業績の修正").any { title.contains(it) } -> NewsCategory.BUSINESS_PERFORMANCE
        listOf("合併", "株式交換", "株式移転", "買収", "子会社化", "会社分割").any { title.contains(it) } -> NewsCategory.MERGER_ACQUISITION
        listOf("業務提携", "資本提携", "業務・資本提携").any { title.contains(it) } -> NewsCategory.PARTNERSHIP
        listOf("新製品", "新サービス", "新商品").any { title.contains(it) } -> NewsCategory.NEW_PRODUCT
        listOf("役員", "人事", "代表取締役の異動").any { title.contains(it) } -> NewsCategory.PERSONNEL
        listOf("行政処分", "不祥事", "訴訟", "調査委員会", "特別損失", "上場廃止").any { title.contains(it) } -> NewsCategory.SCANDAL
        listOf("自己株式", "配当", "株式分割", "増資", "第三者割当", "新株予約権").any { title.contains(it) } -> NewsCategory.IR
        else -> NewsCategory.OTHER
    }

    private fun estimateImportance(title: String, category: NewsCategory): Importance = when {
        listOf("上方修正", "下方修正", "業績予想の修正", "配当予想の修正", "民事再生", "破産", "上場廃止").any { title.contains(it) } -> Importance.HIGHEST
        category == NewsCategory.EARNINGS || category == NewsCategory.MERGER_ACQUISITION || category == NewsCategory.SCANDAL -> Importance.HIGH
        category == NewsCategory.PARTNERSHIP || category == NewsCategory.IR -> Importance.NOTABLE
        else -> Importance.NORMAL
    }

    companion object {
        private const val BASE_URL = "https://webapi.yanoshin.jp/webapi/tdnet/list"

        // SimpleDateFormat is not thread-safe; ThreadLocal keeps this provider safe to call
        // concurrently (e.g. Dashboard + Watchlist refreshing at the same time).
        private val JST_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.JAPAN).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Tokyo")
                }
        }
    }
}
