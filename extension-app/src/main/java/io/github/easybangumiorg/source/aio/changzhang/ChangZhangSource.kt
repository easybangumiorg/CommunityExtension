package io.github.easybangumiorg.source.aio.changzhang

import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.extension_api.ExtensionIconSource
import com.heyanle.extension_api.ExtensionSource
import org.easybangumi.extension.R
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import kotlin.reflect.KClass

class ChangZhangSource:ExtensionSource(),ExtensionIconSource {
    override val describe: String
        get() = "厂长资源"
    override val label: String
        get() = describe
    override val sourceKey: String
        get() = "changzhang"
    override val version: String
        get() = "1.0"
    override val versionCode: Int
        get() = 1

    override fun getIconResourcesId(): Int= R.drawable.changzhang

    override fun register(): List<KClass<*>> = listOf(
        ChangZhangPage::class,
        ChangZhangDetailPage::class,
        ChangZhangSearchPage::class,
        ChangZhangPlayPage::class
    )

    companion object {
        const val BASE_URL = "https://www.czzy88.com"

        fun Element.parseAnime(source:String): CartoonCover {
            val url = selectFirst("a")!!.absUrl("href")
            val image = selectFirst("img")!!.let {
                it.dataset()["original"] ?: it.attr("src")
            }
            val ep = selectFirst(".jidi")?.text()?.trim()
            val title = selectFirst(".dytit")!!.text().trim()
            return CartoonCoverImpl(
                id = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.')),
                url = url,
                title = title,
                source = source,
                intro = ep,
                coverUrl = image
            )
        }

        fun Document.hasNextPage(): Boolean {
            val pageItems = select(".pagenavi_txt > a")
            if (pageItems.isEmpty()) {
                return false
            }
            return !pageItems.last()!!.hasClass("current")
        }
    }
}