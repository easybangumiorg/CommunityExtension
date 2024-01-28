package io.github.easybangumiorg.source.aio.fengche

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.withResult
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.encodeUri
import io.github.easybangumiorg.source.aio.newGetRequest
import kotlinx.coroutines.Dispatchers

class FengCheSearch : ComponentWrapper(), SearchComponent {

    override fun getFirstSearchKey(keyword: String): Int = 1

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> = withResult(Dispatchers.IO) {
        val document = commonHttpClient.newGetRequest {
            url("$FengCheBaseUrl/search/${keyword.encodeUri()}----------$pageKey---.html")
        }.asDocument()
        val videos =
            document.getElementById("searchList")?.children()
                ?.map { it.parseFengCheAnime(source.key) }
                ?: emptyList()
        val nextPage = if (document.hasNextPage()) pageKey + 1 else null
        nextPage to videos
    }

}