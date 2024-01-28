package io.github.easybangumiorg.source.aio.auete

import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.withResult
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.newGetRequest
import io.github.easybangumiorg.source.aio.withIoResult

class AuetePage : ComponentWrapper(), PageComponent {
    override fun getPages(): List<SourcePage> {
        val home = SourcePage.Group(label = "首页", newScreen = false) {
            withIoResult {
                val doc = commonHttpClient.newGetRequest {
                    url(AueteBaseUrl)
                }.asDocument()
                val areas: MutableList<SourcePage.SingleCartoonPage> =
                    doc.select(".container > .row > .main .card").map { area ->
                        val areaName = area.firstElementChild()!!.selectFirst("a")!!.text().trim()
                        val videos =
                            area.select(".threadlist > li").map { it.parseAueteVideo(source.key) }
                        SourcePage.SingleCartoonPage.WithCover(label = areaName, firstKey = { 0 }) {
                            withResult {
                                null to videos
                            }
                        }
                    }.toMutableList()
                doc.selectFirst(".card-site-info")
                    ?.parent()
                    ?.children()
                    ?.asSequence()
                    ?.forEach {
                        val title = it.selectFirst(".card-header")?.text()?.trim() ?: return@forEach
                        val videos = it.select(".card-body > .list-ul > li > a").map { videoEl ->
                            CartoonCoverImpl(
                                id = videoEl.attr("href").trim('/'),
                                source = source.key,
                                url = videoEl.absUrl("href"),
                                title = videoEl.text().trim()
                            )
                        }
                        if (videos.isEmpty()) {
                            return@forEach
                        }
                        SourcePage.SingleCartoonPage.WithoutCover(label = title, firstKey = { 0 }) {
                            withResult {
                                null to videos
                            }
                        }.let { page -> areas.add(page) }
                    }

                areas
            }
        }
        val others = getVideoCategories().map { (pageLabel, pageKey, subTypes) ->
            SourcePage.Group(label = pageLabel, newScreen = false) {
                withResult {
                    subTypes.map { (typeName, typeKey) ->
                        SourcePage.SingleCartoonPage.WithCover(
                            label = typeName,
                            firstKey = { 1 }) { page ->
                            withIoResult {
                                getVideoOfCategoryAndType(
                                    category = pageKey,
                                    type = typeKey,
                                    page = page
                                )
                            }
                        }
                    }
                }
            }
        }
        return listOf(home) + others
    }

    private fun getVideoOfCategoryAndType(
        category: String,
        type: String,
        page: Int
    ): Pair<Int?, List<CartoonCover>> {
        val urlBuilder = StringBuilder(AueteBaseUrl)
            .append('/')
            .append(category)
        if (type.isNotEmpty()) {
            urlBuilder.append('/')
                .append(type)
        }
        urlBuilder.append("/index")
        if (page > 1) {
            urlBuilder.append(page)
        }
        urlBuilder.append(".html")
        val doc = commonHttpClient.newGetRequest {
            url(urlBuilder.toString())
        }.asDocument()
        val videos = doc.select(".threadlist > li").map { it.parseAueteVideo(source = source.key) }
        val nextPage = if (doc.aueteHaveNextPage()) page + 1 else null
        return nextPage to videos

    }

    private fun getVideoCategories() = listOf(
        Triple(
            "电影",
            "Movie",
            listOf(
                "全部" to "",
                "喜剧片" to "xjp",
                "动作片" to "dzp",
                "爱情片" to "aqp",
                "科幻片" to "khp",
                "恐怖片" to "kbp",
                "惊悚片" to "jsp",
                "战争片" to "zzp",
                "剧情片" to "jqp"
            )
        ),
        Triple(
            "电视剧",
            "Tv",
            listOf(
                "全部" to "",
                "美剧" to "oumei",
                "韩剧" to "hanju",
                "日剧" to "riju",
                "泰剧" to "yataiju",
                "网剧" to "wangju",
                "台剧" to "taiju",
                "国产" to "neidi",
                "港剧" to "tvbgj",
                "英剧" to "yingju",
                "外剧" to "waiju"
            )
        ),
        Triple(
            "综艺",
            "Zy",
            listOf(
                "全部" to "",
                "国综" to "guozong",
                "韩综" to "hanzong",
                "美综" to "meizong"
            )
        ),
        Triple(
            "动漫",
            "Dm",
            listOf(
                "全部" to "",
                "动画" to "donghua",
                "日漫" to "riman",
                "国漫" to "guoman",
                "美漫" to "meiman"
            )
        ),
        Triple(
            "其他",
            "qita",
            listOf(
                "全部" to "",
                "记录片" to "Jlp",
                "经典片" to "Jdp",
                "经典剧" to "Jdj",
                "网大电影" to "wlp",
                "国产老电影" to "laodianying"
            )
        )
    )
}