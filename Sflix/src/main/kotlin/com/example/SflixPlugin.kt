package com.example

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.extractorApis
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

@CloudstreamPlugin
class SflixPlugin: Plugin() {
    var activity: AppCompatActivity? = null

    override fun load(context: Context) {
        activity = context as AppCompatActivity
        // All providers should be added in this manner
        registerExtractorAPI(DoodRe())
        registerExtractorAPI(MixDropCo())
        
        // Force this extractor to be first in the list
        // Use this when you rewrite/upgrade an existing extractor
        addExtractor(Upstream())
        addExtractor(Voe2())

        registerMainAPI(ExampleProvider(this))

        openSettings = { ctx ->
            val frag = BlankFragment(this)
            frag.show(activity!!.supportFragmentManager, "Frag")
        }
    }


    private fun addExtractor(element: ExtractorApi) {
        element.sourcePlugin = __filename
        extractorApis.add(0, element)
    }
}