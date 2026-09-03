package com.investmentmonitor.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.data.model.NewsItem
import com.investmentmonitor.app.data.model.TrendingCompany
import com.investmentmonitor.app.data.repository.WatchedCompany
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val importantNews: List<NewsItem> = emptyList(),
    val watchedCompanies: List<WatchedCompany> = emptyList(),
    val trending: List<TrendingCompany> = emptyList(),
    val errorMessage: String? = null,
    val lastUpdatedEpochMillis: Long = 0L
)

class DashboardViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val news = serviceLocator.newsRepository.getLatestNews(limit = 10)
                    .sortedByDescending { it.importance.stars }
                    .take(5)
                val trending = serviceLocator.marketRepository.getTrendingCompanies()
                    .sortedByDescending { it.scores.totalScore }
                    .take(6)
                val watched = serviceLocator.companyRepository.observeWatchedCompanies()
                    .first()
                    .mapNotNull { (entity, company) ->
                        company?.let {
                            WatchedCompany(
                                company = it,
                                settings = com.investmentmonitor.app.data.model.WatchedCompanySettings(
                                    newsMonitoringEnabled = entity.newsMonitoringEnabled,
                                    relatedCompanyMonitoringEnabled = entity.relatedCompanyMonitoringEnabled,
                                    priceAlertsEnabled = entity.priceAlertsEnabled,
                                    importantNewsOnly = entity.importantNewsOnly
                                ),
                                addedAtEpochMillis = entity.addedAtEpochMillis,
                                hasUnseenNews = true
                            )
                        }
                    }

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    importantNews = news,
                    watchedCompanies = watched.take(5),
                    trending = trending,
                    lastUpdatedEpochMillis = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "現在データを更新できません。しばらくしてから再度お試しください。"
                )
            }
        }
    }
}
