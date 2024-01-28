package io.github.easybangumiorg.source.aio.fengche

import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.withResult
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.encodeUri
import io.github.easybangumiorg.source.aio.newGetRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FengChePage : ComponentWrapper(), PageComponent {

    private val categoryLock = Mutex()

    private var categories: List<VideoCategory>? = null

    override fun getPages(): List<SourcePage> {
        val home = SourcePage.Group("首页", false) {
            withResult(Dispatchers.IO) {
                val document = commonHttpClient.newGetRequest {
                    url(FengCheBaseUrl)
                }.asDocument()
                val pages = mutableListOf<SourcePage.SingleCartoonPage>()
                document.selectFirst(".flickity-slider")?.let { container ->
                    val videos =
                        container.getElementsByClass("myui-vodlist__box")
                            .map { it.parseFengCheAnime(source.key) }
                    if (videos.isNotEmpty()) {
                        val page = SourcePage.SingleCartoonPage.WithCover("热门推荐", { 0 }) {
                            withResult {
                                null to videos
                            }
                        }
                        pages.add(page)
                    }
                }
                document.getElementsByClass("myui-panel").forEach { panelEl ->
                    val videos =
                        panelEl.getElementsByClass("myui-vodlist__box")
                            .map { it.parseFengCheAnime(source.key) }
                    if (videos.isNotEmpty()) {
                        val page = SourcePage.SingleCartoonPage.WithCover(
                            panelEl.selectFirst(".title")!!.text(), { 0 }) {
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
        val pageUrl = "$FengCheBaseUrl/show/$typeKey---${category.encodeUri()}-----$page---.html"
        val document = commonHttpClient.newGetRequest {
            url(pageUrl)
        }.asDocument()
        val videos = document.getElementsByClass("myui-vodlist__box")
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
        val document = commonHttpClient.newGetRequest {
            url("$FengCheBaseUrl/type/ribendongman.html")
        }.asDocument()
        val row =
            document.select(".container .row .myui-panel_bd ul.myui-screen__list.nav-slide")[1]
        val ignoreKey = "ribendongman"
        val linkList = row.getElementsByTag("a")
        val valueIndex = linkList.last()!!.attr("href").run {
            substring(lastIndexOf('/') + 1, lastIndexOf('.'))
        }
            .split('-')
            .indexOfFirst { it.isNotEmpty() && it != ignoreKey }
        // 忽略第一个
        val categories = List(linkList.size - 1) {
            val el = linkList[it + 1]
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