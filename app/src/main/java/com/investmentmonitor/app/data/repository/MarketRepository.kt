package com.investmentmonitor.app.data.repository

import com.investmentmonitor.app.data.model.ChartRange
import com.investmentmonitor.app.data.model.Company
import com.investmentmonitor.app.data.model.IpoCompany
import com.investmentmonitor.app.data.model.MarketScores
import com.investmentmonitor.app.data.model.StockPricePoint
import com.investmentmonitor.app.data.model.StockQuote
import com.investmentmonitor.app.data.model.TrendCategory
import com.investmentmonitor.app.data.model.TrendingCompany
import com.investmentmonitor.app.data.provider.CompanyProvider
import com.investmentmonitor.app.data.provider.MarketDataProvider
import com.investmentmonitor.app.data.provider.MockCompanyProvider
import kotlin.math.abs

class MarketRepository(
    private val marketDataProvider: MarketDataProvider,
    private val companyProvider: CompanyProvider
) {
    suspend fun getQuote(companyId: String): Result<StockQuote> = marketDataProvider.getQuote(companyId)

    suspend fun getHistory(companyId: String, range: ChartRange): Result<List<StockPricePoint>> =
        marketDataProvider.getHistory(companyId, range)

    /**
     * Builds the "注目企業" (trending) universe for Phase 1 out of the mock company master.
     * Scoring is objective and explicitly NOT a buy/sell recommendation (spec sections 13, 27-29):
     * we surface momentum / growth / valuation / news / volume / long-term signals only.
     */
    suspend fun getTrendingCompanies(): List<TrendingCompany> {
        val universe = MockCompanyProvider.sampleCompanies
        return universe.mapNotNull { company ->
            val quote = marketDataProvider.getQuote(company.companyId).getOrNull() ?: return@mapNotNull null
            val scores = computeScores(company, quote)
            val categories = categorize(company, quote, scores)
            TrendingCompany(
                company = company,
                quote = quote,
                scores = scores,
                categories = categories,
                newsCount24h = (0..6).random()
            )
        }
    }

    suspend fun getIpoCompanies(): List<IpoCompany> {
        val ipo = MockCompanyProvider.sampleCompanies.firstOrNull { it.companyId == "IPO001" } ?: return emptyList()
        val quote = marketDataProvider.getQuote(ipo.companyId).getOrNull() ?: return emptyList()
        return listOf(
            IpoCompany(
                company = ipo,
                listingDateEpochMillis = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000,
                market = ipo.exchange,
                offeringPrice = 2500.0,
                currentPrice = quote.price,
                firstDayPrice = 3400.0
            )
        )
    }

    private fun computeScores(company: Company, quote: StockQuote): MarketScores {
        val momentum = ((quote.changePercent + 5) / 10 * 100).coerceIn(0.0, 100.0).toInt()
        val growth = company.fiveYearGrowthScore.coerceIn(0, 100)
        val per = company.per
        val valuation = when {
            per == null -> 50
            per <= 0 -> 20
            per < 10 -> 85
            per < 20 -> 65
            per < 35 -> 45
            else -> 25
        }
        val news = (30 + (company.companyId.hashCode().mod(50))).coerceIn(0, 100)
        val volumeScore = ((quote.volume / 20_000_000.0) * 100).coerceIn(0.0, 100.0).toInt()
        val longTerm = ((growth * 0.6) + (valuation * 0.4)).toInt().coerceIn(0, 100)
        return MarketScores(
            momentumScore = momentum,
            growthScore = growth,
            valuationScore = valuation,
            newsScore = news,
            volumeScore = volumeScore,
            longTermScore = longTerm
        )
    }

    private fun categorize(company: Company, quote: StockQuote, scores: MarketScores): List<TrendCategory> {
        val categories = mutableListOf<TrendCategory>()
        if (quote.changePercent >= 2.0) categories += TrendCategory.SURGING
        if (quote.changePercent <= -2.0) categories += TrendCategory.PLUNGING
        if (scores.volumeScore >= 60) categories += TrendCategory.VOLUME_SPIKE
        if (scores.newsScore >= 70) categories += TrendCategory.NEWS_SPIKE
        if (company.companyId == "IPO001") categories += TrendCategory.NEW_IPO
        // The important "下がっているが中長期指標では注目" bucket (spec section 29):
        if (quote.changePercent < 0 && scores.longTermScore >= 60) categories += TrendCategory.LONG_TERM_WATCH
        if (categories.isEmpty() && abs(quote.changePercent) < 2.0 && scores.longTermScore >= 55) {
            categories += TrendCategory.LONG_TERM_WATCH
        }
        return categories
    }
}
