package io.github.easybangumiorg.source.aio.fengche

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

class FengCheSearch(
    private val hostUrlHelper: FengCheHostUrlHelper
) : ComponentWrapper(), SearchComponent {

    override fun getFirstSearchKey(keyword: String): Int = 1

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> = withResult(Dispatchers.IO) {
        val document = commonHttpClient.newGetRequest {
            url("${hostUrlHelper.fengcheBaseUrl}/search/${keyword.encodeUri()}----------$pageKey---.html")
        }.asDocument()
        val videos =
            document.select(".sear_con > .reusltbox").map { box->
                    val link = box.selectFirst("a")!!.absUrl("href")
                    val img = box.selectFirst(".img_wrapper")?.dataset()?.get("original")
                    val title = box.selectFirst(".result_title > a")!!.text().trim()
                    CartoonCoverImpl(
                        id = link.extractFengCheIdFromUrl(),
                        source = source.key,
                        url = link,
                        title = title,
                        coverUrl = img
                    )
                }
        val nextPage = if (document.hasNextPage() && videos.isNotEmpty()) pageKey + 1 else null
        nextPage to videos
    }

}