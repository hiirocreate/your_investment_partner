package com.investmentmonitor.app.data.provider

import com.investmentmonitor.app.data.model.Importance
import com.investmentmonitor.app.data.model.NewsCategory
import com.investmentmonitor.app.data.model.NewsItem
import com.investmentmonitor.app.data.model.NewsSource
import com.investmentmonitor.app.data.model.Sentiment
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Provider abstraction for news collection.
 *
 * Phase 2 real implementation should prioritize, per spec section 9:
 *   1. 企業公式サイト / IRページ 2. 適時開示(TDnet等) 3. 官公庁 4. 取引所関連 5. 決算資料
 *   6. 信頼性の高いニュースメディアのRSS/公開API  7. その他公開Web情報
 * and MUST respect robots.txt / ToS / rate limits (spec section 7).
 */
interface NewsProvider {
    suspend fun getNewsForCompany(companyId: String, companyName: String): Result<List<NewsItem>>
    suspend fun getLatestNews(limit: Int = 50): Result<List<NewsItem>>
}

class MockNewsProvider(
    private val watchedCompanyNames: () -> Map<String, String> = { emptyMap() }
) : NewsProvider {

    override suspend fun getNewsForCompany(companyId: String, companyName: String): Result<List<NewsItem>> {
        delay(180)
        return Result.success(generateNews(companyId, companyName, count = 6))
    }

    override suspend fun getLatestNews(limit: Int): Result<List<NewsItem>> {
        delay(220)
        val companies = watchedCompanyNames().ifEmpty {
            mapOf("7203" to "トヨタ自動車", "9984" to "ソフトバンクグループ", "6758" to "ソニーグループ")
        }
        val all = companies.flatMap { (id, name) -> generateNews(id, name, count = 3) }
            .sortedByDescending { it.source.publishedAtEpochMillis }
        return Result.success(all.take(limit))
    }

    private fun generateNews(companyId: String, companyName: String, count: Int): List<NewsItem> {
        val random = Random(companyId.hashCode() * 31 + count)
        val now = System.currentTimeMillis()
        return (0 until count).map { i ->
            val template = templates[random.nextInt(templates.size)]
            val minutesAgo = (i * 47L + random.nextInt(30)) * 60_000L
            val publishedAt = now - minutesAgo
            NewsItem(
                id = "$companyId-news-$i-${template.category}",
                companyId = companyId,
                companyName = companyName,
                title = "${companyName}、${template.title}",
                summary = template.summary.format(companyName),
                source = NewsSource(
                    sourceName = template.sourceName,
                    sourceUrl = "https://example.com/news/$companyId/$i",
                    publishedAtEpochMillis = publishedAt,
                    collectedAtEpochMillis = publishedAt + 60_000L
                ),
                category = template.category,
                importance = template.importance,
                sentiment = template.sentiment,
                stockImpactNote = template.impactNote,
                relatedCount = random.nextInt(0, 4)
            )
        }
    }

    private data class NewsTemplate(
        val title: String,
        val summary: String,
        val category: NewsCategory,
        val importance: Importance,
        val sentiment: Sentiment,
        val impactNote: String,
        val sourceName: String
    )

    companion object {
        private val templates = listOf(
            NewsTemplate(
                "通期業績予想を上方修正", "%sは通期の連結業績予想を上方修正したと発表した(モックデータ)。",
                NewsCategory.EARNINGS, Importance.HIGHEST, Sentiment.POSITIVE,
                "業績予想の上方修正のため株価への影響度は高いと考えられます", "適時開示(モック)"
            ),
            NewsTemplate(
                "新製品を発表", "%sは新製品ラインナップを発表した(モックデータ)。",
                NewsCategory.NEW_PRODUCT, Importance.NOTABLE, Sentiment.POSITIVE,
                "新製品の市場規模により影響度は変動します", "企業公式サイト(モック)"
            ),
            NewsTemplate(
                "業務提携を締結", "%sは他社との業務提携を発表した(モックデータ)。",
                NewsCategory.PARTNERSHIP, Importance.HIGH, Sentiment.POSITIVE,
                "提携内容次第で中期的な業績への影響が見込まれます", "IRページ(モック)"
            ),
            NewsTemplate(
                "第2四半期決算を発表", "%sは第2四半期の決算を発表した(モックデータ)。",
                NewsCategory.EARNINGS, Importance.HIGH, Sentiment.NEUTRAL,
                "決算内容の詳細確認が推奨されます", "決算短信(モック)"
            ),
            NewsTemplate(
                "組織変更を発表", "%sは役員人事および組織変更を発表した(モックデータ)。",
                NewsCategory.PERSONNEL, Importance.REFERENCE, Sentiment.NEUTRAL,
                "経営体制の変更のため中長期的な影響を注視", "プレスリリース(モック)"
            ),
            NewsTemplate(
                "自社株買いを発表", "%sは自己株式の取得を発表した(モックデータ)。",
                NewsCategory.IR, Importance.HIGH, Sentiment.POSITIVE,
                "需給改善要因として株価にポジティブな影響の可能性", "適時開示(モック)"
            ),
        )
    }
}
