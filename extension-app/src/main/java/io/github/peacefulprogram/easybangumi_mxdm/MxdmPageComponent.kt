package io.github.peacefulprogram.easybangumi_mxdm


import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class MxdmPageComponent(
    private val okhttpHelper: OkhttpHelper
) : ComponentWrapper(), PageComponent {
    override fun getPages(): List<SourcePage> {
        val pages = mutableListOf<SourcePage>()
        val homePage = SourcePage.Group("首页", false) {
            withResult(Dispatchers.IO) {
                parseHomePage(Jsoup.parse(MxdmUtil.getDocument(okhttpHelper, "/"), MxdmUtil.BASE_URL))
            }
        }
        pages.add(homePage)
        val timeline = SourcePage.Group("更新时间表", false) {
            withResult(Dispatchers.IO) {
                parseTimeLine(Jsoup.parse(MxdmUtil.getDocument(okhttpHelper, "/"), MxdmUtil.BASE_URL))
            }
        }
        pages.add(timeline)

        listOf(
            "riman" to "日本动漫",
            "guoman" to "国产动漫",
            "dmdianying" to "动漫电影",
            "oman" to "欧美动漫",
        ).forEach { (typeId, typeName) ->
            val page = SourcePage.SingleCartoonPage.WithCover(typeName, { 1 }) { page ->
                withResult(Dispatchers.IO) {
                    buildPageOfType(typeId, page)
                }
            }
            pages.add(page)
        }

        return pages
    }

    private fun buildPageOfType(typeId: String, page: Int): Pair<Int?, List<CartoonCover>> {
        val document = Jsoup.parse(
            MxdmUtil.getDocument(okhttpHelper, "/show/${typeId}--------${page}---.html"),
            MxdmUtil.BASE_URL
        )
        val videos = document.select(".content .module .module-item").map { it.parseToCartoon() }
        val nextPage = if (MxdmUtil.hasNextPage(document) && videos.isNotEmpty()) page + 1 else null
        return nextPage to videos
    }

    private fun parseTimeLine(document: Document): List<SourcePage.SingleCartoonPage> {
        val tabs = document.selectFirst(".mxoneweek-tabs") ?: return emptyList()
        val result = mutableListOf<SourcePage.SingleCartoonPage>()
        var activeTabIndex = 0
        val tabNames = tabs.children().mapIndexed { index, el ->
            if (el.hasClass("active")) {
                activeTabIndex = index
            }
            el.text().trim()
        }
        val videoGroups = document.select(".mxoneweek-list").map { el ->
            el.getElementsByTag("a").map { link ->
                val title =
                    if (link.childrenSize() > 0) link.child(0).text().trim() else link.text().trim()
                val episodeText = if (link.childrenSize() > 1) link.child(1).text().trim() else ""
                val url = link.absUrl("href")
                val videoId = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'))
                CartoonCoverImpl(
                    id = videoId,
                    source = source.key,
                    url = "${MxdmUtil.BASE_URL}/dongman/$videoId.html",
                    title = title,
                    intro = episodeText
                )
            }

        }
        for (i in activeTabIndex until tabNames.size.coerceAtMost(videoGroups.size)) {
            val page = SourcePage.SingleCartoonPage.WithoutCover(tabNames[i], { 0 }) {
                withResult {
                    null to videoGroups[i]
                }
            }
            result.add(page)
        }
        for (i in 0 until activeTabIndex) {
            val page = SourcePage.SingleCartoonPage.WithoutCover(tabNames[i], { 0 }) {
                withResult {
                    null to videoGroups[i]
                }
            }
            result.add(page)
        }
        return result
    }

    private fun parseHomePage(document: Document): List<SourcePage.SingleCartoonPage> {
        val result = mutableListOf<SourcePage.SingleCartoonPage>()
        val contents = document.select(".content .module .module-list>.module-items").iterator()
        val titles = document.select(".content .module .module-title").iterator()
        while (contents.hasNext() && titles.hasNext()) {
            val contentEl = contents.next()
            val titleEl = titles.next()
            val videos = contentEl.select(".module-item")
            if (videos.isEmpty()) {
                continue
            }
            if (videos[0].classNames().size > 1) {
                continue
            }
            val cartoonList = videos.map { videoEl -> videoEl.parseToCartoon() }
            val page = SourcePage.SingleCartoonPage.WithCover(titleEl.text().trim(), { 0 }) {
                withResult {
                    Pair(null, cartoonList)
                }
            }
            result.add(page)
        }
        return result
    }

    private fun Element.parseToCartoon(): CartoonCover {
        val coverUrl = MxdmUtil.extractImageSrc(this.selectFirst("img")!!)
        val linkEl = this.selectFirst("a")!!
        val url = linkEl.absUrl("href")
        val id = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'))
        val videoTitle = this.selectFirst(".video-name")!!.text().trim()
        val tags = LinkedHashSet<String>()
        val episode = this.selectFirst(".module-item-text")?.text()?.trim() ?: ""
        if (episode.isEmpty()) {
            tags.add(episode)
        }
        this.selectFirst(".module-item-caption")?.children()?.forEach {
            val text = it.text().trim()
            if (text.isNotEmpty()) {
                tags.add(text)
            }
        }
        return CartoonCoverImpl(
            id = id,
            source = source.key,
            url = url,
            title = videoTitle,
            intro = tags.joinToString(" | "),
            coverUrl = coverUrl
        )
    }
}