package io.github.easybangumiorg.source.aio.xigua

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.withResult
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.encodeUri
import io.github.easybangumiorg.source.aio.newGetRequest
import kotlinx.coroutines.Dispatchers

class XiguaSearch : ComponentWrapper(), SearchComponent {
    override fun getFirstSearchKey(keyword: String): Int = 0

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> = withResult(Dispatchers.IO) {
        val doc = commonHttpClient.newGetRequest {
            url("$XiguaBaseUrl/search?q=${keyword.encodeUri()}")
        }.asDocument()
        val videos =
            doc.select("#layout > .container.search > .topic-list > .topic-list-box").map { box ->
                val url = box.selectFirst("a")!!.absUrl("href")
                val id = url.extractXiguaIdFromUrl()
                val tags =
                    box.select(".topic-tag .tag").asSequence().map { it.text() }.toMutableList()
                val infoList = box.selectFirst(".topic-list-item__info")!!.children()
                val name = infoList.last()!!.text()
                if (infoList.size > 1) {
                    tags.add(infoList[0].text())
                }
                CartoonCoverImpl(
                    id = id,
                    source = source.key,
                    url = url,
                    title = name,
                    intro = tags.joinToString(" "),
                    coverUrl = "https://static-a.xgcartoon.com/cover/$id.jpg"
                )
            }
        null to videos
    }
}