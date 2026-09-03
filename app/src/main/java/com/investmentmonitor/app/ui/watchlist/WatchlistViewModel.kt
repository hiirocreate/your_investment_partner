package com.investmentmonitor.app.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.data.model.Company
import com.investmentmonitor.app.data.model.StockQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class WatchlistRow(
    val company: Company,
    val quote: StockQuote?,
    val latestNewsTitle: String?,
    val hasNew: Boolean,
    val newsUnavailable: Boolean = false
)

data class WatchlistUiState(
    val isLoading: Boolean = true,
    val rows: List<WatchlistRow> = emptyList()
)

class WatchlistViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchlistUiState())
    val uiState: StateFlow<WatchlistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            serviceLocator.companyRepository.observeWatchedCompanies().collectLatest { entities ->
                _uiState.value = _uiState.value.copy(isLoading = true)
                val rows = entities.mapNotNull { (entity, company) ->
                    if (company == null) return@mapNotNull null
                    val quote = serviceLocator.marketRepository.getQuote(company.companyId).getOrNull()
                    // A single company's news source failing (offline, TDnet unavailable, etc.)
                    // must not blank out the rest of the watchlist (spec section 41) - degrade
                    // that one row to "news unavailable" instead of crashing/propagating.
                    val newsResult = runCatching {
                        serviceLocator.newsRepository.getNewsForCompany(company.companyId, company.companyName)
                    }
                    val news = newsResult.getOrNull()?.maxByOrNull { it.source.publishedAtEpochMillis }
                    WatchlistRow(
                        company = company,
                        quote = quote,
                        latestNewsTitle = news?.title,
                        hasNew = news != null && System.currentTimeMillis() - news.source.publishedAtEpochMillis < 3 * 60 * 60 * 1000,
                        newsUnavailable = newsResult.isFailure
                    )
                }
                _uiState.value = WatchlistUiState(isLoading = false, rows = rows)
            }
        }
    }

    fun removeFromWatchlist(companyId: String) {
        viewModelScope.launch {
            serviceLocator.companyRepository.removeFromWatchlist(companyId)
        }
    }
}
