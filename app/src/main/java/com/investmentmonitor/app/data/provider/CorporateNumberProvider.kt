package com.investmentmonitor.app.data.provider

import com.investmentmonitor.app.data.model.CorporateNumberCandidate
import com.investmentmonitor.app.data.model.Exchange
import kotlinx.coroutines.delay

/**
 * Looks up 法人番号 (corporate number) candidates for a company name.
 * IMPORTANT: never auto-confirms a single candidate - the UI must always let the
 * user pick from a list (spec section 16).
 *
 * Phase 2 candidate: 国税庁 法人番号公表サイト Web-API (https://www.houjin-bangou.nta.go.jp/webapi/)
 * which is free and does not require an application-level API key for basic queries.
 */
interface CorporateNumberProvider {
    suspend fun findCandidates(companyName: String): Result<List<CorporateNumberCandidate>>
}

class MockCorporateNumberProvider : CorporateNumberProvider {
    override suspend fun findCandidates(companyName: String): Result<List<CorporateNumberCandidate>> {
        delay(250)
        val hit = MockCompanyProvider.sampleCompanies.filter {
            it.companyName.contains(companyName) || companyName.contains(it.companyName)
        }
        val candidates = hit.map {
            CorporateNumberCandidate(
                corporateNumber = it.corporateNumber ?: "不明",
                officialName = it.officialName,
                location = "東京都(モックデータ)",
                stockCode = it.stockCode,
                exchange = it.exchange
            )
        }
        return Result.success(candidates)
    }
}
