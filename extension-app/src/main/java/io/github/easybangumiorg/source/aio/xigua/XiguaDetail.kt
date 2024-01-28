package io.github.easybangumiorg.source.aio.xigua

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.detailed.DetailedComponent
import com.heyanle.easybangumi4.source_api.entity.Cartoon
import com.heyanle.easybangumi4.source_api.entity.CartoonImpl
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.withResult
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.map
import io.github.easybangumiorg.source.aio.newGetRequest
import kotlinx.coroutines.Dispatchers

class XiguaDetail : ComponentWrapper(), DetailedComponent {
    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> {
        return withResult(Dispatchers.IO) {

            val doc = commonHttpClient.newGetRequest {
                url("$XiguaBaseUrl/detail/${summary.id}")
            }.asDocument()
            val detailContainer = doc.selectFirst(".detail-right")!!
            val name = detailContainer.selectFirst(".detail-right__title")!!.text()
            val tags = detailContainer.select(".detail-right__tags .tag").map { it.text() }
            val desc =
                detailContainer.selectFirst(".detail-right__desc")?.children()?.last()?.text()
            val cartoon = CartoonImpl(
                id = summary.id,
                source = source.key,
                url = "$XiguaBaseUrl/detail/${summary.id}",
                title = name,
                genre = tags.joinToString(" "),
                coverUrl = "https://static-a.xgcartoon.com/cover/${summary.id}.jpg",
                description = desc
            )
            val playlists = mutableListOf<PlayLine>()
            var currentEpisodes = arrayListOf<Episode>()
            var playLineId = 0
            detailContainer.selectFirst(".detail-right__volumes")?.lastElementChild()?.children()
                ?.forEach { element ->
                    if (element.hasClass("volume-title")) {
                        currentEpisodes = arrayListOf()
                        playlists.add(
                            PlayLine(
                                (playLineId++).toString(),
                                label = element.text(),
                                currentEpisodes
                            )
                        )
                    } else {
                        val linkEl = element.selectFirst("a")!!
                        currentEpisodes.add(
                            Episode(
                                id = linkEl.attr("href").let {
                                    it.substring(it.lastIndexOf('=') + 1)
                                },
                                label = linkEl.text(),
                                order = 0
                            )
                        )
                    }
                }
            cartoon to playlists
        }

    }

    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> {
        return getAll(summary).map { it.first }
    }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>> {
        return getAll(summary).map { it.second }
    }
}