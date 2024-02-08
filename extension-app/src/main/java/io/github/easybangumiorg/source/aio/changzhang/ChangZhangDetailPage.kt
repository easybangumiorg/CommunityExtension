package io.github.easybangumiorg.source.aio.changzhang

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.detailed.DetailedComponent
import com.heyanle.easybangumi4.source_api.entity.Cartoon
import com.heyanle.easybangumi4.source_api.entity.CartoonImpl
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.map
import io.github.easybangumiorg.source.aio.newGetRequest
import io.github.easybangumiorg.source.aio.withIoResult

class ChangZhangDetailPage(private val okhttpHelper: OkhttpHelper):DetailedComponent,ComponentWrapper() {
    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> =
        withIoResult{
            val detailPageUrl = "${ChangZhangSource.BASE_URL}/movie/${summary.id}.html"
        val doc = okhttpHelper.cloudflareClient.newGetRequest {
            url(detailPageUrl)
        }.asDocument()
        val episodes = doc.select(".paly_list_btn > a").map { epEl ->
            val url = epEl.attr("href")
            Episode(
                id = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.')),
                label = epEl.text().trim(),
                order = 0
            )
        }
        val container = doc.selectFirst(".dyxingq")!!
        val image = container.selectFirst(".dyimg > img")!!.let {
            it.dataset()["original"] ?: it.attr("src")
        }
        val title = container.selectFirst(".dytext > .moviedteail_tt > h1")!!.text()
        val infoList = container.select(".dytext > .moviedteail_list > li").map { it.text().trim() }
        val desc = doc.selectFirst(".yp_context")?.text()?.trim()
            CartoonImpl(
                id = summary.id,
                source = source.key,
                url = detailPageUrl,
                title = title,
                intro = infoList.joinToString(separator = "\n"),
                coverUrl = image,
                description = desc
            ) to listOf(PlayLine(id = "1", label = "播放列表", episode = arrayListOf(*episodes.toTypedArray())))
    }

    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> = getAll(summary).map { it.first }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>>  = getAll(summary).map { it.second }
}