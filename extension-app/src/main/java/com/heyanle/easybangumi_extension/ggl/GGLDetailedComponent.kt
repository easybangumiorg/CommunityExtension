package com.heyanle.easybangumi_extension.ggl

import com.heyanle.easybangumi4.source_api.SourceResult
import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.component.detailed.DetailedComponent
import com.heyanle.easybangumi4.source_api.component.page.PageComponent
import com.heyanle.easybangumi4.source_api.component.search.SearchComponent
import com.heyanle.easybangumi4.source_api.component.update.UpdateComponent
import com.heyanle.easybangumi4.source_api.entity.Cartoon
import com.heyanle.easybangumi4.source_api.entity.CartoonImpl
import com.heyanle.easybangumi4.source_api.entity.CartoonSummary
import com.heyanle.easybangumi4.source_api.entity.Episode
import com.heyanle.easybangumi4.source_api.entity.PlayLine
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.core.SourceUtils
import com.heyanle.easybangumi4.source_api.utils.core.network.GET
import com.heyanle.easybangumi4.source_api.withResult
import kotlinx.coroutines.Dispatchers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Created by heyanle on 2024/1/28.
 * https://github.com/heyanLE
 */
class GGLDetailedComponent(
    private val okhttpHelper: OkhttpHelper,
) : ComponentWrapper(), DetailedComponent, UpdateComponent {

    override suspend fun getAll(summary: CartoonSummary): SourceResult<Pair<Cartoon, List<PlayLine>>> {
        return withResult(Dispatchers.IO) {
            val doc = getDoc(summary)
            detailed(doc, summary) to playLine(doc, summary)
        }

    }

    override suspend fun getDetailed(summary: CartoonSummary): SourceResult<Cartoon> {
        return withResult(Dispatchers.IO) {
            val doc = getDoc(summary)
            detailed(doc, summary)
        }
    }

    override suspend fun getPlayLine(summary: CartoonSummary): SourceResult<List<PlayLine>> {
        return withResult(Dispatchers.IO) {
            val doc = getDoc(summary)
            playLine(doc, summary)
        }
    }

    private fun getDoc(summary: CartoonSummary): Document {
        val d = okhttpHelper.cloudflareWebViewClient.newCall(
            GET(
                SourceUtils.urlParser(
                    GGLListComponent.ROOT_URL,
                    summary.id
                )
            )
        )
            .execute().body?.string() ?: throw NullPointerException()
        return Jsoup.parse(d)
    }

    private fun detailed(doc: Document, summary: CartoonSummary): Cartoon {
        val title = doc.select("div.detail-info h3.slide-info-title").text()
//        val genre = doc.select("div.detail-info div.slide-info span").map { it.text() }.joinToString { ", " }
        val cover = doc.select("div.wow div.detail-pic img").first()?.attr("data-src") ?: ""
        val desc = doc.select("div.switch-box div.check div.text").first()?.text() ?: ""
        return CartoonImpl(
            id = summary.id,
            url = SourceUtils.urlParser(GGLListComponent.ROOT_URL, summary.id),
            source = summary.source,
            title = title,
            coverUrl = SourceUtils.urlParser(GGLListComponent.ROOT_URL, cover),
            intro = "",
            description = desc,
            genre = null,
            status = Cartoon.STATUS_UNKNOWN,
            updateStrategy = Cartoon.UPDATE_STRATEGY_ALWAYS,
        )
    }

    private fun playLine(doc: Document, summary: CartoonSummary): List<PlayLine> {
        val tabs =
            doc.select("div.anthology.wow div.anthology-tab div.swiper-wrapper a.swiper-slide")
                .iterator()
        val epRoot = doc.select("div.anthology-list-box div ul.anthology-list-play").iterator()
        val playLines = arrayListOf<PlayLine>()
        var ii = 1
        while (tabs.hasNext() && epRoot.hasNext()) {
            val tab = tabs.next()
            val ul = epRoot.next()

            val es = arrayListOf<Episode>()
            ul.children().forEachIndexed { index, element ->
                es.add(
                    Episode(
                        id = (index + 1).toString(),
                        label = element?.text() ?: "",
                        order = index
                    )
                )
            }

            playLines.add(
                PlayLine(
                    id = ii.toString(),
                    label = tab.text(),
                    episode = es
                )
            )
            ii++
        }
        return playLines
    }

    override suspend fun update(
        cartoon: Cartoon,
        oldPlayLine: List<PlayLine>
    ): SourceResult<Cartoon> {
        return withResult(Dispatchers.IO) {
            when (val n = getAll(CartoonSummary(cartoon.id, cartoon.source))) {
                is SourceResult.Complete -> {
                    n.data.first.apply {

                        val newPlayLine = n.data.second

                        if (oldPlayLine.size != newPlayLine.size) {
                            isUpdate = true
                        } else {
                            isUpdate = false
                            for (i in oldPlayLine.indices) {
                                if (oldPlayLine[i].episode.size != newPlayLine[i].episode.size) {
                                    isUpdate = true
                                    break
                                }
                            }
                        }
                    }
                }

                is SourceResult.Error -> {
                    throw n.throwable
                }
            }
        }
    }
}