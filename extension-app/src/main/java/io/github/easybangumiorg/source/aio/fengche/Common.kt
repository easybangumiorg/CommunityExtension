package io.github.easybangumiorg.source.aio.fengche

import android.util.Log
import com.heyanle.easybangumi4.source_api.entity.CartoonCover
import com.heyanle.easybangumi4.source_api.entity.CartoonCoverImpl
import io.github.easybangumiorg.source.aio.asDocument
import io.github.easybangumiorg.source.aio.commonHttpClient
import io.github.easybangumiorg.source.aio.newGetRequest
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


//private val urlPageUrl = "https://wedm.cc/"
//
//private val baseUrlFallback = "https://www.886dm.tv"
//
//@Volatile
//private var _fengCheBaseUrl: String? = null
//
//val FengCheBaseUrl: String
//    get() = requireFengCheBaseUrl()
//
//private val baseUrlLock = ReentrantLock()
//
//private fun requireFengCheBaseUrl(): String {
//    if (_fengCheBaseUrl == null) {
//        baseUrlLock.withLock {
//            if (_fengCheBaseUrl == null) {
//                _fengCheBaseUrl = runCatching {
//                    requestFengCheBaseUrl()
//                }.onFailure {
//                    Log.e("FengCheSourceCommon", "requireFengCheBaseUrl:${it.message}", it)
//                }
//                    .getOrNull() ?: baseUrlFallback
//            }
//        }
//    }
//    return _fengCheBaseUrl!!
//}
//
//private fun requestFengCheBaseUrl(): String {
//    val doc = commonHttpClient.newGetRequest {
//        url(urlPageUrl)
//    }.asDocument()
//
//    val website = doc.selectFirst(".main .speedlist li a i")?.text()?.trim()?.let { text ->
//        text
//    }
//    return if (website?.isNotBlank() == true) {
//        "https://$website"
//    } else {
//        baseUrlFallback
//    }
//}


fun String.extractFengCheIdFromUrl() =
    this.substring(this.lastIndexOf('/') + 1, this.lastIndexOf('.'))


fun Document.hasNextPage(): Boolean =
    this.getElementById("aapages")
        ?.children()
        ?.findLast { it.hasClass("pagenow") }
        ?.nextElementSibling()
        ?.tagName() === "a"

fun Element.parseFengCheAnime(sourceKey: String): CartoonCover {
    val linkEl = selectFirst("a")!!
    val url = linkEl.absUrl("href")
    val imageUrl = selectFirst(".img_wrapper")?.dataset()?.get("original")
    val episode = linkEl.children().last()?.text()?.trim() ?: ""
    val title = linkEl.attr("title").takeIf { it.isNotEmpty() }?: linkEl.text().trim()
    return CartoonCoverImpl(
        id = url.extractFengCheIdFromUrl(),
        url = url,
        title = title,
        source = sourceKey,
        coverUrl = imageUrl,
        intro = episode
    )
}