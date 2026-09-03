package com.investmentmonitor.app.data.provider

import com.investmentmonitor.app.data.model.ChartRange
import com.investmentmonitor.app.data.model.StockPricePoint
import com.investmentmonitor.app.data.model.StockQuote
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * Provider abstraction for stock price data.
 *
 *   MarketDataProvider
 *   ├── (Phase 2) a real, ToS-compliant market data API implementation
 *   └── MockMarketDataProvider (Phase 1, ships today)
 *
 * Swapping the real implementation in later must not require touching any ViewModel or
 * Composable - they only depend on this interface.
 */
interface MarketDataProvider {
    suspend fun getQuote(companyId: String): Result<StockQuote>
    suspend fun getHistory(companyId: String, range: ChartRange): Result<List<StockPricePoint>>
}

class MockMarketDataProvider : MarketDataProvider {

    override suspend fun getQuote(companyId: String): Result<StockQuote> {
        delay(150)
        val base = basePrice(companyId)
        val random = Random(companyId.hashCode() + (System.currentTimeMillis() / 60_000))
        val prevClose = base
        val drift = base * (random.nextDouble(-0.03, 0.035))
        val price = (base + drift).coerceAtLeast(1.0)
        val high = maxOf(price, prevClose) * (1 + random.nextDouble(0.0, 0.01))
        val low = minOf(price, prevClose) * (1 - random.nextDouble(0.0, 0.01))
        return Result.success(
            StockQuote(
                companyId = companyId,
                price = price,
                previousClose = prevClose,
                open = prevClose * (1 + random.nextDouble(-0.005, 0.005)),
                dayHigh = high,
                dayLow = low,
                volume = random.nextLong(500_000L, 20_000_000L),
                asOfEpochMillis = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getHistory(companyId: String, range: ChartRange): Result<List<StockPricePoint>> {
        delay(200)
        val base = basePrice(companyId)
        val random = Random(companyId.hashCode())
        val points = mutableListOf<StockPricePoint>()
        val count = when (range) {
            ChartRange.D1 -> 24 // hourly
            ChartRange.W1 -> 7
            ChartRange.M1 -> 30
            ChartRange.M3 -> 90
            ChartRange.M6 -> 180
            ChartRange.Y1 -> 365
            ChartRange.Y5 -> 60 // monthly-ish samples to keep list small
        }
        val stepMillis = when (range) {
            ChartRange.D1 -> 60L * 60 * 1000
            ChartRange.Y5 -> 30L * 24 * 60 * 60 * 1000
            else -> 24L * 60 * 60 * 1000
        }
        val now = System.currentTimeMillis()
        var value = base * 0.85
        for (i in count downTo 0) {
            val trend = sin(i.toDouble() / count * Math.PI * 2) * base * 0.04
            value = (value + random.nextDouble(-base * 0.01, base * 0.012)).coerceAtLeast(base * 0.4)
            val close = (value + trend).coerceAtLeast(1.0)
            val open = close * (1 + random.nextDouble(-0.008, 0.008))
            val high = maxOf(open, close) * (1 + random.nextDouble(0.0, 0.006))
            val low = minOf(open, close) * (1 - random.nextDouble(0.0, 0.006))
            points += StockPricePoint(
                timestampEpochMillis = now - i * stepMillis,
                open = open, high = high, low = low, close = close,
                volume = random.nextLong(300_000L, 15_000_000L)
            )
        }
        return Result.success(points)
    }

    private fun basePrice(companyId: String): Double = when (companyId) {
        "7203" -> 2850.0
        "7267" -> 1650.0
        "9984" -> 8200.0
        "6758" -> 3100.0
        "9432" -> 165.0
        "8306" -> 1780.0
        "6501" -> 3600.0
        "4661" -> 4600.0
        "9983" -> 47000.0
        "4755" -> 780.0
        "IPO001" -> 3200.0
        else -> 1000.0
    }
}
