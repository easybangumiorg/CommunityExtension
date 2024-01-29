package io.github.easybangumiorg.source.aio.libvio

import android.content.Context
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.page.SourcePage
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.api.StringHelper
import com.heyanle.easybangumi4.source_api.withResult
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.withIoResult

class LibVioPage(
    private val libVioSource: LibVioSource,
    context: Context,
    okhttpHelper: OkhttpHelper,
    stringHelper: StringHelper
) : ComponentWrapper(), PageComponent {

    init {
        libVioSource.init(context, okhttpHelper, stringHelper)
    }

    override fun getPages(): List<SourcePage> {
        val pages =
            listOf("电影" to "1", "剧集" to "2", "动漫" to "4").map { (groupName, groupKey) ->
                SourcePage.Group(label = groupName, newScreen = false) {
                    withIoResult {
                        val doc = libVioSource.requestWithFunCDNInterceptor {
                            url("${LibVioSource.BASE_URL}/type/$groupKey.html")
                            get()
                        }.asDocument()
                        val categories =
                            listOf("全部" to "") + doc.getElementById("screenbox")!!.child(0)
                                .children()
                                .let {
                                    it.subList(1, it.size)
                                }.map { el ->
                                    val link = el.getElementsByTag("a")[0]
                                    val url = link.attr("href")
                                    val key =
                                        url.substring(
                                            url.lastIndexOf('/') + 1,
                                            url.lastIndexOf('.')
                                        )
                                            .split('-')
                                            .asSequence()
                                            .filter { it.isNotEmpty() }
                                            .last()
                                    link.text().trim() to key
                                }.distinctBy { it.second }

                        categories.map { (categoryName, categoryKey) ->
                            SourcePage.SingleCartoonPage.WithCover(
                                label = categoryName,
                                firstKey = { 1 }) { page ->
                                withIoResult {
                                    loadVideoOfCategory(
                                        groupKey = groupKey,
                                        categoryKey = categoryKey,
                                        page = page
                                    )
                                }
                            }
                        }
                    }
                }
            }
        return listOf(SourcePage.Group(label = "首页", newScreen = false) {
            withIoResult {
                val doc = libVioSource.requestWithFunCDNInterceptor {
                    url(LibVioSource.BASE_URL)
                    get()
                }.asDocument()
                val list = mutableListOf<SourcePage.SingleCartoonPage>()

                val containers = doc.getElementsByClass("stui-vodlist")
                for (container in containers) {
                    val videoEls = container.select("li > .stui-vodlist__box")
                    if (videoEls.isEmpty()) {
                        break
                    }

                    val groupName =
                        container.previousElementSibling()
                            ?.takeIf { it.hasClass("stui-vodlist__head") }
                            ?.selectFirst("h3")?.text()?.trim() ?: "推荐视频"
                    val videos = videoEls.map { libVioSource.parseLibVioVideo(it) }
                    val page = SourcePage.SingleCartoonPage.WithCover(
                        label = groupName,
                        firstKey = { 0 }) {
                        withResult {
                            null to videos
                        }
                    }
                    list.add(page)
                }
                list
            }
        }) + pages
    }

    private suspend fun loadVideoOfCategory(
        groupKey: String,
        categoryKey: String,
        page: Int
    ): Pair<Int?, List<CartoonCover>> {
        val doc = libVioSource.requestWithFunCDNInterceptor {
            url("${LibVioSource.BASE_URL}/show/$groupKey--time-$categoryKey-----$page---.html")
            get()
        }.asDocument()
        return libVioSource.parseVideoPage(doc, page)
    }


}