package com.investmentmonitor.app.data.provider

import com.investmentmonitor.app.data.model.CorporateNumberCandidate
import com.investmentmonitor.app.data.model.Exchange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Real (Phase 2) [CorporateNumberProvider] backed by 国税庁 法人番号システム Web-API
 * (https://www.houjin-bangou.nta.go.jp/webapi/index.html), the official free government API.
 *
 * Auth: an "アプリケーションID" the user personally applies for by emailing
 * invoice-web-api@nta.go.jp (free, but requires their own request - see README). [applicationId]
 * is supplied per-install from Settings, never hardcoded (spec section 47).
 *
 * The API only returns XML/CSV (no JSON), so this uses the JVM's built-in DOM parser
 * (javax.xml.parsers) rather than adding an XML library dependency.
 *
 * Per the API's own terms, any UI built on this data must credit the source - see the
 * disclosure line rendered in CompanySearchScreen / documented in README.
 */
class HoujinBangouCorporateNumberProvider(private val applicationId: String) : CorporateNumberProvider {

    override suspend fun findCandidates(companyName: String): Result<List<CorporateNumberCandidate>> =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val encodedName = URLEncoder.encode(companyName, "UTF-8")
                val encodedId = URLEncoder.encode(applicationId, "UTF-8")
                // type=12: XML output. mode=2: 部分一致(contains) search, broader than prefix-only.
                val url = URL("$BASE_URL/name?id=$encodedId&name=$encodedName&type=12&mode=2")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
                val status = connection.responseCode
                if (status !in 200..299) {
                    return@withContext Result.failure(Exception("法人番号Web-API returned HTTP $status"))
                }
                val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(connection.inputStream)
                val nodes = document.getElementsByTagName("corporation")
                val candidates = (0 until nodes.length).mapNotNull { i ->
                    (nodes.item(i) as? Element)?.let { toCandidate(it) }
                }.take(MAX_CANDIDATES)
                Result.success(candidates)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }

    private fun toCandidate(element: Element): CorporateNumberCandidate? {
        val corporateNumber = firstText(element, "corporateNumber") ?: return null
        val name = firstText(element, "name") ?: return null
        val prefecture = firstText(element, "prefectureName").orEmpty()
        val city = firstText(element, "cityName").orEmpty()
        val street = firstText(element, "streetNumber").orEmpty()
        return CorporateNumberCandidate(
            corporateNumber = corporateNumber,
            officialName = name,
            location = listOf(prefecture, city, street).filter { it.isNotBlank() }.joinToString(""),
            stockCode = null, // this API does not provide stock codes; CompanyProvider supplies those
            exchange = Exchange.UNKNOWN
        )
    }

    private fun firstText(element: Element, tagName: String): String? {
        val nodeList = element.getElementsByTagName(tagName)
        if (nodeList.length == 0) return null
        return nodeList.item(0)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val BASE_URL = "https://api.houjin-bangou.nta.go.jp/4"
        private const val MAX_CANDIDATES = 10
    }
}
