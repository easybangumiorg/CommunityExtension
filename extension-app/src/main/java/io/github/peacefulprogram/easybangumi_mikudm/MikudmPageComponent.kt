package io.github.peacefulprogram.easybangumi_mikudm

import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.NetworkHelper
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class MikudmPageComponent(
    private val okhttpHelper: OkhttpHelper
) : ComponentWrapper(), PageComponent {
    override fun getPages(): List<SourcePage> {
        val pages = mutableListOf<SourcePage>()
        val homePage = SourcePage.Group("首页", false) {
            withResult(Dispatchers.IO) {
                parseHomePage(Jsoup.parse(MikudmUtil.getDocument(okhttpHelper, "/"), MikudmUtil.BASE_URL))
            }
        }
        pages.add(homePage)

        listOf(
            22 to "新番",
            20 to "完结"
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


    private fun buildPageOfType(typeId: Int, page: Int): Pair<Int?, List<CartoonCover>> {
        val document = Jsoup.parse(
            MikudmUtil.getDocument(okhttpHelper, "/index.php/vod/type/id/$typeId/page/$page.html"),
            MikudmUtil.BASE_URL
        )
        val videos = document.select(".vodlist_item").map { it.parseToCartoon() }
        val nextPage =
            if (MikudmUtil.hasNextPage(document) && videos.isNotEmpty()) page + 1 else null
        return nextPage to videos
    }

    private fun parseHomePage(document: Document): List<SourcePage.SingleCartoonPage> {
        val result = mutableListOf<SourcePage.SingleCartoonPage>()
        document.select(".vod_row .pannel").forEach { videoGroupContainer ->
            val groupTitle =
                videoGroupContainer.selectFirst(".title")!!.textNodes().last().text().trim()
            if (groupTitle.contains("福利")) {
                return@forEach
            }
            val videos = videoGroupContainer.select(".vodlist_item").map { it.parseToCartoon() }
            val page = SourcePage.SingleCartoonPage.WithCover(groupTitle, { 0 }) {
                withResult {
                    null to videos
                }
            }
            result.add(page)
        }
        return result
    }

    private fun Element.parseToCartoon(): CartoonCover {
        val linkEl = this.selectFirst("a")!!
        val url = linkEl.absUrl("href")
        val coverUrl = MikudmUtil.extractImageSrc(linkEl)
        val title = this.selectFirst(".vodlist_title")!!.text().trim()
        val episode = this.selectFirst(".pic_text")?.text()?.trim()
        return CartoonCoverImpl(
            id = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.')),
            source = source.key,
            url = url,
            title = title,
            intro = episode,
            coverUrl = coverUrl
        )
    }
}