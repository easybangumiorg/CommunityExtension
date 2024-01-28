package com.heyanle.easybangumi_extension.ggl

import android.util.Log
import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.core.SourceUtils
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers
import java.net.URLEncoder

/**
 * Created by heyanle on 2024/1/28.
 * https://github.com/heyanLE
 */
class GGLComponent(
    private val gglListComponent: GGLListComponent,
) : ComponentWrapper(), PageComponent, SearchComponent {


    override fun getPages(): List<SourcePage> {
        return listOf(
            SourcePage.SingleCartoonPage.WithCover(
                label = "日番",
                firstKey = { 1 },
            ) {
                withResult(Dispatchers.IO) {
                    gglListComponent.listHomePage(
                        "https://anime.girigirilove.com/show/2-----------/",
                        it
                    )
                }
            },
            SourcePage.SingleCartoonPage.WithCover(
                label = "美番",
                firstKey = { 1 },
            ) {
                withResult(Dispatchers.IO) {
                    gglListComponent.listHomePage(
                        "https://anime.girigirilove.com/show/3-----------/",
                        it
                    )
                }

            },
            SourcePage.SingleCartoonPage.WithCover(
                label = "剧场版",
                firstKey = { 1 },
            ) {
                withResult(Dispatchers.IO) {
                    gglListComponent.listHomePage(
                        "https://anime.girigirilove.com/show/21-----------/",
                        it
                    )
                }

            },
        )
    }

    override fun getFirstSearchKey(keyword: String): Int {
        return 1
    }

    override suspend fun search(
        pageKey: Int,
        keyword: String
    ): SourceResult<Pair<Int?, List<CartoonCover>>> {
        return withResult(Dispatchers.IO) {
            val url = SourceUtils.urlParser(
                GGLListComponent.ROOT_URL,
                "/search/${URLEncoder.encode(keyword, "utf-8")}----------${pageKey}---/"
            )
            val doc = gglListComponent.getDoc(url).getOrThrow()
            val list = arrayListOf<CartoonCover>()

            doc.select("div div.public-list-box.search-box").forEach {
                val uu = it.child(1).child(0).attr("href")
                val id = uu.subSequence(1, uu.length - 1).toString()

                val coverStyle = it.select("div.cover")[0].attr("style")
                val coverPattern = Regex("""(?<=url\().*(?=\))""")
                var cover = coverPattern.find(coverStyle)?.value ?: ""
                if (cover.startsWith("//")) {
                    cover = "http:${cover}"
                }
                Log.d("GGLSearchComponent", coverStyle)

                val title = it.select("div.thumb-content div.thumb-txt").first()?.text() ?: ""
                val b = CartoonCoverImpl(
                    id = id,
                    title = title,
                    url = SourceUtils.urlParser(GGLListComponent.ROOT_URL, uu),
                    intro = "",
                    coverUrl = SourceUtils.urlParser(GGLListComponent.ROOT_URL, cover),
                    source = source.key,
                )
                list.add(b)
            }

            (if (list.isEmpty()) null else pageKey + 1) to list


        }
    }
}