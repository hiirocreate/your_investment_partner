package com.investmentmonitor.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.data.model.NewsCategory
import com.investmentmonitor.app.data.model.NewsItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NewsSortOrder(val label: String) {
    NEWEST("新着順"),
    IMPORTANCE("重要度順")
}

data class NewsUiState(
    val isLoading: Boolean = true,
    val allNews: List<NewsItem> = emptyList(),
    val selectedCategory: NewsCategory? = null,
    val sortOrder: NewsSortOrder = NewsSortOrder.NEWEST,
    val errorMessage: String? = null
) {
    val filteredNews: List<NewsItem>
        get() {
            val filtered = if (selectedCategory == null) allNews else allNews.filter { it.category == selectedCategory }
            return when (sortOrder) {
                NewsSortOrder.NEWEST -> filtered.sortedByDescending { it.source.publishedAtEpochMillis }
                NewsSortOrder.IMPORTANCE -> filtered.sortedByDescending { it.importance.stars }
            }
        }
}

class NewsViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val news = serviceLocator.newsRepository.getLatestNews(limit = 100)
                _uiState.value = _uiState.value.copy(isLoading = false, allNews = news)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "ニュースを取得できませんでした。")
            }
        }
    }

    fun onCategorySelected(category: NewsCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun onSortOrderSelected(order: NewsSortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
    }
}
