package com.heyanle.easybangumi_extension.anfun

import android.util.Log
import com.heyanle.easybangumi4.source_api.ParserException
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.detailed.DetailedComponent
import com.heyanle.easybangumi4.source_api.component.play.PlayComponent
import com.heyanle.easybangumi4.source_api.component.update.UpdateComponent
import com.heyanle.easybangumi4.source_api.entity.Cartoon
import com.heyanle.easybangumi4.source_api.entity.CartoonImpl
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.entity.PlayerInfo
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.api.WebViewHelper
import com.heyanle.easybangumi4.source_api.utils.core.SourceUtils
import com.heyanle.easybangumi4.source_api.utils.core.network.GET
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Created by heyanle on 2024/1/29.
 * https://github.com/heyanLE
 */
class AnfunDetailedComponent(
    private val okhttpHelper: OkhttpHelper,
    private val webViewHelper: WebViewHelper
): ComponentWrapper(), DetailedComponent, UpdateComponent, PlayComponent {


    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> {
        return withResult(Dispatchers.IO) {
            detailed(getDoc(summary), summary)
        }
    }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>> {
        return withResult(Dispatchers.IO) {
            playLine(getDoc(summary), summary)
        }
    }

    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> {
        return withResult(Dispatchers.IO) {
            detailed(getDoc(summary), summary) to playLine(getDoc(summary), summary)
        }
    }

    override suspend fun update(
        cartoon: Cartoon,
        oldPlayLine: List<PlayLine>
    ): SourceResult<Cartoon> {
        return withResult(Dispatchers.IO) {

            when (val n = getAll(CartoonSummary(cartoon.id, cartoon.source))) {
                is SourceResult.Complete -> {
                    n.data.first.apply {

                        val newPlayLine = n.data.second

                        if (oldPlayLine.size != newPlayLine.size) {
                            isUpdate = true
                        } else {
                            isUpdate = false
                            for (i in oldPlayLine.indices) {
                                if (oldPlayLine[i].episode.size != newPlayLine[i].episode.size) {
                                    isUpdate = true
                                    break
                                }
                            }
                        }
                    }
                }
                is SourceResult.Error -> {
                    throw n.throwable
                }
            }
        }
    }

    private fun getDoc(summary: CartoonSummary): Document {
        val d = okhttpHelper.cloudflareWebViewClient.newCall(GET(SourceUtils.urlParser(AnfunSource.ROOT_URL, "/anime/${summary.id}.html")))
            .execute().body?.string() ?: throw NullPointerException()
        return Jsoup.parse(d)
    }
    private fun playLine(document: Document, summary: CartoonSummary): List<PlayLine> {
        Log.e("TAG","------->>>>>>>playLine")
        val res = arrayListOf<PlayLine>()
        val module = document.select(".hl-play-source").first() ?: return res
        val playNameList = module.select(".hl-plays-wrap").first()?.select("a") ?: return res
        val playEpisodeList = module.select(".hl-tabs-box")
        for (index in 0..playNameList.size) {
            val playName = playNameList.getOrNull(index)?.text()
            val playEpisode = playEpisodeList.getOrNull(index)
            if (playName != null && playEpisode != null) {
                val results = playEpisode.select("li").select("a")
                val es = arrayListOf<Episode>()

                for (i in results.indices) {
                    es.add(Episode((i+1).toString(), results[i].text(), i))  // title

                }
                val playLine = PlayLine(
                    id = (index + 1).toString(),
                    label = playName,
                    episode = es
                )
                res.add(playLine)
            }
        }
        return res
    }

    override suspend fun getPlayInfo(
        summary: CartoonSummary,
        playLine: PlayLine,
        episode: Episode
    ): SourceResult<PlayerInfo> {
        Log.e("TAG","------->>>>>>>开始播放")
        return withResult(Dispatchers.IO) {

//            Log.e("TAG","${playUrlTemp[playLine.id.toInt()]}") // [/play/632-1-1.html]
            val url = SourceUtils.urlParser(AnfunSource.ROOT_URL, "/play/${summary.id}-${playLine.id}-${episode.id}")
//            Log.e("TAG", url) // https://www.anfuns.cc/play/632-1-1.html
            var videoUrl = webViewHelper.interceptResource(
                url, regex = "https://www.anfuns.cc/vapi/AIRA/mui.php?.*"
            )
            Log.e("TAG", "地址：$videoUrl")
            if (videoUrl.isNotEmpty()) {
                when {
                    videoUrl.contains(".m3u8&") -> videoUrl = videoUrl.substringAfter("url=")
                        .substringBefore("&")
                    videoUrl.contains(".mp4") -> videoUrl = videoUrl.substringAfter("url=")
                        .substringBefore("&next=")
                }
                Log.e("TAG", "解析后url：$videoUrl")
                if (videoUrl.indexOf(".mp4") != -1){
                    PlayerInfo(
                        decodeType = PlayerInfo.DECODE_TYPE_OTHER,
                        uri = SourceUtils.urlParser(AnfunSource.ROOT_URL,videoUrl)
                    )
                }else{
                    PlayerInfo(
                        decodeType = PlayerInfo.DECODE_TYPE_HLS,
                        uri = SourceUtils.urlParser(AnfunSource.ROOT_URL,videoUrl)
                    )
                }
            }else{
                throw ParserException("Unknown")
            }
        }
    }

    private fun detailed(document: Document, summary: CartoonSummary): Cartoon {
        Log.e("TAG","------->>>>>>>detailed")

        var desc = ""
        var update = 0
        var status = 0

        val cover = document.select(".hl-dc-pic").select("span").attr("data-original")
        val title = document.select(".hl-dc-headwrap").select(".hl-dc-title").text()
        //document.select(".hl-dc-headwrap").select(".hl-dc-sub").text()
        // 更新状况
        val upStateItems = document.select(".hl-dc-content")
            .select(".hl-vod-data").select(".hl-full-box").select("ul").select("li")
        for (upStateEm in upStateItems){
            val t = upStateEm.text()
            when{
                t.contains("状态：") -> {
                    status =
                        if (t.startsWith("连载")) Cartoon.STATUS_ONGOING
                        else if (t.startsWith("全")) Cartoon.STATUS_COMPLETED
                        else Cartoon.STATUS_UNKNOWN
                    val isTheater = title.contains("剧场版")
                    update =
                        if (isTheater) {
                            if (status == Cartoon.STATUS_COMPLETED) {
                                Cartoon.UPDATE_STRATEGY_NEVER
                            } else {
                                Cartoon.UPDATE_STRATEGY_ONLY_STRICT
                            }
                        } else {
                            if (status == Cartoon.STATUS_COMPLETED) {
                                Cartoon.UPDATE_STRATEGY_ONLY_STRICT
                            } else {
                                Cartoon.UPDATE_STRATEGY_ALWAYS
                            }
                        }
                }
                t.contains("简介：") -> desc = t
            }
        }

        return CartoonImpl(
            id = summary.id,
            url = SourceUtils.urlParser(AnfunSource.ROOT_URL, "/anime/${summary.id}.html"),
            source = summary.source,

            title = title,
            coverUrl = cover,

            intro = "",
            description = desc,

            genre = "",

            status = status,
            updateStrategy = update,
        )
    }


}