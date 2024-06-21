package com.example

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import android.util.Log

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
        val doc = app.get(url, referer = referer).text
        if (doc.isNotBlank()) {

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


                // https://s18.upstreamcdn.co/hls2/01/01097/9qx7lhnoezn_n/master.m3u8?t=bYYSztvRHlImhy_PjVqV91W7oXRu4LXALz76pLJPFI&s=1719404641&e=10800&f=5485070&i=0.0&sp=0
                val linkUrl = "https://${fullDomain}/hls2/${n1}/${n2}/${id}/master.m3u8?t=${t}&s=${s}&e=${e}&f=${f}&i=${i}&sp=${sp}"

                Log.d("mnemo", "Testing ${linkUrl}")
                M3u8Helper.generateM3u8(
                    this.name,
                    linkUrl,
                    "$mainUrl/",
                    headers = mapOf("Origin" to mainUrl)
                ).forEach(callback)
            }
        }
        else{
            Log.d("mnemo", "Got nothing, are you banned ?")
        }
    }
}