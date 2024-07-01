package com.example

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.extractorApis
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.MixDrop
import com.fasterxml.jackson.annotation.JsonProperty
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class SflixProvider(val plugin: SflixPlugin) : MainAPI() { 
    // all providers must be an instance of MainAPI
    override var mainUrl = "https://sflix.to" 
    override var name = "Sflix"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)
    override var lang = "en"

    // enable this when your provider has a main page
    override val hasMainPage = true

    // this function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        val escaped = query.replace(' ', '-')
        val url = "$mainUrl/search/${escaped}"

        return app.get(
            url,
        ).document.select(".flw-item").mapNotNull { article ->
            val name = article.selectFirst("h2 > a")?.text() ?: ""
            val poster = article.selectFirst("img")?.attr("data-src")
            val url = article.selectFirst("a.btn")?.attr("href") ?: ""
            val type = article.selectFirst("strong")?.text() ?: ""

            if (type == "Movie") {
                newMovieSearchResponse(name, url) {
                    posterUrl = poster
                }
            }
            else {
                newTvSeriesSearchResponse(name, url) {
                    posterUrl = poster
                }
            }
        }
    }


    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val html = app.get("$mainUrl/home").text
        val document = Jsoup.parse(html)
        val all = ArrayList<HomePageList>()

        val map = mapOf(
            "Trending Movies" to "div#trending-movies",
            "Trending TV Shows" to "div#trending-tv",
        )
        map.forEach {
            all.add(HomePageList(
                it.key,
                document.select(it.value).select("div.flw-item").map { element ->
                    element.toSearchResult()
                }
            ))
        }

        document.select("section.block_area.block_area_home.section-id-02").forEach {
            val title = it.select("h2.cat-heading").text().trim()
            val elements = it.select("div.flw-item").map { element ->
                element.toSearchResult()
            }
            all.add(HomePageList(title, elements))
        }

        return HomePageResponse(all)
    }


    private fun Element.toSearchResult(): SearchResponse {
        val inner = this.selectFirst("div.film-poster")
        val img = inner!!.select("img")
        val title = img.attr("title")
        val posterUrl = img.attr("data-src") ?: img.attr("src")
        val href = fixUrl(inner.select("a").attr("href"))
        val isMovie = href.contains("/movie/")
        val otherInfo =
            this.selectFirst("div.film-detail > div.fd-infor")?.select("span")?.toList() ?: listOf()
        //var rating: Int? = null
        var year: Int? = null
        var quality: SearchQuality? = null
        when (otherInfo.size) {
            1 -> {
                year = otherInfo[0]?.text()?.trim()?.toIntOrNull()
            }
            2 -> {
                year = otherInfo[0]?.text()?.trim()?.toIntOrNull()
            }
            3 -> {
                //rating = otherInfo[0]?.text()?.toRatingInt()
                quality = getQualityFromString(otherInfo[1]?.text())
                year = otherInfo[2]?.text()?.trim()?.toIntOrNull()
            }
        }

        return if (isMovie) {
            MovieSearchResponse(
                title,
                href,
                "Sflix.to",
                TvType.Movie,
                posterUrl = posterUrl,
                year = year,
                quality = quality,
            )
        } else {
            TvSeriesSearchResponse(
                title,
                href,
                "Sflix.to",
                TvType.Movie,
                posterUrl,
                year = year,
                episodes = null,
                quality = quality,
            )
        }
    }






    // start with movie easier than tv series
    // this function only displays info about movies and series
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val posterUrl = document.selectFirst("img.film-poster-img")?.attr("src")
        val details = document.select("div.detail_page-watch")
        val img = details.select("img.film-poster-img")
        val title = img.attr("title") ?: throw ErrorLoadingException("No Title")
        val plot = details.select("div.description").text().replace("Overview:", "").trim()
        val rating = document.selectFirst(".fs-item > .imdb")?.text()?.trim()?.removePrefix("IMDB:")?.toRatingInt()
        val isMovie = url.contains("/movie/")

        val idRegex = Regex(""".*-(\d+)""")
        val dataId = details.attr("data-id")
        val id = if (dataId.isNullOrEmpty())
            idRegex.find(url)?.groupValues?.get(1)
                ?: throw ErrorLoadingException("Unable to get id from '$url'")
        else dataId

        // duration  duration = duration ?: element.ownText().trim()
        // Casts cast = element.select("a").mapNotNull { it.text() }
        // genre
        // country
        // production
        // tags = element.select("a").mapNotNull { it.text() }

        if (isMovie){
            val episodesUrl = "$mainUrl/ajax/episode/list/$id"
            val episodes = app.get(episodesUrl).text

            val sourceIds: List<String> = Jsoup.parse(episodes).select("a").mapNotNull { element ->
                var sourceId: String? = element.attr("data-id")

                if (sourceId.isNullOrEmpty())
                    sourceId = element.attr("data-linkid")

                Log.d("mnemo", "sourceId: $sourceId, type: ${sourceId?.javaClass?.name}")
                if (sourceId.isNullOrEmpty()) {
                    null
                }
                else{
                    "$mainUrl/ajax/episode/sources/$sourceId"
                }
            }
            Log.d("mnemo", sourceIds.toString());        
            val comingSoon = sourceIds.isEmpty()

            return newMovieLoadResponse(title, url, TvType.Movie, sourceIds) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.comingSoon = comingSoon
                this.rating = rating
                // this.year = year
                // addDuration(duration)
                // addActors(cast)
                // this.tags = tags
                // this.recommendations = recommendations
                // addTrailer(youtubeTrailer)
            }

        }
        else{
            // TV series
            val seasonsDocument = app.get("$mainUrl/ajax/v2/tv/seasons/$id").document
            val episodes = arrayListOf<Episode>()
            var seasonItems = seasonsDocument.select("div.dropdown-menu.dropdown-menu-model > a")
            if (seasonItems.isNullOrEmpty())
                seasonItems = seasonsDocument.select("div.dropdown-menu > a.dropdown-item")
            seasonItems.apmapIndexed { season, element ->
                val seasonId = element.attr("data-id")
                if (seasonId.isNullOrBlank()) return@apmapIndexed

                var episode = 0
                val seasonEpisodes = app.get("$mainUrl/ajax/v2/season/episodes/$seasonId").document
                var seasonEpisodesItems =
                    seasonEpisodes.select("div.flw-item.film_single-item.episode-item.eps-item")
                if (seasonEpisodesItems.isNullOrEmpty()) {
                    seasonEpisodesItems =
                        seasonEpisodes.select("ul > li > a")
                }
                seasonEpisodesItems.forEach {
                    val episodeImg = it?.select("img")
                    val episodeTitle = episodeImg?.attr("title") ?: it.ownText()
                    val episodePosterUrl = episodeImg?.attr("src")
                    val episodeData = it.attr("data-id") ?: return@forEach

                    episode++

                    val episodeNum =
                        (it.select("div.episode-number").text()
                            ?: episodeTitle).let { str ->
                            Regex("""\d+""").find(str)?.groupValues?.firstOrNull()
                                ?.toIntOrNull()
                        } ?: episode

                    episodes.add(
                        newEpisode(Pair(url, episodeData)) {
                            this.posterUrl = fixUrlNull(episodePosterUrl)
                            this.name = episodeTitle?.removePrefix("Episode $episodeNum: ")
                            this.season = season + 1
                            this.episode = episodeNum
                        }
                    )
                }
            }

            // Log.d("mnemo", episodes.toString());   
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.rating = rating
                // this.year = year
                // addDuration(duration)
                // addActors(cast)
                // this.tags = tags
                // this.recommendations = recommendations
                // addTrailer(youtubeTrailer)
            }
        }
    }


    // this function loads the links (upcloud/vidcloud/doodstream/other)
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("mnemo", "LOADLINKS")
        Log.d("mnemo", data)

        val dataList = data
            .replace("[", "")
            .replace("]", "")
            .replace("\"", "")
            .split(",")

        val links = dataList.mapNotNull { url ->
            var jsonData = app.get(url).parsed<SrcJSON>()
            // {"type":"iframe","link":"https://example/e/xxxxx","sources":[],"tracks":[],"title":""}

            if (jsonData.link.startsWith("https://dood.watch")){
                // Cloudflare bypass
                // dood.re need a correct DNS resolver != ISP
                var replacedLink = jsonData.link.replace("dood.watch", "dood.re")
                Log.d("mnemo", "Handling dood ${replacedLink}")

                // Extract the link and display the content
                if (!loadExtractor(replacedLink, subtitleCallback, callback)){
                    Log.d("mnemo", "Couldn't extract dood")
                }
            }
            else if (jsonData.link.startsWith("https://rabbitstream")){
                // rabbitstream api player-v2-e4 is not compatible yet
                Log.d("mnemo", "Handling rabbit ${jsonData.link}")
                if (!loadExtractor(jsonData.link, subtitleCallback, callback)){
                    Log.d("mnemo", "Couldn't extract rabbit/vidcloud/upcloud")
                }
            }
            else{
                Log.d("mnemo", "Handling ${jsonData.link}")
                if (!loadExtractor(jsonData.link, subtitleCallback, callback)){
                    Log.d("mnemo", "Couldn't extract other provider")
                }
            }
        }

        return true
    }

    data class SrcJSON(
        @JsonProperty("type") val type: String = "",
        @JsonProperty("link") val link: String = "",
        @JsonProperty("sources") val sources: ArrayList<String> = arrayListOf(),
        @JsonProperty("tracks") val tracks: ArrayList<String> = arrayListOf(),
        @JsonProperty("title") val title: String = "",
    )
}

class DoodRe : DoodLaExtractor() {
    override var mainUrl = "https://dood.re"
}

class MixDropCo : MixDrop(){
    override var mainUrl = "https://mixdrop.co"
}