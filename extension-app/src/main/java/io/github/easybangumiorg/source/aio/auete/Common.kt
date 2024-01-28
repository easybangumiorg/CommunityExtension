package io.github.easybangumiorg.source.aio.auete

import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

const val AueteBaseUrl = "https://auete.pro"

fun Element.parseAueteVideo(source: String): CartoonCover {
    val linkEl = selectFirst("a")!!
    val id = linkEl.attr("href").trim('/')
    val image = selectFirst("img")?.absUrl("src") ?: ""
    val episode = selectFirst(".hdtag")?.text()?.trim()
    val title = selectFirst(".title")!!.text().trim()
    return CartoonCoverImpl(
        id = id,
        source = source,
        url = linkEl.absUrl("href"),
        title = title,
        coverUrl = image,
        intro = episode
    )
}

fun Document.aueteHaveNextPage(): Boolean {
    val pageItems = select(".pagination > li")
    if (pageItems.isEmpty()) {
        return false
    }
    return pageItems.indexOfLast { it.hasClass("active") } < pageItems.size - 3
}