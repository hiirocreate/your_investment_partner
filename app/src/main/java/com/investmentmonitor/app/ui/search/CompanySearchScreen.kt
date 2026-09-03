package com.investmentmonitor.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.investmentmonitor.app.ServiceLocator
import com.investmentmonitor.app.data.model.Company
import com.investmentmonitor.app.ui.components.EmptyState
import com.investmentmonitor.app.ui.util.simpleViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySearchScreen(
    serviceLocator: ServiceLocator,
    onBack: () -> Unit,
    onRegistered: () -> Unit
) {
    val viewModel: CompanySearchViewModel = viewModel(
        factory = simpleViewModelFactory { CompanySearchViewModel(serviceLocator) }
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.registeredCompanyId) {
        if (state.registeredCompanyId != null) onRegistered()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("企業を検索") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                label = { Text("企業名・証券コードを入力") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true
            )

            val selected = state.selectedCompany
            if (selected != null) {
                CompanyConfirmationCard(
                    company = selected,
                    candidatesCount = state.corporateCandidates.size,
                    isLoadingCandidates = state.isLoadingCandidates,
                    onConfirm = { viewModel.register(selected) }
                )
            } else if (state.results.isEmpty() && state.query.isNotBlank() && !state.isSearching) {
                EmptyState("該当する企業が見つかりませんでした。")
            } else {
                LazyColumn {
                    items(state.results, key = { it.companyId }) { company ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { viewModel.onCompanySelected(company) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(company.companyName, fontWeight = FontWeight.Bold)
                                Text(
                                    "${company.officialName} ・ ${company.stockCode ?: "コード不明"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyConfirmationCard(
    company: Company,
    candidatesCount: Int,
    isLoadingCandidates: Boolean,
    onConfirm: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("企業情報の確認", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("企業名", company.companyName)
            InfoRow("正式名称", company.officialName)
            InfoRow("証券コード", company.stockCode ?: "-")
            InfoRow("市場", company.exchange.displayName)
            InfoRow("業種", company.industry)
            if (company.corporateNumber != null) {
                InfoRow("法人番号", company.corporateNumber)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (isLoadingCandidates) "法人番号の候補を確認中..." else "法人番号候補: ${candidatesCount}件見つかりました",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text("この企業を監視する")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
