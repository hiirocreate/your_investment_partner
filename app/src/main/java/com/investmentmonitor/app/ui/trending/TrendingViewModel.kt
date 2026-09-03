package com.investmentmonitor.app.ui.trending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.data.model.IpoCompany
import com.investmentmonitor.app.data.model.TrendCategory
import com.investmentmonitor.app.data.model.TrendingCompany
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrendingUiState(
    val isLoading: Boolean = true,
    val selectedCategory: TrendCategory = TrendCategory.SURGING,
    val companies: List<TrendingCompany> = emptyList(),
    val ipoCompanies: List<IpoCompany> = emptyList(),
    val errorMessage: String? = null
) {
    val filtered: List<TrendingCompany>
        get() = companies.filter { selectedCategory in it.categories }
            .sortedByDescending { it.scores.totalScore }
}

class TrendingViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendingUiState())
    val uiState: StateFlow<TrendingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val companies = serviceLocator.marketRepository.getTrendingCompanies()
                val ipos = serviceLocator.marketRepository.getIpoCompanies()
                _uiState.value = _uiState.value.copy(isLoading = false, companies = companies, ipoCompanies = ipos)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "データを取得できませんでした。")
            }
        }
    }

    fun onCategorySelected(category: TrendCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }
}
