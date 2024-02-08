package io.github.easybangumiorg.source.aio.changzhang

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.changzhang.ChangZhangSource.Companion.hasNextPage
import io.github.easybangumiorg.source.aio.changzhang.ChangZhangSource.Companion.parseAnime
import io.github.easybangumiorg.source.aio.newGetRequest
import io.github.easybangumiorg.source.aio.withIoResult

class ChangZhangPage(private val okhttpHelper: OkhttpHelper):PageComponent,ComponentWrapper() {
    override fun getPages(): List<SourcePage> {
       val homePage = SourcePage.Group("首页",newScreen = false){
            withIoResult {
                val doc = okhttpHelper.cloudflareClient.newGetRequest { url(ChangZhangSource.BASE_URL) }.asDocument()
                doc.getElementsByClass("mi_btcon").asSequence().map { groupEl ->
                    val name = groupEl.selectFirst(".bt_tit a")?.text()?.trim()?:"推荐"
                    val videos = groupEl.select(".bt_img > ul > li").map { it.parseAnime(source = source.key) }
                    name to videos
                }
                    .filter { it.second.isNotEmpty() }
                    .map { (label,videos)->
                        SourcePage.SingleCartoonPage.WithCover(label = label, firstKey = {0}){
                            SourceResult.Complete(null to videos)
                        }
                    }
                    .toList()
            }
        }

        val movie = SourcePage.Group("电影",newScreen = false){
            withIoResult {
                val doc = okhttpHelper.cloudflareClient.newGetRequest { url("${ChangZhangSource.BASE_URL}/movie_bt") }.asDocument()
                doc.select("#beautiful-taxonomy-filters-tax-movie_bt_tags > a").asSequence()
                    .map {
                        it.text() to (it.attr("cat-url").takeIf { it.isNotEmpty() }?: it.attr("href"))
                    }
                    .map { (label,url)->
                        SourcePage.SingleCartoonPage.WithCover(label = label, firstKey = {1}){page->
                            withIoResult { fetchVideoCategoryPage(url,page) }
                        }
                    }
                    .toList()
            }
        }
        val otherPages = listOf(
            "美剧" to "meijutt",
            "日剧" to "riju",
            "韩剧" to "hanjutv",
            "番剧" to "fanju",
            "电视剧" to "dsj",
            "国产剧" to "gcj",
            "剧场版" to "dongmanjuchangban",
            "海外剧" to "haiwaijuqita",
            "热映中" to "benyueremen"
        ).map { (label,urlSuffix)->
            SourcePage.SingleCartoonPage.WithCover(label = label, firstKey = {1}){page->
                withIoResult {
                    fetchVideoCategoryPage("${ChangZhangSource.BASE_URL}/$urlSuffix",page)
                }
            }
        }
        return  listOf(homePage,movie)+otherPages
    }

    private fun fetchVideoCategoryPage(url:String,page:Int): Pair<Int?,List<CartoonCover>> {
        val actualUrl = if (page>1) "$url/page/$page" else url
        val doc = okhttpHelper.cloudflareClient.newGetRequest { url(actualUrl) }.asDocument()
        val videos = doc.select(".bt_img > ul > li").map { it.parseAnime(source = source.key) }
        val next = if (doc.hasNextPage()) page +1 else null
        return next to videos
    }
}