package io.github.peacefulprogram.easybangumi_mikudm

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

class MikudmDetailComponent(
    private val okhttpHelper: OkhttpHelper,
    private val mikudmUtil: MikudmUtil,
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
            Jsoup.parse(
                mikudmUtil.getDocument(okhttpHelper, "/index.php/vod/detail/id/$videoId.html"),
                mikudmUtil.BASE_URL
            )
        val detailContainer = document.selectFirst(".detail_list")!!
        val videoTitle = detailContainer.selectFirst(".content_detail .title")!!.text().trim()

        val desc = detailContainer.selectFirst(".desc")?.text()
        val coverUrl = detailContainer.selectFirst(".vodlist_thumb")?.run {
            mikudmUtil.extractImageSrc(this)
        }
        val cartoon = CartoonImpl(
            id = videoId,
            source = source.key,
            url = "${mikudmUtil.BASE_URL}/index.php/vod/detail/id/$videoId.html",
            title = videoTitle,
            coverUrl = coverUrl,
            description = desc
        )

        val episodeList = (document.select(".content_playlist").last()?.let { playlist ->
            playlist.getElementsByTag("a").map { it.text().trim() }
        } ?: emptyList())
        val ep = arrayListOf<Episode>()
        for (s in episodeList.indices) {
            val d = episodeList[s]
            ep.add(Episode(s.toString(), d, s))
        }

        return cartoon to DetailedComponent.NonPlayLine(
            PlayLine(
                id = "1",
                label = "异世界动漫",
                episode = ep
            )
        )
    }
}