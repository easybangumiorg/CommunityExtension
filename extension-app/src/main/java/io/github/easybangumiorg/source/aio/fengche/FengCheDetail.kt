package io.github.easybangumiorg.source.aio.fengche

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.detailed.DetailedComponent
import com.heyanle.easybangumi4.source_api.entity.Cartoon
import com.heyanle.easybangumi4.source_api.entity.CartoonImpl
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.withResult
//import com.heyanle.easybangumi4.source_api.withResult
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.map
import io.github.easybangumiorg.source.aio.newGetRequest
import kotlinx.coroutines.Dispatchers

class FengCheDetail (
    private val hostUrlHelper: FengCheHostUrlHelper
): ComponentWrapper(), DetailedComponent {
    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> =
        withResult(Dispatchers.IO) {
            val document = commonHttpClient.newGetRequest {
                url("${hostUrlHelper.fengcheBaseUrl}/video/${summary.id}.html")
            }.asDocument()
            val imageUrl =
                document.selectFirst(".con_c1 .img_wrapper")!!.dataset()["original"]!!
            val detailContainer = document.selectFirst(".con_xinxi")!!
            val title = detailContainer.selectFirst("a")!!.text().trim()
            val intro = detailContainer.selectFirst(".yplx_c1")!!.children()
                .find { it.text().contains("类型：") }
                ?.children()
                ?.lastOrNull()
                ?.text()
                ?.trim()
                ?: ""
            val desc = detailContainer.selectFirst(".yplx_c3")?.children()
                ?.lastOrNull()
                ?.text()
                ?.trim()
            val playlistNames = document.select(".playlist-tab a").map { it.text().trim() }
            val episodesGroup =
                document.select(".con_juji_bg > .tab-content > .con_c2_list").asSequence()
                    .map { epContainer ->
                        epContainer.select("a").map {
                            Episode(
                                id = it.attr("href").extractFengCheIdFromUrl(),
                                label = it.text().trim(),
                                order = 1
                            )
                        }
                    }
                    .toList()
            val playlists = List(playlistNames.size.coerceAtMost(episodesGroup.size)) {
                PlayLine(
                    id = it.toString(),
                    label = playlistNames[it],
                    episode = arrayListOf(*episodesGroup[it].toTypedArray())
                )
            }
            val cartoon = CartoonImpl(
                id = summary.id,
                source = source.key,
                url = "${hostUrlHelper.fengcheBaseUrl}/video/${summary.id}.html",
                title = title,
                coverUrl = imageUrl,
                description = desc,
                intro = intro
            )
            cartoon to playlists
        }

    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> {
        return getAll(summary).map { it.first }
    }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>> {
        return getAll(summary).map { it.second }
    }

}