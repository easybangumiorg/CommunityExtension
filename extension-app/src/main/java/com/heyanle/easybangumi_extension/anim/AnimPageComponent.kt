package com.heyanle.easybangumi_extension.anim

import android.util.Log
import com.google.gson.JsonParser
import com.heyanle.easybangumi4.source_api.ParserException
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.detailed.DetailedComponent
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.component.play.PlayComponent
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.entity.Cartoon
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonImpl
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.entity.PlayerInfo
import com.heyanle.easybangumi4.source_api.utils.api.NetworkHelper
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.core.network.GET
import com.heyanle.easybangumi4.source_api.utils.core.network.POST
import com.heyanle.easybangumi4.source_api.withResult

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.lang.NullPointerException
import java.lang.StringBuilder
import java.net.URLDecoder

/**
 * Created by HeYanLe on 2023/6/6 18:34.
 * https://github.com/heyanLE
 */
class AnimPageComponent(
    private val okhttpHelper: OkhttpHelper,
    private val networkHelper: NetworkHelper,
) : ComponentWrapper(), PageComponent,
    SearchComponent, PlayComponent, DetailedComponent {

    override fun getPages(): List<SourcePage> {

        return PageComponent.NonLabelSinglePage(
            SourcePage.SingleCartoonPage.WithoutCover(
                label = "",
                firstKey = { 1 },
                load = {
                    withResult {
                        null to DataSource.getData(true, okhttpHelper).map {
                            it.toCartoonCover(source)
                        }
                    }

                }
            )
        )
    }

    override fun getFirstSearchKey(keyword: String): Int {
        return 1
    }

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> {
        return withResult {
            null to DataSource.getData(false, okhttpHelper).filter {
                it.name.contains(keyword) || it.intro.contains(keyword) || it.year.contains(
                    keyword
                ) || it.season.contains(keyword) || it.translator.contains(keyword)
            }.map {
                it.toCartoonCover(source)
            }
        }
    }

    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> {
        return withResult(Dispatchers.IO) {
            Log.e("AnimPage", "https://anime1.me/?cat=${summary.id}")
            val resp = okhttpHelper.client.newCall(GET("https://anime1.me/?cat=${summary.id}"))
                .execute()
            if(!resp.isSuccessful){
                throw ParserException("网络错误")
            }
            val res = resp.body?.string() ?: ""
            val doc = Jsoup.parse(res)
            getDetailed(doc, summary) to DetailedComponent.NonPlayLine(
                getPlayLine(doc, summary)
            )
        }
    }

    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> {
        return withResult(Dispatchers.IO) {
            val resp = okhttpHelper.client.newCall(GET("https://anime1.me/?cat=${summary.id}"))
                .execute()
            if(!resp.isSuccessful){
                throw ParserException("网络错误")
            }
            val res = resp.body?.string() ?: ""

            val doc = Jsoup.parse(res)
            getDetailed(doc, summary)
        }
    }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>> {
        return withResult(Dispatchers.IO) {

            val resp = okhttpHelper.client.newCall(GET("https://anime1.me/?cat=${summary.id}"))
                .execute()
            if(!resp.isSuccessful){
                throw ParserException("网络错误")
            }
            val res = resp.body?.string() ?: ""
            val doc = Jsoup.parse(res)
            DetailedComponent.NonPlayLine(
                getPlayLine(doc, summary)
            )
        }
    }

    fun getDetailed(document: Document, summary: CartoonSummary): Cartoon {
        return CartoonImpl(
            id = summary.id,
            source = summary.source,
            url = "https://anime1.me/?cat=${summary.id}",
            title = document.select("#content header h1.page-title").text(),
        )
    }

    fun getPlayLine(document: Document, summary: CartoonSummary): PlayLine {
        val allTit = document.select("#content header h1.page-title").text()
        val episodeList = arrayListOf<Episode>()
        val dd = document.select("div.content-area main.site-main article")
        playUrlTemp.clear()
        Log.e("AnimPage", dd.toString())
        dd.forEachIndexed { index, it ->
            Log.e("AnimPage", it.toString())
            var title = it.select(".entry-header h2").first()?.text()
            if (title?.startsWith(allTit) == true) {
                title = title.substringAfter(allTit)
            }

            val dataApi = it.select("div.entry-content video").first()?.attr("data-apireq")
            if (title != null && dataApi != null) {
                val episode = Episode(index.toString(), title, index )
                episodeList.add(episode)
                playUrlTemp[episode.id] = dataApi
            }
        }
        lastCartoonSummary = summary
        return PlayLine(id = "", "", episodeList)
    }

    private var lastCartoonSummary: CartoonSummary? = null
    private val playUrlTemp = hashMapOf<String, String>()

    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episode: Episode,
    ): SourceResult<PlayerInfo> {
        var token = playUrlTemp[episode.id]
        if(lastCartoonSummary != summary || playUrlTemp.isEmpty() || token == null){
            getPlayLine(summary)
        }
        token = playUrlTemp[episode.id] ?: return SourceResult.Error(ParserException("token is null"), true)
        var cookie = ""
        return withResult(Dispatchers.IO) {

            var isMp4 = true
            val url = if(token.startsWith("%7B%22")){
                val builder = FormBody.Builder()
                builder.add("d", URLDecoder.decode(token, "utf-8"))
                val resp = okhttpHelper.client.newCall(POST(
                    "https://v.anime1.me/api",
                    headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded", "User-Agent", networkHelper.randomUA),
                    body = builder.build()
                ))
                    .execute()
                val cookieBuilder = StringBuilder()
                resp.headers("Set-Cookie").map {

                    val d = it.split(";").first().split("=")
                    var name = d[0]
                    var value = d[1]
                    cookieBuilder.append(name).append("=").append(value).append("; ")

                }
                if(cookieBuilder.endsWith("; ")){
                    cookieBuilder.setLength(cookieBuilder.length - 2)
                }
                cookie = cookieBuilder.toString()
                val res = resp.body?.string() ?: ""

                val p = JsonParser.parseString(res).asJsonObject
                val o = p.get("s").asJsonArray.get(0).asJsonObject
                val url = o.get("src").asString
                val type = o.get("type").asString
                if(type!="video/mp4"){
                    isMp4 = false
                }
                if(url.startsWith("//")) "https:${url}" else url
            }else{
                token
            }

            Log.e("AnimPage", url + "  " + cookie)
            PlayerInfo(uri = url, decodeType = if(isMp4)PlayerInfo.DECODE_TYPE_OTHER else PlayerInfo.DECODE_TYPE_HLS).apply{
                header = hashMapOf<String, String>("Cookie" to cookie.replace("HttpOnly", ""))
            }
        }
    }
}