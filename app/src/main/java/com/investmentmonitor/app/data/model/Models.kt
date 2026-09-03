package com.investmentmonitor.app.data.model

/**
 * Core domain models shared across the app.
 * Kept intentionally simple (plain data classes) for Phase 1 - the Provider layer
 * (see data.provider) is what will change when real APIs are wired in during Phase 2+.
 */

enum class Exchange(val displayName: String) {
    TSE_PRIME("東証プライム"),
    TSE_STANDARD("東証スタンダード"),
    TSE_GROWTH("東証グロース"),
    OTHER("その他"),
    UNKNOWN("不明")
}

enum class NewsCategory(val displayName: String) {
    IR("IR"),
    EARNINGS("決算"),
    MERGER_ACQUISITION("M&A"),
    BUSINESS_PERFORMANCE("業績"),
    NEW_PRODUCT("新商品"),
    PARTNERSHIP("提携"),
    PERSONNEL("人事"),
    SCANDAL("不祥事"),
    OTHER("その他")
}

enum class Importance(val stars: Int, val label: String) {
    HIGHEST(5, "最重要"),
    HIGH(4, "重要"),
    NOTABLE(3, "注目"),
    NORMAL(2, "普通"),
    REFERENCE(1, "参考");

    companion object {
        fun fromScore(score: Int): Importance = entries.firstOrNull { it.stars == score } ?: NORMAL
    }
}

enum class Sentiment { POSITIVE, NEUTRAL, NEGATIVE, UNKNOWN }

enum class RelationType(val displayName: String) {
    SUBSIDIARY("子会社"),
    PARENT("親会社"),
    AFFILIATE("関連会社"),
    MAJOR_SHAREHOLDER("大株主"),
    PARTNER("提携企業"),
    MAJOR_CLIENT("主要取引先"),
    GROUP("グループ企業")
}

enum class NotificationLevel(val label: String) {
    ALL("全通知"),
    IMPORTANT_ONLY("重要ニュースのみ"),
    CRITICAL_ONLY("最重要ニュースのみ")
}

enum class ThemeMode(val label: String) {
    LIGHT("ライト"),
    DARK("ダーク"),
    SYSTEM("システムに従う")
}

data class CorporateNumberCandidate(
    val corporateNumber: String,
    val officialName: String,
    val location: String,
    val stockCode: String? = null,
    val exchange: Exchange = Exchange.UNKNOWN
)

data class Company(
    val companyId: String,
    val companyName: String,
    val officialName: String,
    val corporateNumber: String? = null,
    val stockCode: String? = null,
    val exchange: Exchange = Exchange.UNKNOWN,
    val website: String? = null,
    val irUrl: String? = null,
    val parentCompanyId: String? = null,
    val industry: String = "-",
    val marketCapBillionYen: Double? = null,
    val per: Double? = null,
    val pbr: Double? = null,
    val roe: Double? = null,
    val revenueBillionYen: Double? = null,
    val operatingIncomeBillionYen: Double? = null,
    val fiveYearGrowthScore: Int = 0
)

data class CompanyRelation(
    val fromCompanyId: String,
    val toCompany: Company,
    val relationType: RelationType,
    val autoSuggested: Boolean = true
)

data class WatchedCompanySettings(
    val newsMonitoringEnabled: Boolean = true,
    val relatedCompanyMonitoringEnabled: Boolean = false,
    val priceAlertsEnabled: Boolean = false,
    val importantNewsOnly: Boolean = false
)

data class NewsSource(
    val sourceName: String,
    val sourceUrl: String,
    val publishedAtEpochMillis: Long,
    val collectedAtEpochMillis: Long
)

data class NewsItem(
    val id: String,
    val companyId: String,
    val companyName: String,
    val title: String,
    val summary: String,
    val source: NewsSource,
    val category: NewsCategory,
    val importance: Importance,
    val sentiment: Sentiment = Sentiment.UNKNOWN,
    val stockImpactNote: String = "現時点では影響度は評価されていません",
    val relatedCount: Int = 0,
    val contentHash: String = "",
    val isAiAnalyzed: Boolean = false
)

data class StockPricePoint(
    val timestampEpochMillis: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

enum class ChartRange(val label: String, val days: Int) {
    D1("1D", 1),
    W1("1W", 7),
    M1("1M", 30),
    M3("3M", 90),
    M6("6M", 180),
    Y1("1Y", 365),
    Y5("5Y", 1825)
}

data class StockQuote(
    val companyId: String,
    val price: Double,
    val previousClose: Double,
    val open: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val volume: Long,
    val asOfEpochMillis: Long,
    val isStale: Boolean = false
) {
    val change: Double get() = price - previousClose
    val changePercent: Double get() = if (previousClose == 0.0) 0.0 else (change / previousClose) * 100.0
}

enum class TrendCategory(val displayName: String) {
    SURGING("急上昇"),
    VOLUME_SPIKE("出来高急増"),
    NEWS_SPIKE("ニュース急増"),
    NEW_IPO("新規上場"),
    PLUNGING("急落"),
    LONG_TERM_WATCH("中長期注目")
}

data class MarketScores(
    val momentumScore: Int,
    val growthScore: Int,
    val valuationScore: Int,
    val newsScore: Int,
    val volumeScore: Int,
    val longTermScore: Int
) {
    val totalScore: Int get() =
        ((momentumScore + growthScore + valuationScore + newsScore + volumeScore + longTermScore) / 6.0)
            .coerceIn(0.0, 100.0)
            .toInt()
}

data class TrendingCompany(
    val company: Company,
    val quote: StockQuote,
    val scores: MarketScores,
    val categories: List<TrendCategory>,
    val newsCount24h: Int = 0
)

data class IpoCompany(
    val company: Company,
    val listingDateEpochMillis: Long,
    val market: Exchange,
    val offeringPrice: Double,
    val currentPrice: Double,
    val firstDayPrice: Double?
) {
    val firstDayChangePercent: Double?
        get() = firstDayPrice?.let { ((it - offeringPrice) / offeringPrice) * 100.0 }
    val currentChangePercent: Double
        get() = ((currentPrice - offeringPrice) / offeringPrice) * 100.0
}

/** Simple result wrapper so screens can render loading / error / stale-data states uniformly. */
sealed class DataState<out T> {
    data object Loading : DataState<Nothing>()
    data class Success<T>(val data: T, val fetchedAtEpochMillis: Long) : DataState<T>()
    data class Error(val message: String, val cachedData: Any? = null) : DataState<Nothing>()
}
