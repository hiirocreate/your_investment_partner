package com.investmentmonitor.app

import com.investmentmonitor.app.data.provider.MockCompanyProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanySearchTest {

    private val provider = MockCompanyProvider()

    @Test
    fun `searching by partial company name returns a match`() = runTest {
        val result = provider.searchCompanies("トヨタ").getOrThrow()
        assertTrue(result.any { it.companyId == "7203" })
    }

    @Test
    fun `searching by stock code returns a match`() = runTest {
        val result = provider.searchCompanies("9984").getOrThrow()
        assertTrue(result.any { it.companyId == "9984" })
    }

    @Test
    fun `blank query returns no results`() = runTest {
        val result = provider.searchCompanies("   ").getOrThrow()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `unmatched query returns no results`() = runTest {
        val result = provider.searchCompanies("該当しない企業名XYZ").getOrThrow()
        assertTrue(result.isEmpty())
    }
}
