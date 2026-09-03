package com.investmentmonitor.app

import com.investmentmonitor.app.data.provider.MockCompanyProvider
import com.investmentmonitor.app.data.provider.MockMarketDataProvider
import com.investmentmonitor.app.data.repository.MarketRepository
import com.investmentmonitor.app.data.model.TrendCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendCategorizationTest {

    private val repository = MarketRepository(MockMarketDataProvider(), MockCompanyProvider())

    @Test
    fun `trending companies never carry a buy recommendation label`() = runTest {
        val trending = repository.getTrendingCompanies()
        // Spec section 13/29: only objective category labels are allowed, e.g. no "買い時" text anywhere.
        trending.forEach { company ->
            company.categories.forEach { category ->
                assertFalse(category.displayName.contains("買い"))
            }
        }
        assertTrue(TrendCategory.entries.none { it.displayName.contains("買い") })
    }

    @Test
    fun `every company in the universe produces a resolvable quote`() = runTest {
        val trending = repository.getTrendingCompanies()
        assertTrue(trending.size == MockCompanyProvider.sampleCompanies.size)
    }
}
