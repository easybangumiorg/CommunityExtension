package com.heyanle.easybangumi_extension.ggl

import com.heyanle.easybangumi4.source_api.component.ComponentWrapper
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import com.heyanle.easybangumi4.source_api.utils.api.OkhttpHelper
import com.heyanle.easybangumi4.source_api.utils.core.SourceUtils
import com.heyanle.easybangumi4.source_api.utils.core.network.GET

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Created by HeYanLe on 2023/5/21 21:18.
 * https://github.com/heyanLE
 */
class GGLListComponent(
    private val okhttpHelper: OkhttpHelper,
): ComponentWrapper() {

    companion object {
        val ROOT_URL = "https://anime.girigirilove.com"
    }
    suspend fun listHomePage(
        url: String,
        page: Int,
    ): Pair<Int?, List<CartoonCover>>{
        val d = if(!url.endsWith("/")) "${url}/" else url
        val u = d.replace("---/", "${page}---/")
        val list = arrayListOf<CartoonCover>()
        val doc = getDoc(u).getOrThrow()
        doc.select("div.border-box div.public-list-box").forEach {
            val uu = it.child(0).child(0).attr("href")
            val id = uu.subSequence(1, uu.length-1).toString()
            list.add(
                CartoonCoverImpl(
                    id = id,
                    source = source.key,
                    url = SourceUtils.urlParser(ROOT_URL, uu),
                    title = it.child(1).child(0).text(),
                    intro = it.select("span .public-list-prb").first()?.text(),
                    coverUrl = SourceUtils.urlParser(ROOT_URL,it.select("img").first()?.attr("data-src")?:""),
                )
            )
        }


        return (if(list.isEmpty()) null else page+1) to list
    }

    fun getDoc(target: String): Result<Document> {
        return runCatching {
            val req = okhttpHelper.cloudflareWebViewClient.newCall(
                GET(
                    SourceUtils.urlParser(ROOT_URL, target),
                )
            ).execute()
            val body = req.body!!.string()
            Jsoup.parse(body)
        }
    }
}



