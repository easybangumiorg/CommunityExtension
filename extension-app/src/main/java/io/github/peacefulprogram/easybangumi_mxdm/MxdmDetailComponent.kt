package io.github.peacefulprogram.easybangumi_mxdm

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.detailed.DetailedComponent
import com.heyanle.easybangumi4.source_api.entity.Cartoon
import com.heyanle.easybangumi4.source_api.entity.CartoonImpl
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup

class MxdmDetailComponent(
    private val okhttpHelper: OkhttpHelper
) : ComponentWrapper(), DetailedComponent {
    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> =
        withResult(Dispatchers.IO) {
            getVideoDetail(summary.id)
        }

    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> =
        withResult(Dispatchers.IO) {
            getVideoDetail(summary.id).first
        }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>> =
        withResult(Dispatchers.IO) {
            getVideoDetail(summary.id).second
        }

    private fun getVideoDetail(videoId: String): Pair<Cartoon, List<PlayLine>> {
        val document =
            Jsoup.parse(MxdmUtil.getDocument(okhttpHelper,"/dongman/$videoId.html"), MxdmUtil.BASE_URL)
        val videoTitle = document.selectFirst(".page-title")!!.text().trim()
        val tags = document.select(".video-info-aux a").joinToString(", ") { it.text().trim() }
        val desc = document.selectFirst(".video-info-content")?.text()
        val coverUrl = document.selectFirst(".module-item-pic img")?.run {
            MxdmUtil.extractImageSrc(this)
        }
        val cartoon = CartoonImpl(
            id = videoId,
            source = source.key,
            url = "${MxdmUtil.BASE_URL}/dongman/$videoId.html",
            title = videoTitle,
            genre = tags,
            coverUrl = coverUrl,
            description = desc
        )
        val playlistNames = document.select(".module-player-tab .module-tab-item").map { el ->
            if (el.childrenSize() > 0) {
                el.child(0).text().trim()
            } else {
                el.text().trim()
            }
        }
        val playLineList = mutableListOf<PlayLine>()
        document.select(".module-player-list > .module-blocklist")
            .forEachIndexed { index, container ->
                if (index >= playlistNames.size) {
                    return@forEachIndexed
                }
                val episodeElements = container.getElementsByTag("a")
                if (episodeElements.isEmpty()) {
                    return@forEachIndexed
                }
                val playlistId = episodeElements[0].attr("href").split('-').run {
                    this[size - 2]
                }
                val episodeNames = container.getElementsByTag("a").map { it.text().trim() }
                val ep = arrayListOf<Episode>()
                for (s in episodeNames.indices) {
                    val d = episodeNames[s]
                    ep.add(Episode(s.toString(), d, s))
                }
                playLineList.add(
                    PlayLine(
                        playlistId,
                        playlistNames[index],
                        ep
                    )
                )
            }

        return cartoon to playLineList
    }
}