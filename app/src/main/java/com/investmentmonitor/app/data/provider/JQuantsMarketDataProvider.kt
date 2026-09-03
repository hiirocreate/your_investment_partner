package com.investmentmonitor.app.data.provider

import com.investmentmonitor.app.data.model.ChartRange
import com.investmentmonitor.app.data.model.StockPricePoint
import com.investmentmonitor.app.data.model.StockQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Real (Phase 2) [MarketDataProvider] backed by the J-Quants API v2
 * (https://jpx-jquants.com/), JPX's official personal-investor data service.
 *
 * Auth: a single static `x-api-key` header, generated once by the user from their own
 * J-Quants dashboard - no email/password ever passes through this app (spec section 47).
 * [apiKey] is supplied per-install from Settings (see SettingsRepository), never hardcoded.
 *
 * IMPORTANT: the J-Quants Free plan delivers data with roughly a 12-week delay. We are
 * honest about that rather than presenting old data as "now" (spec section 40): every quote
 * returned here has [StockQuote.isStale] = true and [StockQuote.asOfEpochMillis] set to the
 * actual trading date of the data, not the current time.
 */
class JQuantsMarketDataProvider(private val apiKey: String) : MarketDataProvider {

    override suspend fun getQuote(companyId: String): Result<StockQuote> = withContext(Dispatchers.IO) {
        try {
            // Free plan data lags real-time, so pull a wide-enough window to reliably find the
            // two most recent trading days available, whatever "recent" means for this plan.
            val to = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"))
            val from = (to.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -21) }
            val bars = fetchBars(companyId, from.time.time, to.time.time).getOrElse { return@withContext Result.failure(it) }
            if (bars.size < 1) return@withContext Result.failure(Exception("No J-Quants data returned for $companyId"))

            val latest = bars.last()
            val previous = if (bars.size >= 2) bars[bars.size - 2] else latest

            Result.success(
                StockQuote(
                    companyId = companyId,
                    price = latest.close,
                    previousClose = previous.close,
                    open = latest.open,
                    dayHigh = latest.high,
                    dayLow = latest.low,
                    volume = latest.volume,
                    asOfEpochMillis = latest.timestampEpochMillis,
                    isStale = true // Free plan is delayed - never claim this is "now"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistory(companyId: String, range: ChartRange): Result<List<StockPricePoint>> =
        withContext(Dispatchers.IO) {
            val to = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"))
            val from = (to.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -range.days) }
            fetchBars(companyId, from.time.time, to.time.time)
        }

    private fun fetchBars(code: String, fromEpochMillis: Long, toEpochMillis: Long): Result<List<StockPricePoint>> {
        var connection: HttpURLConnection? = null
        return try {
            val from = DATE_FORMAT.get()!!.format(java.util.Date(fromEpochMillis))
            val to = DATE_FORMAT.get()!!.format(java.util.Date(toEpochMillis))
            val url = URL("$BASE_URL/v2/equities/bars/daily?code=$code&from=$from&to=$to")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("x-api-key", apiKey)
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                return Result.failure(Exception("J-Quants API returned HTTP $status"))
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val data = JSONObject(body).optJSONArray("data") ?: JSONArray()
            val points = (0 until data.length()).mapNotNull { i -> toPricePoint(data.optJSONObject(i)) }
                .sortedBy { it.timestampEpochMillis }
            Result.success(points)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun toPricePoint(bar: JSONObject?): StockPricePoint? {
        if (bar == null) return null
        val dateStr = bar.optString("Date").ifBlank { return null }
        val timestamp = try {
            DATE_FORMAT.get()!!.parse(dateStr)?.time ?: return null
        } catch (e: Exception) {
            return null
        }
        // Use adjusted OHLC (AdjO/AdjH/AdjL/AdjC) when present so stock splits don't create a
        // visual cliff in the chart; fall back to raw O/H/L/C otherwise.
        val open = bar.optDouble("AdjO", bar.optDouble("O", Double.NaN))
        val high = bar.optDouble("AdjH", bar.optDouble("H", Double.NaN))
        val low = bar.optDouble("AdjL", bar.optDouble("L", Double.NaN))
        val close = bar.optDouble("AdjC", bar.optDouble("C", Double.NaN))
        if (close.isNaN() || open.isNaN() || high.isNaN() || low.isNaN()) return null
        val volume = bar.optDouble("AdjVo", bar.optDouble("Vo", 0.0)).toLong()

        return StockPricePoint(
            timestampEpochMillis = timestamp,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = volume
        )
    }

    companion object {
        private const val BASE_URL = "https://api.jquants.com"

        private val DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat =
                SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Tokyo")
                }
        }
    }
}
