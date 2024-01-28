package io.github.easybangumiorg.source.aio.auete

//import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.encodeUri
import io.github.easybangumiorg.source.aio.newGetRequest
import io.github.easybangumiorg.source.aio.withIoResult

class AueteSearch : ComponentWrapper(), SearchComponent {
    override fun getFirstSearchKey(keyword: String): Int = 1

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> = withIoResult {
        val doc = commonHttpClient.newGetRequest {
            url("$AueteBaseUrl/auete3so.php?searchword=${keyword.encodeUri()}")
        }.asDocument()
        val videos = doc.getElementsByClass("threadlist").map { el ->
            val linkEl = el.selectFirst("a")!!
            CartoonCoverImpl(
                id = linkEl.attr("href").trim('/'),
                url = linkEl.absUrl("href"),
                title = linkEl.text().trim(),
                source = source.key,
            )
        }
        null to videos
    }
}