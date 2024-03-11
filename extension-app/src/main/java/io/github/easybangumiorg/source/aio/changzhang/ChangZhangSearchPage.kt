package io.github.easybangumiorg.source.aio.changzhang

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.changzhang.ChangZhangSource.Companion.hasNextPage
import io.github.easybangumiorg.source.aio.changzhang.ChangZhangSource.Companion.parseAnime
import io.github.easybangumiorg.source.aio.encodeUri
import io.github.easybangumiorg.source.aio.newGetRequest
import io.github.easybangumiorg.source.aio.newRequest
import io.github.easybangumiorg.source.aio.withIoResult
import okhttp3.Cookie
import okhttp3.FormBody

class ChangZhangSearchPage(private val okhttpHelper: OkhttpHelper):SearchComponent,ComponentWrapper() {

    var searchUrl:String? = null

    override fun getFirstSearchKey(keyword: String): Int {
        return 1
    }

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> = withIoResult{
        if (searchUrl == null) {
            searchUrl = okhttpHelper.client.newGetRequest {
                url(ChangZhangSource.BASE_URL)
            }
                .asDocument()
                .selectFirst(".w-search-form")
                ?.attr("action")
                ?: throw RuntimeException("未获取到搜索视频链接")
        }
        val doc = okhttpHelper.cloudflareClient.newGetRequest {
            url("$searchUrl?q=${keyword.encodeUri()}&f=_all&p=$pageKey")
        }.use { resp ->
            val respDoc = resp.asDocument()
            if (respDoc.title().contains("人机验证")) {
                val value = Cookie.parseAll(resp.request.url, resp.headers)
                    .lastOrNull { it.name == "result" }?.value
                    ?: throw RuntimeException("Cookie中无验证码")
                okhttpHelper.cloudflareClient.newRequest {
                    url(resp.request.url)
                    post(FormBody.Builder().add("result", value).build())
                }
                okhttpHelper.cloudflareClient.newGetRequest { url(resp.request.url) }.asDocument()
            } else {
                respDoc
            }
        }
        val videos = doc.select(".search_list > ul > li").map { it.parseAnime(source = source.key) }
        val nextPage = if (doc.hasNextPage()) pageKey + 1 else null
        nextPage to videos
    }
}