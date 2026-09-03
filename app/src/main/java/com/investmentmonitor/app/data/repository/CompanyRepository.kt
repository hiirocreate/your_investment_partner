package com.investmentmonitor.app.data.repository

import com.investmentmonitor.app.data.local.WatchedCompanyDao
import com.investmentmonitor.app.data.local.WatchedCompanyEntity
import com.investmentmonitor.app.data.model.Company
import com.investmentmonitor.app.data.model.CompanyRelation
import com.investmentmonitor.app.data.model.WatchedCompanySettings
import com.investmentmonitor.app.data.provider.CompanyProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class WatchedCompany(
    val company: Company,
    val settings: WatchedCompanySettings,
    val addedAtEpochMillis: Long,
    val hasUnseenNews: Boolean
)

class CompanyRepository(
    private val companyProvider: CompanyProvider,
    private val watchedCompanyDao: WatchedCompanyDao
) {
    suspend fun searchCompanies(query: String): Result<List<Company>> =
        companyProvider.searchCompanies(query)

    suspend fun getCompany(companyId: String): Company? =
        companyProvider.getCompany(companyId).getOrNull()

    suspend fun getRelatedCompanies(companyId: String): List<CompanyRelation> =
        companyProvider.getRelatedCompanies(companyId).getOrElse { emptyList() }

    fun observeWatchedIds(): Flow<Set<String>> =
        watchedCompanyDao.observeAll().map { list -> list.map { it.companyId }.toSet() }

    fun observeWatchedCompanies(): Flow<List<Pair<WatchedCompanyEntity, Company?>>> =
        watchedCompanyDao.observeAll().map { entities ->
            entities.map { entity -> entity to MockLookup.byId(entity.companyId) }
        }

    suspend fun isWatched(companyId: String): Boolean =
        watchedCompanyDao.findById(companyId) != null

    suspend fun addToWatchlist(company: Company) {
        val existing = watchedCompanyDao.findById(company.companyId)
        if (existing == null) {
            watchedCompanyDao.upsert(
                WatchedCompanyEntity(
                    companyId = company.companyId,
                    companyName = company.companyName,
                    addedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun removeFromWatchlist(companyId: String) {
        watchedCompanyDao.deleteById(companyId)
    }

    suspend fun updateSettings(companyId: String, settings: WatchedCompanySettings) {
        val existing = watchedCompanyDao.findById(companyId) ?: return
        watchedCompanyDao.update(
            existing.copy(
                newsMonitoringEnabled = settings.newsMonitoringEnabled,
                relatedCompanyMonitoringEnabled = settings.relatedCompanyMonitoringEnabled,
                priceAlertsEnabled = settings.priceAlertsEnabled,
                importantNewsOnly = settings.importantNewsOnly
            )
        )
    }

    // Small helper so we can resolve full Company objects for watched entities without
    // making CompanyRepository suspend-heavy in the Flow mapper above.
    private object MockLookup {
        fun byId(id: String): Company? =
            com.investmentmonitor.app.data.provider.MockCompanyProvider.sampleCompanies
                .firstOrNull { it.companyId == id }
    }
}
