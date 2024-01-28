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

class FengCheDetail : ComponentWrapper(), DetailedComponent {
    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> =
        withResult(Dispatchers.IO) {
            val document = commonHttpClient.newGetRequest {
                url("$FengCheBaseUrl/video/${summary.id}.html")
            }.asDocument()
            val imageUrl =
                document.selectFirst(".myui-vodlist__thumb img")!!.dataset()["original"]!!
            val detailContainer = document.selectFirst(".myui-content__detail")!!
            val title = detailContainer.selectFirst(".title")!!.text().trim()
            val currentEpisode =
                document.selectFirst("div.myui-content__thumb  pic-text.text-right")?.text()?.trim()
                    ?: ""
            val infoList = detailContainer.getElementsByClass("data").map { it.text().trim() }.run {
                if (currentEpisode.isEmpty()) {
                    this
                } else {
                    listOf(currentEpisode) + this
                }
            }
            val desc = detailContainer.selectFirst(".desc .sketch")?.text()?.trim() ?: ""
            val playlistNames =
                document.selectFirst("ul.nav-tabs")?.children()?.map { it.text().trim() }
                    ?: emptyList()
            val episodesGroup = document.select(".tab-content > .tab-pane").asSequence()
                .filter { it.id().startsWith("playlist") }
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
                url = "$FengCheBaseUrl/video/${summary.id}.html",
                title = title,
                coverUrl = imageUrl,
                description = desc,
                intro = infoList.joinToString(" ")
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