package io.github.easybangumiorg.source.aio.xigua

import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.withResult
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.encodeUri
import io.github.easybangumiorg.source.aio.newGetRequest
import io.github.easybangumiorg.source.aio.readJson
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class XiGuaPage : ComponentWrapper(), PageComponent {

    override fun getPages(): List<SourcePage> {
        val home = SourcePage.Group(label = "首页", newScreen = false) {
            withResult(Dispatchers.IO) {
                val doc = commonHttpClient.newGetRequest { url(XiguaBaseUrl) }.asDocument()
                val result = mutableListOf<SourcePage.SingleCartoonPage>()
                doc.select("#index .index-hot").forEach { hot ->
                    val title = hot.child(0).text()
                    val videos = hot.select(".index-hot-item > .box").map { box ->
                        val linkEl = box.selectFirst(".title")!!
                        val url = linkEl.absUrl("href")
                        val name = linkEl.text().trim()
                        val id = url.extractXiguaIdFromUrl()
                        CartoonCoverImpl(
                            id = id,
                            source = source.key,
                            url = url,
                            title = name,
                            intro = box.selectFirst(".info")?.text(),
                            coverUrl = "https://static-a.xgcartoon.com/cover/$id.jpg"
                        )
                    }
                    if (videos.isNotEmpty()) {
                        val page = SourcePage.SingleCartoonPage.WithCover(
                            label = title,
                            firstKey = { 0 }) {
                            withResult {
                                null to videos
                            }
                        }
                        result.add(page)
                    }


                }
                val recommendPages =
                    doc.select("#index > .index-category > .catelog").map { category ->
                        val videos = mutableListOf<CartoonCover>()
                        val title = category.selectFirst(".head")!!.text()
                        category.selectFirst(".top-item")?.let { item ->
                            val linkEl = item.selectFirst("a")!!
                            val url = linkEl.absUrl("href")
                            val name = item.selectFirst(".info > .title")!!.text()
                            val id = url.extractXiguaIdFromUrl()
                            CartoonCoverImpl(
                                id = id,
                                source = source.key,
                                url = url,
                                title = name,
                                intro = item.selectFirst(".info > .author")?.text(),
                                coverUrl = "https://static-a.xgcartoon.com/cover/$id.jpg"
                            ).run {
                                videos.add(this)
                            }
                        }
                        category.select(".list > a").forEach { linkEl ->
                            val url = linkEl.absUrl("href")
                            val infoEls = linkEl.selectFirst(".topic-info")!!.children()
                            val id = url.extractXiguaIdFromUrl()
                            CartoonCoverImpl(
                                id = id,
                                source = source.key,
                                url = url,
                                title = infoEls[0].text().trim(),
                                intro = infoEls.getOrNull(1)?.text(),
                                coverUrl = "https://static-a.xgcartoon.com/cover/$id.jpg"
                            ).run {
                                videos.add(this)
                            }
                        }
                        SourcePage.SingleCartoonPage.WithCover(label = title, firstKey = { 0 }) {
                            withResult {
                                null to videos
                            }
                        }
                    }
                result.addAll(recommendPages)
                result
            }

        }

        val categoryPage = SourcePage.Group("分类导航", false) {
            withResult(Dispatchers.IO) {
                val categoryDoc = commonHttpClient.newGetRequest {
                    url("$XiguaBaseUrl/classify?type=")
                }.asDocument()
                categoryDoc.select("#classify .filter-type > .filter-item > a")
                    .asSequence()
                    .map { linkEl ->
                        val url = linkEl.attr("href")
                        val paramNameAndValue =
                            url.substring(url.indexOf('?') + 1, url.indexOf('&')).split('=')
                        linkEl.text().trim() to (paramNameAndValue.getOrNull(1) ?: "")
                    }
                    .filter { it.second.isNotEmpty() }
                    .map { (pageLabel, filterType) ->
                        SourcePage.SingleCartoonPage.WithCover(pageLabel, { 1 }) { page ->
                            withResult(Dispatchers.IO) {
                                queryVideoOfCategory(filterType, page)
                            }
                        }
                    }
                    .toList()
            }
        }
        return listOf(home, categoryPage)
    }

    private fun queryVideoOfCategory(category: String, page: Int): Pair<Int?, List<CartoonCover>> {
        val star = "*".encodeUri()
        val resp = commonHttpClient.newGetRequest {
            url("$XiguaBaseUrl/api/amp_query_cartoon_list?type=$category&region=$star&filter=$star&page=$page&limit=36&language=cn&__amp_source_origin=${XiguaBaseUrl.encodeUri()}")
        }
            .readJson<CategoryResponse>()
        val videos = resp.items.map { item ->
            CartoonCoverImpl(
                id = item.id,
                source = source.key,
                url = "$XiguaBaseUrl/detail/${item.id}",
                title = item.name,
                intro = (listOf(item.regionName, item.author) + item.typeNames).asSequence()
                    .filter { it.isNotEmpty() }.joinToString(separator = " "),
                coverUrl = item.topicImg.takeIf { it.isNotEmpty() }
                    ?.let { "https://static-a.xgcartoon.com/cover/${item.topicImg}" }
            )
        }
        val nextPage = if (resp.next.isNotEmpty()) page + 1 else null
        return nextPage to videos
    }


    @Serializable
    data class CategoryResponse(
        val items: List<CategoryVideoItem> = emptyList(),
        val next: String = ""
    )

    @Serializable
    data class CategoryVideoItem(
        @SerialName("cartoon_id")
        val id: String,
        val name: String,
        val region: String = "",
        val author: String = "",
        @SerialName("region_name")
        val regionName: String = "",
        @SerialName("topic_img")
        val topicImg: String = "",
        @SerialName("type_names")
        val typeNames: List<String> = emptyList(),
    )
}