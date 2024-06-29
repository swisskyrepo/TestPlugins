package com.example

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString


// Define data classes for the JSON structure
@Serializable
data class Question(
    val name: String,
    val type: Int
)

@Serializable
data class Answer(
    val name: String,
    val type: Int,
    val TTL: Int,
    val data: String
)

@Serializable
data class ApiResponse(
    val Status: Int,
    val TC: Boolean,
    val RD: Boolean,
    val RA: Boolean,
    val AD: Boolean,
    val CD: Boolean,
    val Question: List<Question>,
    val Answer: List<Answer>
)


class Upstream : ExtractorApi() {
    override val name: String = "Upstream"
    override val mainUrl: String = "https://upstream.to"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("mnemo", "Upstream extractor enabled")
        Log.d("mnemo", "Test-3")

        // // Bypass ISP blocking with DNS over HTTP to resolve the IP for upstream
        // val dnsDoc = app.get(
        //     "https://cloudflare-dns.com/dns-query?name=upstream.to&type=A", 
        //     headers = mapOf(
        //         "accept" to "application/dns-json"
        //     )
        // ).text

        // // Parse JSON string to ApiResponse object
        // val apiResponse = Json.decodeFromString<ApiResponse>(dnsDoc)

        // // Extract the desired value
        // var ipAddress = if (apiResponse.Answer.isNotEmpty()) apiResponse.Answer[0].data else "185.178.208.135"
        var ipAddress = "185.178.208.135"
        Log.d("mnemo", "IP ${ipAddress}")

        // Connect to upstream using the IP and set the Host header
        // curl https://185.178.208.135/embed-9qx7lhanoezn.html -k -H 'Host: upstream.to'
        val doc = app.get(url.replace("upstream.to", ipAddress),
            headers = mapOf(
                "Host" to "upstream.to"
            ),
            referer = referer).text


        if (doc.isNotBlank()) {
            Log.d("mnemo", "Doc loaded")

            // Find the matches in |file|01097|01|co|upstreamcdn|s18|HTTP
            val regex1 = """\|file\|(\d+)\|(\d+)\|(\w+)\|(\w+)\|(\w+)\|HTTP""".toRegex()
            val matchResult1 = regex1.find(doc)

            // Find the matches in master|9qx7lhanoezn_n|hls2
            val regex2 = """\|master\|(\w+)\|hls2""".toRegex()
            val matchResult2 = regex2.find(doc)

            // Find the matches in sp|10800|bYYSztvRHlImhy_PjVqV91W7EoXRu4LXALz76pLJPFI|m3u8
            val regex3 = """sp\|(\d+)\|(\w+)\|m3u8""".toRegex()
            val matchResult3 = regex3.find(doc)
            

            // Find the matches in data|1719404641|5485070||hide
            val regex4 = """data\|(\d+)\|(\d+)\|\|hide""".toRegex()
            val matchResult4 = regex4.find(doc)


            if (matchResult1 != null && matchResult2 != null && matchResult3 != null && matchResult4 != null) {
                val (n2, n1, tld, domain, subdomain) = matchResult1.destructured
                var id = matchResult2.destructured // "9qx7lhanoezn_n"
                var (e,t) = matchResult3.destructured // sp|10800|bYYSztvRHlImhy_PjVqV91W7EoXRu4LXALz76pLJPFI|m3u8
                var (s,f) = matchResult4.destructured // |data|1719404641|5485070||hide|
                val fullDomain = "$subdomain.$domain.$tld"

                Log.d("mnemo", "n2 = \"$n2\"")
                Log.d("mnemo", "n1 = \"$n1\"")
                Log.d("mnemo", "domain = \"$fullDomain\"")
                Log.d("mnemo", "id = \"$id\"")
                Log.d("mnemo", "t = \"$t\"")
                Log.d("mnemo", "e = \"$e\"")
                Log.d("mnemo", "s = \"$s\"")
                Log.d("mnemo", "f = \"$f\"")

                var i = "0.0" // &i=0.0&5
                var sp = "0" //

                val linkUrl = "https://${fullDomain}/hls2/${n1}/${n2}/${id}/master.m3u8?t=${t}&s=${s}&e=${e}&f=${f}&i=${i}&sp=${sp}"
                Log.d("mnemo", "Testing ${linkUrl}")

                M3u8Helper.generateM3u8(
                    this.name,
                    linkUrl.replace("s18.upstreamcdn.co", "51.83.140.60"),
                    "$mainUrl/",
                    headers = mapOf(
                        "Host" to "s18.upstreamcdn.co",
                        "Origin" to mainUrl
                    )
                ).forEach(callback)
            }
        }
        else{
            Log.d("mnemo", "Got nothing, are you banned ?")
        }
    }
}