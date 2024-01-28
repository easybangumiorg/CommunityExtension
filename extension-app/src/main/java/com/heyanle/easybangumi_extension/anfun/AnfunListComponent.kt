package com.heyanle.easybangumi_extension.anfun

import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.core.SourceUtils
import org.jsoup.select.Elements

/**
 * Created by heyanle on 2024/1/28.
 * https://github.com/heyanLE
 */
class AnfunListComponent(
    private val okhttpHelper: OkhttpHelper,
) : ComponentWrapper() {

    companion object {
        const val ROOT_URL = "https://www.anfuns.cc"
    }

    suspend fun listPage(
        element: Elements,
    ): Pair<Int?, List<CartoonCover>> {
        val r = arrayListOf<CartoonCover>()
        for (video in element) {
            video.apply {
                val name = select("a").attr("title")
                val videoUrl = select("a").attr("href")
                val coverUrl = select("a").attr("data-original")
                val episode = select(".remarks").text()
                val id = videoUrl.subSequence(7, videoUrl.length - 5).toString()
                if (!name.isNullOrBlank() && !videoUrl.isNullOrBlank() && !coverUrl.isNullOrBlank()) {
                    val b = CartoonCoverImpl(
                        id = id,
                        source = source.key,
                        url = videoUrl,
                        title = name,
                        intro = episode ?: "",
                        coverUrl = SourceUtils.urlParser(ROOT_URL, coverUrl)
                    )
                    r.add(b)
                }
            }
        }
        return Pair(null, r)
    }

}