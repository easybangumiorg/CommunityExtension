package io.github.easybangumiorg.source.aio.libvio

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.detailed.DetailedComponent
import com.heyanle.easybangumi4.source_api.entity.Cartoon
import com.heyanle.easybangumi4.source_api.entity.CartoonImpl
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.map
import io.github.easybangumiorg.source.aio.withIoResult

class LibVioDetail(private val libVioSource: LibVioSource) : ComponentWrapper(), DetailedComponent {
    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> =
        withIoResult {
            val doc = libVioSource.requestWithFunCDNInterceptor {
                url("${LibVioSource.BASE_URL}/detail/${summary.id}.html")
                get()
            }.asDocument()
            val playlists = mutableListOf<PlayLine>()
            for (playlistEl in doc.select(".stui-vodlist__head > .stui-content__playlist")) {
                val name =
                    playlistEl.previousElementSibling()?.takeIf { it.hasClass("stui-pannel__head") }
                        ?.text()?.trim() ?: continue
                if (name.contains("下载") || name.contains("网盘") || name.contains("云盘")) {
                    continue
                }
                val episodes = playlistEl.select("a").map { el ->
                    Episode(
                        label = el.text(),
                        order = 0,
                        id = el.attr("href")
                            .let { it.substring(it.lastIndexOf('/') + 1, it.lastIndexOf('.')) }
                    )
                }
                if (episodes.isEmpty()) {
                    continue
                }
                playlists.add(
                    PlayLine(
                        id = name,
                        label = name,
                        episode = arrayListOf(*episodes.toTypedArray())
                    )
                )
            }

            val detailContainer =
                doc.selectFirst(".stui-content") ?: throw RuntimeException("未找到视频详情")
            val img = detailContainer.selectFirst("img")?.dataset()?.get("original") ?: ""
            val title = detailContainer.selectFirst(".stui-content__detail > .title")!!.text()
            val infos = detailContainer.select(".stui-content__detail > .data").map { it.text() }
            val desc =
                detailContainer.selectFirst(".stui-content__detail > .desc > .detail-content")
                    ?.text()
                    ?: ""
            val cartoon = CartoonImpl(
                id = summary.id,
                source = libVioSource.key,
                title = title,
                coverUrl = img,
                intro = infos.joinToString(separator = "\n"),
                description = desc,
                url = "${LibVioSource.BASE_URL}/detail/${summary.id}.html"
            )
            cartoon to playlists
        }

    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> =
        getAll(summary).map { it.first }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>> =
        getAll(summary).map { it.second }

}