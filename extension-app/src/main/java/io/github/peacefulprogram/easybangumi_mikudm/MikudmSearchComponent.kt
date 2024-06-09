package io.github.peacefulprogram.easybangumi_mikudm

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.api.WebViewHelper
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import java.net.URLEncoder

class MikudmSearchComponent(
    private val okhttpHelper: OkhttpHelper,
    private val mikudmUtil: MikudmUtil,
) : ComponentWrapper(), SearchComponent {
    override fun getFirstSearchKey(keyword: String): Int = 1

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> = withResult(Dispatchers.IO) {
        val document =
            Jsoup.parse(
                mikudmUtil.getDocument(
                    okhttpHelper, "/index.php/vod/search/page/$pageKey/wd/${
                        encodeUrlComponent(
                            keyword
                        )
                    }.html"
                ),
                mikudmUtil.BASE_URL
            )
        val videos = document.select(".searchlist_item").map { videoElement ->
            val link = videoElement.selectFirst(".vodlist_thumb")!!
            val url = link.absUrl("href")
            val videoTitle = videoElement.selectFirst(".vodlist_title>a")!!.attr("title")
            val coverUrl = mikudmUtil.extractImageSrc(link)
            CartoonCoverImpl(
                id = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.')),
                source = source.key,
                url = url,
                title = videoTitle,
                coverUrl = coverUrl
            )
        }
        val nextPage =
            if (mikudmUtil.hasNextPage(document) && videos.isNotEmpty()) pageKey + 1 else null
        nextPage to videos
    }

    private fun encodeUrlComponent(text: String): String =
        URLEncoder.encode(text, Charsets.UTF_8.name())
}