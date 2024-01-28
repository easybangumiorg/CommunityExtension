package com.heyanle.easybangumi_extension.anfun

import android.util.Log
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.core.SourceUtils
import com.heyanle.easybangumi4.source_api.utils.core.network.GET
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import org.jsoup.select.Elements

/**
 * Created by heyanle on 2024/1/29.
 * https://github.com/heyanLE
 */
class AnfunSearchComponent(
    private val okhttpHelper: OkhttpHelper
) : ComponentWrapper(), SearchComponent {
    override fun getFirstSearchKey(keyword: String): Int {
        return 0
    }

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> {
        return withResult(Dispatchers.IO) {
            val url = SourceUtils.urlParser(
                AnfunSource.ROOT_URL,
                "/search/page/${pageKey + 1}/wd/${keyword}.html"
            )
            Log.e("TAG", "--->${url}")
            val d = okhttpHelper.cloudflareWebViewClient.newCall(
                GET(
                    SourceUtils.urlParser(
                        AnfunSource.ROOT_URL,
                        url
                    )
                )
            ).execute().body?.string()!!
            val doc = Jsoup.parse(d)
            val r = arrayListOf<CartoonCover>()
            val lpic = doc.select("#conch-content").select(".row").select("ul")[0]
            val results: Elements = lpic.select("li")
            if (results.size == 0) {
                Log.e("TAG", "已经加载完毕～")
                return@withResult Pair(null, r)
            }
            for (i in results.indices) {
                var cover = results[i].select("a").attr("data-original")
                if (cover.startsWith("//")) {
                    cover = "https:${cover}"
                }
                val title = results[i].select("a").attr("title")
                val itemUrl = results[i].select("a").attr("href")
                val id = itemUrl.subSequence(7, itemUrl.length - 5).toString()
                val episode = results[i].select(".hl-pic-text").select("span").text()
                val describe =
                    results[i].select("p[class='hl-item-sub hl-text-muted hl-lc-2']").text()

                val b = CartoonCoverImpl(
                    id = id,
                    title = title,
                    url = itemUrl,
                    intro = episode,
                    coverUrl = SourceUtils.urlParser(AnfunSource.ROOT_URL, cover),
                    source = source.key,
                )
                r.add(b)
            }
            val pages = doc.select(".hl-list-wrap").select(".hl-page-wrap").select("li")
            return@withResult if (pages.isEmpty()) {
                Pair(null, r)
            } else {
                var hasNext = false
                for (p in pages) {
                    if (p.text() == (pageKey + 2).toString() || p.text() == "下一页") {
                        hasNext = true
                        break
                    }
                }
                if (!hasNext) {
                    Pair(null, r)
                } else {
                    Pair(pageKey + 1, r)
                }
            }
        }
    }
}