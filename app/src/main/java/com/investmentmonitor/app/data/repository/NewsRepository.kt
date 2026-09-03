package com.investmentmonitor.app.data.repository

import com.investmentmonitor.app.data.model.NewsItem
import com.investmentmonitor.app.data.provider.NewsProvider
import kotlin.math.abs

class NewsRepository(private val newsProvider: NewsProvider) {

    suspend fun getNewsForCompany(companyId: String, companyName: String): List<NewsItem> =
        deduplicate(newsProvider.getNewsForCompany(companyId, companyName).getOrElse { emptyList() })

    suspend fun getLatestNews(limit: Int = 50): List<NewsItem> =
        deduplicate(newsProvider.getLatestNews(limit).getOrElse { emptyList() })
            .sortedByDescending { it.source.publishedAtEpochMillis }

    /**
     * Collapses near-duplicate news (spec section 10): same company + same category +
     * titles that are effectively the same, or published within a short window of each
     * other. The "primary" item is kept and its [NewsItem.relatedCount] is bumped so the
     * UI can render "関連ニュース N件".
     */
    fun deduplicate(items: List<NewsItem>): List<NewsItem> {
        val sorted = items.sortedByDescending { it.source.publishedAtEpochMillis }
        val kept = mutableListOf<NewsItem>()

        outer@ for (candidate in sorted) {
            for ((index, existing) in kept.withIndex()) {
                if (isDuplicate(existing, candidate)) {
                    kept[index] = existing.copy(relatedCount = existing.relatedCount + 1 + candidate.relatedCount)
                    continue@outer
                }
            }
            kept += candidate
        }
        return kept
    }

    private fun isDuplicate(a: NewsItem, b: NewsItem): Boolean {
        if (a.companyId != b.companyId) return false
        if (a.contentHash.isNotEmpty() && a.contentHash == b.contentHash) return true
        val sameCategory = a.category == b.category
        val closeInTime = abs(a.source.publishedAtEpochMillis - b.source.publishedAtEpochMillis) < 6 * 60 * 60 * 1000
        val similarTitle = titleSimilarity(a.title, b.title) > 0.6
        return sameCategory && closeInTime && similarTitle
    }

    /** Very small Jaccard-style similarity over character bigrams - good enough for JP titles. */
    private fun titleSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        val bigramsA = bigrams(a)
        val bigramsB = bigrams(b)
        if (bigramsA.isEmpty() || bigramsB.isEmpty()) return 0.0
        val intersection = bigramsA.intersect(bigramsB).size
        val union = bigramsA.union(bigramsB).size
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    private fun bigrams(text: String): Set<String> =
        text.windowed(size = 2, step = 1, partialWindows = false).toSet()
}
