package com.investmentmonitor.app

import android.content.Context
import com.investmentmonitor.app.data.local.AppDatabase
import com.investmentmonitor.app.data.local.SettingsRepository
import com.investmentmonitor.app.data.provider.CompositeCorporateNumberProvider
import com.investmentmonitor.app.data.provider.CompositeMarketDataProvider
import com.investmentmonitor.app.data.provider.MockCompanyProvider
import com.investmentmonitor.app.data.provider.TdnetNewsProvider
import com.investmentmonitor.app.data.repository.CompanyRepository
import com.investmentmonitor.app.data.repository.MarketRepository
import com.investmentmonitor.app.data.repository.NewsRepository

/**
 * Deliberately simple, hand-rolled dependency container instead of a DI framework
 * (Hilt/Dagger) - keeps the APK smaller and the build simpler for Phase 1 (spec section 4).
 * If the app grows significantly in later phases this can be replaced with Hilt without
 * touching Provider/Repository interfaces.
 */
class ServiceLocator(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    private val database by lazy { AppDatabase.getInstance(appContext) }

    private val companyProvider by lazy { MockCompanyProvider() }
    // Stock prices (J-Quants) and corporate number lookup (国税庁法人番号Web-API) use whatever
    // API credential the CURRENT user has entered in Settings - each install/user can supply
    // their own account, nothing is baked into the build (spec section 47). Falls back to
    // Mock automatically when no key is set, or the real call fails.
    private val corporateNumberProvider by lazy { CompositeCorporateNumberProvider(settingsRepository) }
    private val marketDataProvider by lazy { CompositeMarketDataProvider(settingsRepository) }
    private val newsProvider by lazy { TdnetNewsProvider() }

    val companyRepository: CompanyRepository by lazy {
        CompanyRepository(companyProvider, database.watchedCompanyDao())
    }
    val newsRepository: NewsRepository by lazy { NewsRepository(newsProvider) }
    val marketRepository: MarketRepository by lazy { MarketRepository(marketDataProvider, companyProvider) }

    fun corporateNumberProvider() = corporateNumberProvider

    companion object {
        @Volatile private var instance: ServiceLocator? = null
        fun getInstance(context: Context): ServiceLocator =
            instance ?: synchronized(this) {
                instance ?: ServiceLocator(context).also { instance = it }
            }
    }
}
