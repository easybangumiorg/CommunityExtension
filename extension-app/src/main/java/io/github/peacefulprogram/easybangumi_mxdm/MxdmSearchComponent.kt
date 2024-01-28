package io.github.peacefulprogram.easybangumi_mxdm

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import java.net.URLEncoder

class MxdmSearchComponent(
    private val okhttpHelper: OkhttpHelper
) : ComponentWrapper(), SearchComponent {
    override fun getFirstSearchKey(keyword: String): Int = 1

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> = withResult(Dispatchers.IO) {
        val document =
            Jsoup.parse(
                MxdmUtil.getDocument(okhttpHelper, "/search/${encodeUrlComponent(keyword)}----------$pageKey---.html"),
                MxdmUtil.BASE_URL
            )
        val videos = document.select(".module-search-item").map { videoElement ->
            val link = videoElement.selectFirst("a")!!
            val url = link.absUrl("href")
            val videoTitle = videoElement.selectFirst("h3")!!.text()
            val coverUrl = MxdmUtil.extractImageSrc(videoElement.selectFirst("img")!!)
            val desc = videoElement.select(".video-info-item").last()?.text()
            val videoId = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'))
            CartoonCoverImpl(
                id = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.')),
                source = source.key,
                url = "${MxdmUtil.BASE_URL}/dongman/$videoId.html",
                title = videoTitle,
                intro = desc,
                coverUrl = coverUrl
            )
        }
        val nextPage =
            if (MxdmUtil.hasNextPage(document) && videos.isNotEmpty()) pageKey + 1 else null
        nextPage to videos
    }

    private fun encodeUrlComponent(text: String): String =
        URLEncoder.encode(text, Charsets.UTF_8.name())
}