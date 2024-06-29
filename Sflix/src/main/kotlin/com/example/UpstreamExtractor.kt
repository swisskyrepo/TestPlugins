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

        // Bypass ISP blocking with DNS over HTTP to resolve the IP for upstream
        // val dnsDoc = app.get(
        //     "https://cloudflare-dns.com/dns-query?name=upstream.to&type=A", 
        //     headers = mapOf(
        //         "accept" to "application/dns-json"
        //     )
        // ).text

        // Parse JSON string to ApiResponse object
        // val apiResponse = Json.decodeFromString<ApiResponse>(dnsDoc)

        // Extract the desired value
        // val ipAddress = if (apiResponse.Answer.isNotEmpty()) apiResponse.Answer[0].data else "185.178.208.135"
        // val ipAddress = apiResponse.Answer.find { it.name == "upstream.to" }?.data
        val ipAddress = "185.178.208.135"
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
            val regex = """\|file\|(\d+)\|(\d+)\|(\w+)\|(\w+)\|(\w+)\|HTTP""".toRegex()
            val matchResult = regex.find(doc)

            if (matchResult != null) {
                val (n2, n1, tld, domain, subdomain) = matchResult.destructured
                val fullDomain = "$subdomain.$domain.$tld"

                Log.d("mnemo", "n2 = \"$n2\"")
                Log.d("mnemo", "n1 = \"$n1\"")
                Log.d("mnemo", "domain = \"$fullDomain\"")

                var id = "9qx7lhanoezn_n" // master|9qx7lhanoezn_n|hls2


                var t = "bYYSztvRHlImhy_PjVqV91W7EoXRu4LXALz76pLJPFI" // sp|10800|bYYSztvRHlImhy_PjVqV91W7EoXRu4LXALz76pLJPFI|m3u8
                var e = "10800"


                var s = "1719404641" // |data|1719404641|5485070||hide|
                var f = "5485070"


                var i = "0.0" // &i=0.0&5
                var sp = "0" // TODO

                // ${fullDomain}
                // https://s18.upstreamcdn.co/hls2/01/01097/9qx7lhnoezn_n/master.m3u8?t=bYYSztvRHlImhy_PjVqV91W7oXRu4LXALz76pLJPFI&s=1719404641&e=10800&f=5485070&i=0.0&sp=0
                val linkUrl = "https://51.83.140.60/hls2/${n1}/${n2}/${id}/master.m3u8?t=${t}&s=${s}&e=${e}&f=${f}&i=${i}&sp=${sp}"
                Log.d("mnemo", "Testing ${linkUrl}")

                M3u8Helper.generateM3u8(
                    this.name,
                    linkUrl,
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