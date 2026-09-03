package com.investmentmonitor.app

import com.investmentmonitor.app.data.model.Importance
import com.investmentmonitor.app.data.model.NewsCategory
import com.investmentmonitor.app.data.model.NewsItem
import com.investmentmonitor.app.data.model.NewsSource
import com.investmentmonitor.app.data.provider.NewsProvider
import com.investmentmonitor.app.data.repository.NewsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** NewsProvider is not exercised by these tests - dedup logic only depends on [NewsRepository.deduplicate]. */
private class UnusedNewsProvider : NewsProvider {
    override suspend fun getNewsForCompany(companyId: String, companyName: String) = Result.success(emptyList<NewsItem>())
    override suspend fun getLatestNews(limit: Int) = Result.success(emptyList<NewsItem>())
}

class NewsDeduplicationTest {

    private val repository = NewsRepository(UnusedNewsProvider())

    private fun news(id: String, title: String, publishedAtOffsetMinutes: Long, category: NewsCategory = NewsCategory.IR) = NewsItem(
        id = id,
        companyId = "7203",
        companyName = "トヨタ自動車",
        title = title,
        summary = "summary",
        source = NewsSource("テスト媒体", "https://example.com/$id", publishedAtOffsetMinutes * 60_000L, publishedAtOffsetMinutes * 60_000L),
        category = category,
        importance = Importance.NORMAL
    )

    @Test
    fun `near-identical titles from the same company within a short window are merged`() {
        val items = listOf(
            news("a", "トヨタ自動車が業務提携を発表", 0),
            news("b", "トヨタ自動車、業務提携を発表", 10),
            news("c", "トヨタ自動車が業務提携を発表した", 20)
        )

        val result = repository.deduplicate(items)

        assertEquals(1, result.size)
        assertEquals(2, result.first().relatedCount)
    }

    @Test
    fun `unrelated news items are kept separate`() {
        val items = listOf(
            news("a", "トヨタ自動車が新製品を発表", 0, NewsCategory.NEW_PRODUCT),
            news("b", "トヨタ自動車が人事異動を発表", 0, NewsCategory.PERSONNEL)
        )

        val result = repository.deduplicate(items)

        assertEquals(2, result.size)
    }

    @Test
    fun `news published far apart is not merged even with a similar title`() {
        val items = listOf(
            news("a", "トヨタ自動車が業務提携を発表", 0),
            news("b", "トヨタ自動車が業務提携を発表", 500) // > 6h later
        )

        val result = repository.deduplicate(items)

        assertTrue(result.size == 2)
    }
}
