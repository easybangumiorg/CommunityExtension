package io.github.easybangumiorg.source.aio.auete

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.detailed.DetailedComponent
import com.heyanle.easybangumi4.source_api.entity.Cartoon
import com.heyanle.easybangumi4.source_api.entity.CartoonImpl
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.map
import io.github.easybangumiorg.source.aio.newGetRequest
import io.github.easybangumiorg.source.aio.withIoResult

class AueteDetail : ComponentWrapper(), DetailedComponent {
    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> =
        withIoResult {
            val doc = commonHttpClient.newGetRequest {
                url("$AueteBaseUrl/${summary.id}/")
            }.asDocument()
            val container = doc.selectFirst(".main .card-thread > .card-body")!!
            val img = container.selectFirst(".cover img")?.absUrl("src") ?: ""
            val title =
                container.selectFirst(".media > .media-body > .title")!!.text().trim().let { str ->
                    val idx = str.indexOf('《')
                    if (idx == -1) {
                        str
                    } else {
                        var end = str.length
                        for (i in (idx + 1) until str.length) {
                            if (str[i] == '》') {
                                end = i
                                break
                            }
                        }
                        str.substring(idx + 1, end)
                    }
                }
            val infoList = mutableListOf<String>()
            val infoEls = container.select(".message > p")
            var desc = ""
            for ((index, p) in infoEls.withIndex()) {
                val text = p.text().trim()
                if (text.contains("简介")) {
                    if (index + 1 < infoEls.size) {
                        desc = infoEls[index + 1].text().trim()
                    }
                    break
                }
                infoList.add(text)

            }

            val playlists = container.select("[id=player_list]").map { playlistContainer ->
                var name = playlistContainer.selectFirst(".title")!!.text().trim()
                name.indexOf('』').let {
                    if (it >= 0) {
                        name = name.substring(it + 1)
                    }
                }
                name.indexOf('：').let {
                    if (it >= 0) {
                        name = name.substring(0, it)
                    }
                }
                val episodes = playlistContainer.select("ul > li > a").map { epEl ->
                    val id = epEl.attr("href").let {
                        it.substring(it.lastIndexOf('/') + 1, it.lastIndexOf('.'))
                    }
                    Episode(label = epEl.text().trim(), id = id, order = 0)
                }
                PlayLine(id = name, label = name, episode = arrayListOf(*episodes.toTypedArray()))
            }
            val cartoon = CartoonImpl(
                id = summary.id,
                source = source.key,
                url = "$AueteBaseUrl/${summary.id}/",
                title = title,
                coverUrl = img,
                description = desc,
                intro = infoList.joinToString(separator = "\n")
            )
            cartoon to playlists
        }

    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> =
        getAll(summary).map { it.first }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>> =
        getAll(summary).map { it.second }
}