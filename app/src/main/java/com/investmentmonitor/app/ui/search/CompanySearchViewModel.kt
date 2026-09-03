package com.investmentmonitor.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.data.model.Company
import com.investmentmonitor.app.data.model.CorporateNumberCandidate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CompanySearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<Company> = emptyList(),
    val selectedCompany: Company? = null,
    val corporateCandidates: List<CorporateNumberCandidate> = emptyList(),
    val isLoadingCandidates: Boolean = false,
    val registeredCompanyId: String? = null
)

class CompanySearchViewModel(private val serviceLocator: ServiceLocator) : ViewModel() {

    private val _uiState = MutableStateFlow(CompanySearchUiState())
    val uiState: StateFlow<CompanySearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query, selectedCompany = null, corporateCandidates = emptyList())
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(250) // light debounce so we don't "search" on every keystroke
            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = serviceLocator.companyRepository.searchCompanies(query).getOrElse { emptyList() }
            _uiState.value = _uiState.value.copy(results = results, isSearching = false)
        }
    }

    fun onCompanySelected(company: Company) {
        _uiState.value = _uiState.value.copy(selectedCompany = company, isLoadingCandidates = true)
        viewModelScope.launch {
            val candidates = serviceLocator.corporateNumberProvider()
                .findCandidates(company.companyName).getOrElse { emptyList() }
            _uiState.value = _uiState.value.copy(corporateCandidates = candidates, isLoadingCandidates = false)
        }
    }

    fun register(company: Company) {
        viewModelScope.launch {
            serviceLocator.companyRepository.addToWatchlist(company)
            _uiState.value = _uiState.value.copy(registeredCompanyId = company.companyId)
        }
    }
}
