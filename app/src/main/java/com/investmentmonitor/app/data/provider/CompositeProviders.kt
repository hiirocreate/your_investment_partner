package com.investmentmonitor.app.data.provider

import com.investmentmonitor.app.data.local.SettingsRepository
import com.investmentmonitor.app.data.model.ChartRange
import com.investmentmonitor.app.data.model.CorporateNumberCandidate
import com.investmentmonitor.app.data.model.StockPricePoint
import com.investmentmonitor.app.data.model.StockQuote
import kotlinx.coroutines.flow.first

/**
 * Reads the *current* per-user API credential from [SettingsRepository] on every call and
 * routes to the real provider when one is configured, otherwise to [MockMarketDataProvider].
 *
 * This is what makes "each user can plug in their own API account" (rather than one shared
 * key baked into the APK at build time) work: nothing here is fixed at compile time, and a
 * user can add/remove/replace their key from Settings at any point without reinstalling.
 *
 * If the real provider is configured but a call to it fails (network, quota, bad key, the
 * remote API being unreachable, etc.) this silently falls back to Mock data rather than
 * crashing or blanking the screen (spec section 41) - callers can't tell the difference from
 * a successful Mock call, which is an intentional trade-off for Phase 2: a wrong/expired key
 * degrades to "looks like Mock data" rather than an error banner. Revisit if that trade-off
 * turns out to be confusing in practice.
 */
class CompositeMarketDataProvider(
    private val settingsRepository: SettingsRepository,
    private val mock: MarketDataProvider = MockMarketDataProvider()
) : MarketDataProvider {

    override suspend fun getQuote(companyId: String): Result<StockQuote> {
        val apiKey = settingsRepository.jquantsApiKey.first()
        if (apiKey != null) {
            JQuantsMarketDataProvider(apiKey).getQuote(companyId).onSuccess { return Result.success(it) }
        }
        return mock.getQuote(companyId)
    }

    override suspend fun getHistory(companyId: String, range: ChartRange): Result<List<StockPricePoint>> {
        val apiKey = settingsRepository.jquantsApiKey.first()
        if (apiKey != null) {
            JQuantsMarketDataProvider(apiKey).getHistory(companyId, range).onSuccess { return Result.success(it) }
        }
        return mock.getHistory(companyId, range)
    }
}

class CompositeCorporateNumberProvider(
    private val settingsRepository: SettingsRepository,
    private val mock: CorporateNumberProvider = MockCorporateNumberProvider()
) : CorporateNumberProvider {

    override suspend fun findCandidates(companyName: String): Result<List<CorporateNumberCandidate>> {
        val appId = settingsRepository.houjinBangouAppId.first()
        if (appId != null) {
            HoujinBangouCorporateNumberProvider(appId).findCandidates(companyName).onSuccess { return Result.success(it) }
        }
        return mock.findCandidates(companyName)
    }
}
