package com.example

import android.util.Base64
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import android.util.Log

class Voe2 : ExtractorApi() {
    override val name = "Voe"
    override val mainUrl = "https://voe.sx"
    override val requiresReferer = true
	
	private val linkRegex = "(http|https)://([\\w_-]+(?:\\.[\\w_-]+)+)([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])".toRegex()
    private val base64Regex = Regex("'.*'")

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        Log.d("mnemo", "voe.sx loaded")

        // Extract the first redirect URL, like https://roberteachfinal.com/e/xxxxxxx
        val voeDoc = app.get(url, referer = referer).document
        val redirRegex = """window.location.href = '(.*)'""".toRegex()
        val redirResult = redirRegex.find(voeDoc.html())?.groupValues?.get(1)
        if (redirResult != null){
            Log.d("mnemo", "voe.sx redirect: ${redirResult}")
            val res = app.get(redirResult, referer = referer).document
            val script = res.select("script").find { it.data().contains("sources =") }?.data()
            val link = Regex("[\"']hls[\"']:\\s*[\"'](.*)[\"']").find(script ?: return)?.groupValues?.get(1)
            val videoLinks = mutableListOf<String>()

            if (!link.isNullOrBlank()) {
                videoLinks.add(
                    when {
                        linkRegex.matches(link) -> link
                        else -> String(Base64.decode(link, Base64.DEFAULT))
                    }
                )
            } else {            
                val link2 = base64Regex.find(script)?.value ?: return
                val decoded = Base64.decode(link2, Base64.DEFAULT).toString()
                val videoLinkDTO = AppUtils.parseJson<WcoSources>(decoded)
                videoLinkDTO.let { videoLinks.add(it.toString()) }
            }
            
            videoLinks.forEach { videoLink ->
                Log.d("mnemo", "voe.sx video link: ${videoLink}")
                M3u8Helper.generateM3u8(
                    name,
                    videoLink,
                    "$mainUrl/",
                    headers = mapOf("Origin" to "$mainUrl/")
                ).forEach(callback)
            }
        }
        else{
            Log.d("mnemo", "voe.sx redir not found")
        }
    }
	
	data class WcoSources(
        @JsonProperty("VideoLinkDTO") val VideoLinkDTO: String,
    )
}