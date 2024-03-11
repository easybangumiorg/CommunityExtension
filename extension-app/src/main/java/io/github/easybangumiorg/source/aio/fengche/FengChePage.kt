package io.github.easybangumiorg.source.aio.fengche

import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.withResult
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.encodeUri
import io.github.easybangumiorg.source.aio.newGetRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FengChePage(val okhttpHelper: OkhttpHelper) : ComponentWrapper(), PageComponent {

    private val categoryLock = Mutex()

    private var categories: List<VideoCategory>? = null

    override fun getPages(): List<SourcePage> {
        val home = SourcePage.Group("首页", false) {
            withResult(Dispatchers.IO) {
                val document = okhttpHelper.client.newGetRequest {
                    url(FengCheBaseUrl)
                }.asDocument()
                val pages = mutableListOf<SourcePage.SingleCartoonPage>()
                document.select("body > div.wrapper").forEach { wrapperEl ->
                    val videoEls = wrapperEl.select(".picList > li")
                        .takeIf { it.isNotEmpty() }
                        ?: wrapperEl.select(".c2_list > li")
                    val title = wrapperEl.selectFirst(".cont_title")
                        ?: wrapperEl.selectFirst(".title > .t_head")
                    title ?: return@forEach
                    val videos = videoEls.map { it.parseFengCheAnime(source.key)}
                    if (videos.isNotEmpty()) {
                        val page =
                            SourcePage.SingleCartoonPage.WithCover(title.text().trim(), { 0 }) {
                                withResult {
                                    null to videos
                                }
                            }
                        pages.add(page)
                    }
                }
                pages
            }
        }
        val categoryPages = listOf(
            "ribendongman" to "日本动漫",
            "guochandongman" to "国产动漫",
            "dongmandianying" to "动漫电影",
            "oumeidongman" to "欧美动漫",
        ).map { (typeKey, label) ->
            SourcePage.Group(label = label, false) {
                withResult(Dispatchers.IO) {
                    val categories = getVideoCategories()
                    categories.map { category ->
                        SourcePage.SingleCartoonPage.WithCover(
                            category.label,
                            firstKey = { 1 }) { page ->
                            withResult(Dispatchers.IO) {
                                requestVideoOfCategory(typeKey, category.value, page)
                            }
                        }
                    }
                }
            }
        }
        return listOf(home) + categoryPages
    }

    private fun requestVideoOfCategory(
        typeKey: String,
        category: String,
        page: Int
    ): Pair<Int?, List<CartoonCover>> {
        val pageUrl = "$FengCheBaseUrl/show/$typeKey---$category-----$page---.html"
        val document = okhttpHelper.client.newGetRequest {
            url(pageUrl)
        }.asDocument()
        val videos = document.select(".wrapper > .culum_con > .c2_list > li")
            .map { it.parseFengCheAnime(source.key) }
        val nextPage = if (document.hasNextPage()) page + 1 else null
        return nextPage to videos
    }


    private suspend fun getVideoCategories(): List<VideoCategory> {
        if (categories == null) {
            categoryLock.withLock {
                if (categories == null) {
                    categories = requestVideoCategories()
                }
            }
        }
        return categories!!
    }

    private fun requestVideoCategories(): List<VideoCategory> {
        val document = okhttpHelper.client.newGetRequest {
            url("$FengCheBaseUrl/type/ribendongman.html")
        }.asDocument()
        val row =
            document.select("body > div.wrapper.culum_list > div.conx.star_bot > div > div")[1]
        val ignoreKey = "ribendongman"
        val linkList = row.getElementsByTag("a")
        val valueIndex = linkList.last()!!.attr("href").run {
            substring(lastIndexOf('/') + 1, lastIndexOf('.'))
        }
            .split('-')
            .indexOfFirst { it.isNotEmpty() && it != ignoreKey }
        // 忽略第一个
        val categories = linkList.map {el->
            val value = el.attr("href").run {
                substring(lastIndexOf('/') + 1, lastIndexOf('.'))
            }
                .split('-')[valueIndex]
            VideoCategory(label = el.text().trim(), value = value)
        }
        return categories
    }

    private data class VideoCategory(
        val label: String,
        val value: String
    )
}