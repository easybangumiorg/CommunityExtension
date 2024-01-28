package com.heyanle.easybangumi_extension.anfun

import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.core.SourceUtils
import com.heyanle.easybangumi4.source_api.utils.core.network.GET
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup

/**
 * Created by heyanle on 2024/1/28.
 * https://github.com/heyanLE
 */
class AnfunPageComponent(
    private val anfunListComponent: AnfunListComponent,
    private val okhttpHelper: OkhttpHelper,
): ComponentWrapper(), PageComponent {

    override fun getPages(): List<SourcePage> {
        return listOf(
            // 首页
            SourcePage.Group(
                "首页",
                false,
            ) {
                withResult(Dispatchers.IO) {
                    homeListPages()
                }
            },

            // 新番时刻表
            SourcePage.Group(
                "每日更新列表",
                false,
            ) {
                withResult(Dispatchers.IO) {
                    homeTimelinePages()
                }
            },
        )
    }

    // 获取主页所有 ListPage
    private suspend fun homeListPages(): List<SourcePage.SingleCartoonPage> {
        val res = arrayListOf<SourcePage.SingleCartoonPage>()
        val doc = Jsoup.parse(
            okhttpHelper.cloudflareWebViewClient.newCall(GET(AnfunSource.ROOT_URL))
                .execute().body?.string()!!
        )

        val modules = doc.select("#conch-content").select("div[class='container']")
        for (em in modules){
            val moduleHeading = em.select(".hl-rb-head").first()
            val type = moduleHeading?.select(".hl-rb-title")
            val label = type?.text()?:continue
            if (label == "每周更新" || label == "网络资讯" || label == "动漫专题") continue
            val lis = em.select(".row").select("ul").select("li")
            val page = SourcePage.SingleCartoonPage.WithCover(
                label = label,
                firstKey = { 0 },
            ) {
                withResult(Dispatchers.IO) {
                    anfunListComponent.listPage(
                        lis
                    )
                }
            }
            res.add(page)
        }
        return res
    }

    // 获取新番时刻表 ListPage
    private suspend fun homeTimelinePages(): List<SourcePage.SingleCartoonPage> {
        val res = arrayListOf<SourcePage.SingleCartoonPage>()
        val docmuent = Jsoup.parse(
            okhttpHelper.cloudflareWebViewClient.newCall(GET(AnfunSource.ROOT_URL))
                .execute().body?.string()!!
        )
        val doc = docmuent.select("#conch-content").select("div[class='container']")[2]
        val days = mutableListOf<String>()
        doc.select(".hl-rb-head").select("span").select("a").forEach {
//            Log.e("星期", it.text())
            days.add(it.text())
        }
        val updateList = doc.select(".row").select(".hl-list-wrap")
        for ((index,ems) in updateList.withIndex()){
            val r = arrayListOf<CartoonCover>()
            val target = ems.select("ul").select("li")
            for (em in target) {
                val titleEm = em.select("a")
                val cover = titleEm.attr("data-original")
                val title = titleEm.attr("title")
                val episode = titleEm.select(".remarks").text()
                val url = titleEm.attr("href")
                val id = url.subSequence(7, url.length - 5).toString()
                val desc = em.select(".hl-item-sub").text()
                if (!title.isNullOrBlank() && !episode.isNullOrBlank() && !url.isNullOrBlank()) {
                    val car = CartoonCoverImpl(
                        id = id,
                        source = source.key,
                        url = url,
                        title = title,
                        intro = episode,
                        coverUrl = null,
                    )
                    r.add(car)
                }
            }
            res.add(
                SourcePage.SingleCartoonPage.WithoutCover(
                    days[index],
                    firstKey = { 0 },
                    load = {
                        withResult {
                            null to r
                        }
                    }
                ))
            if (index == 6) break
        }
        return res
    }
}