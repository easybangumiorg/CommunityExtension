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


private val urlPageUrl = "https://wedm.cc/"

private val baseUrlFallback = "https://www.886dm.tv"

@Volatile
private var _fengCheBaseUrl: String? = null

val FengCheBaseUrl: String
    get() = requireFengCheBaseUrl()

private val baseUrlLock = ReentrantLock()

private fun requireFengCheBaseUrl(): String {
    if (_fengCheBaseUrl == null) {
        baseUrlLock.withLock {
            if (_fengCheBaseUrl == null) {
                _fengCheBaseUrl = runCatching {
                    requestFengCheBaseUrl()
                }.onFailure {
                    Log.e("FengCheSourceCommon", "requireFengCheBaseUrl:${it.message}", it)
                }
                    .getOrNull() ?: baseUrlFallback
            }
        }
    }
    return _fengCheBaseUrl!!
}

private fun requestFengCheBaseUrl(): String {
    val doc = commonHttpClient.newGetRequest {
        url(urlPageUrl)
    }.asDocument()

    val website = doc.selectFirst(".main .item p")?.text()?.trim()?.let { text ->
        val colonIndex =
            text.lastIndexOf('：').takeIf { it >= 0 } ?: text.lastIndexOf(':')
        if (colonIndex >= 0) {
            text.substring(colonIndex + 1).trim()
        } else {
            null
        }
    }
    return if (website?.isNotBlank() == true) {
        "https://$website"
    } else {
        baseUrlFallback
    }
}


fun String.extractFengCheIdFromUrl() =
    this.substring(this.lastIndexOf('/') + 1, this.lastIndexOf('.'))


fun Document.hasNextPage(): Boolean =
    this.selectFirst(".myui-page")?.getElementsByTag("a")
        ?.asSequence()
        ?.filter { it.attr("href").isNotBlank() }
        ?.toList()
        ?.run {
            indexOfLast { it.hasClass("btn-warm") } < size - 3
        } ?: false

fun Element.parseFengCheAnime(sourceKey: String): CartoonCover {
    val linkEl = selectFirst("a")!!
    val url = linkEl.absUrl("href")
    val imageUrl = linkEl.dataset()["original"] ?: linkEl.attr("style").run {
        val openBracketIndex = indexOf('(', indexOf("url"))
        val closeBracketIndex = indexOf(')', openBracketIndex)
        try {
            substring(openBracketIndex + 1, closeBracketIndex).trim()
        } catch (ex: Exception) {
            ""
        }
    }
    val episode = linkEl.children().last()?.text()?.trim() ?: ""
    val title =
        selectFirst(".myui-vodlist__detail .title")?.text()?.trim() ?: linkEl.attr("title")
            .trim()
    return CartoonCoverImpl(
        id = url.extractFengCheIdFromUrl(),
        url = url,
        title = title,
        source = sourceKey,
        coverUrl = imageUrl,
        intro = episode
    )
}