package com.investmentmonitor.app.data.provider

import com.investmentmonitor.app.data.model.Company
import com.investmentmonitor.app.data.model.CompanyRelation
import com.investmentmonitor.app.data.model.Exchange
import kotlinx.coroutines.delay

/**
 * Abstraction over "where company master data comes from".
 * Phase 1 ships only [MockCompanyProvider]. A real implementation (e.g. backed by a
 * corporate-database API) can be swapped in later without touching UI/ViewModel code.
 */
interface CompanyProvider {
    suspend fun searchCompanies(query: String): Result<List<Company>>
    suspend fun getCompany(companyId: String): Result<Company?>
    suspend fun getRelatedCompanies(companyId: String): Result<List<CompanyRelation>>
}

class MockCompanyProvider : CompanyProvider {

    override suspend fun searchCompanies(query: String): Result<List<Company>> {
        delay(200) // simulate network latency
        if (query.isBlank()) return Result.success(emptyList())
        val normalized = query.trim()
        val matches = sampleCompanies.filter { company ->
            company.companyName.contains(normalized) ||
                company.officialName.contains(normalized) ||
                (company.stockCode?.contains(normalized) == true) ||
                readingIndex[company.companyId]?.any { it.contains(normalized) } == true
        }
        return Result.success(matches)
    }

    override suspend fun getCompany(companyId: String): Result<Company?> {
        delay(100)
        return Result.success(sampleCompanies.firstOrNull { it.companyId == companyId })
    }

    override suspend fun getRelatedCompanies(companyId: String): Result<List<CompanyRelation>> {
        delay(150)
        return Result.success(relationsIndex[companyId].orEmpty())
    }

    companion object {
        val sampleCompanies: List<Company> = listOf(
            Company(
                companyId = "7203", companyName = "トヨタ自動車", officialName = "トヨタ自動車株式会社",
                corporateNumber = "6180301018771", stockCode = "7203", exchange = Exchange.TSE_PRIME,
                website = "https://global.toyota/jp/", irUrl = "https://global.toyota/jp/ir/",
                industry = "輸送用機器", marketCapBillionYen = 45000.0, per = 10.2, pbr = 1.1, roe = 11.5,
                revenueBillionYen = 45000.0, operatingIncomeBillionYen = 4800.0, fiveYearGrowthScore = 72
            ),
            Company(
                companyId = "7267", companyName = "ホンダ", officialName = "本田技研工業株式会社",
                corporateNumber = "7010401027570", stockCode = "7267", exchange = Exchange.TSE_PRIME,
                parentCompanyId = null, industry = "輸送用機器", marketCapBillionYen = 8900.0,
                per = 7.8, pbr = 0.7, roe = 9.0, revenueBillionYen = 20400.0, operatingIncomeBillionYen = 1300.0,
                fiveYearGrowthScore = 58
            ),
            Company(
                companyId = "9984", companyName = "ソフトバンクグループ", officialName = "ソフトバンクグループ株式会社",
                corporateNumber = "5010401018261", stockCode = "9984", exchange = Exchange.TSE_PRIME,
                industry = "情報・通信業", marketCapBillionYen = 12000.0, per = 14.5, pbr = 1.3, roe = 6.2,
                revenueBillionYen = 6200.0, operatingIncomeBillionYen = 900.0, fiveYearGrowthScore = 65
            ),
            Company(
                companyId = "6758", companyName = "ソニーグループ", officialName = "ソニーグループ株式会社",
                corporateNumber = "3010401067193", stockCode = "6758", exchange = Exchange.TSE_PRIME,
                industry = "電気機器", marketCapBillionYen = 16000.0, per = 19.4, pbr = 2.5, roe = 12.8,
                revenueBillionYen = 13000.0, operatingIncomeBillionYen = 1350.0, fiveYearGrowthScore = 80
            ),
            Company(
                companyId = "9432", companyName = "NTT", officialName = "日本電信電話株式会社",
                corporateNumber = "7010001008844", stockCode = "9432", exchange = Exchange.TSE_PRIME,
                industry = "情報・通信業", marketCapBillionYen = 15500.0, per = 12.1, pbr = 1.6, roe = 13.9,
                revenueBillionYen = 13400.0, operatingIncomeBillionYen = 1800.0, fiveYearGrowthScore = 55
            ),
            Company(
                companyId = "8306", companyName = "三菱UFJフィナンシャル・グループ", officialName = "株式会社三菱UFJフィナンシャル・グループ",
                corporateNumber = "7010001008889", stockCode = "8306", exchange = Exchange.TSE_PRIME,
                industry = "銀行業", marketCapBillionYen = 22000.0, per = 11.0, pbr = 0.9, roe = 8.5,
                revenueBillionYen = 7000.0, operatingIncomeBillionYen = 1900.0, fiveYearGrowthScore = 61
            ),
            Company(
                companyId = "6501", companyName = "日立製作所", officialName = "株式会社日立製作所",
                corporateNumber = "5010001008771", stockCode = "6501", exchange = Exchange.TSE_PRIME,
                industry = "電気機器", marketCapBillionYen = 17000.0, per = 22.0, pbr = 2.9, roe = 14.0,
                revenueBillionYen = 9600.0, operatingIncomeBillionYen = 900.0, fiveYearGrowthScore = 84
            ),
            Company(
                companyId = "4661", companyName = "オリエンタルランド", officialName = "株式会社オリエンタルランド",
                corporateNumber = "4030001018108", stockCode = "4661", exchange = Exchange.TSE_PRIME,
                industry = "サービス業", marketCapBillionYen = 5200.0, per = 32.0, pbr = 6.5, roe = 18.2,
                revenueBillionYen = 630.0, operatingIncomeBillionYen = 160.0, fiveYearGrowthScore = 70
            ),
            Company(
                companyId = "9983", companyName = "ファーストリテイリング", officialName = "株式会社ファーストリテイリング",
                corporateNumber = "7011001008738", stockCode = "9983", exchange = Exchange.TSE_PRIME,
                industry = "小売業", marketCapBillionYen = 13000.0, per = 38.0, pbr = 9.0, roe = 23.0,
                revenueBillionYen = 3100.0, operatingIncomeBillionYen = 500.0, fiveYearGrowthScore = 88
            ),
            Company(
                companyId = "4755", companyName = "楽天グループ", officialName = "楽天グループ株式会社",
                corporateNumber = "5011001048510", stockCode = "4755", exchange = Exchange.TSE_PRIME,
                industry = "サービス業", marketCapBillionYen = 1700.0, per = null, pbr = 3.0, roe = -12.0,
                revenueBillionYen = 2100.0, operatingIncomeBillionYen = -150.0, fiveYearGrowthScore = 30
            ),
            Company(
                companyId = "IPO001", companyName = "ネクストAIホールディングス", officialName = "株式会社ネクストAIホールディングス",
                corporateNumber = "1234567890123", stockCode = "9999", exchange = Exchange.TSE_GROWTH,
                industry = "情報・通信業", marketCapBillionYen = 85.0, per = 45.0, pbr = 8.0, roe = 15.0,
                revenueBillionYen = 12.0, operatingIncomeBillionYen = 1.5, fiveYearGrowthScore = 90
            )
        )

        // Simple reading/alias index so partial or kana-ish queries still surface results in Phase 1.
        private val readingIndex: Map<String, List<String>> = mapOf(
            "7203" to listOf("とよた", "TOYOTA", "toyota"),
            "9984" to listOf("SBG", "ソフトバンク"),
            "9432" to listOf("エヌティティ", "NTT", "日本電信電話"),
            "6758" to listOf("SONY", "sony"),
            "9983" to listOf("ユニクロ", "UNIQLO")
        )

        private val relationsIndex: Map<String, List<CompanyRelation>> = mapOf(
            "9984" to listOf(
                CompanyRelation("9984", sampleCompanies.first { it.companyId == "6758" }, com.investmentmonitor.app.data.model.RelationType.AFFILIATE, autoSuggested = true)
            ),
            "7203" to listOf(
                CompanyRelation("7203", sampleCompanies.first { it.companyId == "7267" }, com.investmentmonitor.app.data.model.RelationType.PARTNER, autoSuggested = true)
            )
        )
    }
}
