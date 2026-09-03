package com.investmentmonitor.app.ui.companydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.data.model.ChartRange
import com.investmentmonitor.app.data.model.Company
import com.investmentmonitor.app.data.model.CompanyRelation
import com.investmentmonitor.app.data.model.NewsItem
import com.investmentmonitor.app.data.model.StockPricePoint
import com.investmentmonitor.app.data.model.StockQuote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CompanyDetailUiState(
    val isLoading: Boolean = true,
    val company: Company? = null,
    val quote: StockQuote? = null,
    val history: List<StockPricePoint> = emptyList(),
    val selectedRange: ChartRange = ChartRange.M1,
    val news: List<NewsItem> = emptyList(),
    val relations: List<CompanyRelation> = emptyList(),
    val isWatched: Boolean = false,
    val errorMessage: String? = null
)

class CompanyDetailViewModel(
    private val serviceLocator: ServiceLocator,
    private val companyId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanyDetailUiState())
    val uiState: StateFlow<CompanyDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val company = serviceLocator.companyRepository.getCompany(companyId)
                val quote = serviceLocator.marketRepository.getQuote(companyId).getOrNull()
                val history = serviceLocator.marketRepository.getHistory(companyId, _uiState.value.selectedRange).getOrElse { emptyList() }
                val news = company?.let { serviceLocator.newsRepository.getNewsForCompany(companyId, it.companyName) }.orEmpty()
                val relations = serviceLocator.companyRepository.getRelatedCompanies(companyId)
                val isWatched = serviceLocator.companyRepository.isWatched(companyId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    company = company,
                    quote = quote,
                    history = history,
                    news = news,
                    relations = relations,
                    isWatched = isWatched
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "データを取得できませんでした。")
            }
        }
    }

    fun onRangeSelected(range: ChartRange) {
        _uiState.value = _uiState.value.copy(selectedRange = range)
        viewModelScope.launch {
            val history = serviceLocator.marketRepository.getHistory(companyId, range).getOrElse { emptyList() }
            _uiState.value = _uiState.value.copy(history = history)
        }
    }

    fun toggleWatch() {
        viewModelScope.launch {
            val company = _uiState.value.company ?: return@launch
            if (_uiState.value.isWatched) {
                serviceLocator.companyRepository.removeFromWatchlist(company.companyId)
            } else {
                serviceLocator.companyRepository.addToWatchlist(company)
            }
            _uiState.value = _uiState.value.copy(isWatched = !_uiState.value.isWatched)
        }
    }
}
